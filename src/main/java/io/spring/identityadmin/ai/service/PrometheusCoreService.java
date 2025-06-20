package io.spring.identityadmin.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.identityadmin.ai.service.dto.PolicyAnalysisReport;
import io.spring.identityadmin.ai.service.dto.RecommendedRoleDto;
import io.spring.identityadmin.ai.service.dto.ResourceNameSuggestion;
import io.spring.identityadmin.ai.service.dto.TrustAssessment;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.entity.Users;
import io.spring.identityadmin.repository.PolicyRepository;
import io.spring.identityadmin.repository.UserRepository;
import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrometheusCoreService implements AiAuthorizationAdvisor {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    /**
     * 인가 컨텍스트를 분석하여 동적 신뢰도를 평가합니다.
     */
    @Override
    public TrustAssessment assessContext(AuthorizationContext context) {
        // 1. RAG 패턴: VectorStore 에서 관련 과거 접근 기록을 검색합니다.
        SearchRequest searchRequest = SearchRequest.builder()
                .query(context.subject().getName() + " " + context.resource().identifier())
                .topK(5)
                .build();
        List<Document> history = vectorStore.similaritySearch(searchRequest);

        // 2. [수정] AI에 전달할 프롬프트를 모두 한국어로 작성합니다.
        String userPromptTemplate = """
            **현재 요청 정보:**
            - 사용자: {username}
            - 접근 리소스: {resource}
            - 요청 행위: {action}
            - 접속 IP 주소: {ip}
            
            **해당 사용자의 최근 접근 이력:**
            {history}
            """;

        String historyContent = history.stream()
                .map(Document::getText) // Document 객체의 내용을 가져옵니다.
                .collect(Collectors.joining("\n"));

        // ChatClient를 사용하여 AI 모델에 요청을 보냅니다.
        String jsonResponse = chatClient.prompt()
                .system("""
                    당신은 IAM(계정 및 접근 관리) 시스템의 보안 리스크를 평가하는 AI 전문가입니다.
                    주어진 사용자의 과거 행동 패턴과 현재 접근 요청의 컨텍스트를 종합적으로 분석하여,
                    현재 요청의 신뢰도를 평가해야 합니다.
                    응답은 반드시 아래 명시된 JSON 형식으로만 제공해야 합니다.
                    JSON 형식: {"score": 0.xx, "riskTags": ["위험_태그_1", "위험_태그_2"], "summary": "한국어 요약 설명"}
                    """)
                .user(userSpec -> userSpec
                        .text(userPromptTemplate)
                        .param("username", context.subject().getName())
                        .param("resource", context.resource().identifier())
                        .param("action", context.action())
                        .param("history", historyContent)
                        .param("ip", context.environment().remoteIp() != null ? context.environment().remoteIp() : "알 수 없음")
                )
                .call()
                .content();

        // 3. AI의 JSON 응답을 DTO 객체로 변환하여 반환합니다.
        try {
            return objectMapper.readValue(jsonResponse, TrustAssessment.class);
        } catch (Exception e) {
            log.error("AI의 신뢰도 평가 응답을 파싱하는 데 실패했습니다. 응답: {}", jsonResponse, e);
            return new TrustAssessment(0.5, List.of("AI_RESPONSE_PARSING_ERROR"), "AI 응답을 분석하는데 실패했습니다.");
        }
    }

    /**
     * 기술적인 리소스 정보를 바탕으로 비즈니스 친화적인 이름과 설명을 제안합니다.
     */
    @Override
    public ResourceNameSuggestion suggestResourceName(String technicalIdentifier, String serviceOwner) {
        // [수정] AI에 전달할 프롬프트를 모두 한국어로 작성합니다.
        String userPromptTemplate = """
            - 소유 서비스: {owner}
            - 기술 식별자: {identifier}
            """;

        String jsonResponse = chatClient.prompt()
                .system("""
                    당신은 소프트웨어의 기술적 용어를 일반 비즈니스 사용자가 이해하기 쉬운 이름과 설명으로 만드는 네이밍 전문가입니다.
                    주어진 기술 정보를 바탕으로, IAM 관리자가 쉽게 이해할 수 있도록 명확하고 직관적인 '친화적 이름(friendlyName)'과 '설명(description)'을 한국어로 추천해주세요.
                    응답은 반드시 아래 명시된 JSON 형식으로만 제공해야 합니다.
                    JSON 형식: {"friendlyName": "추천 이름", "description": "상세 설명"}
                    """)
                .user(spec -> spec.text(userPromptTemplate)
                        .param("owner", serviceOwner)
                        .param("identifier", technicalIdentifier))
                .call()
                .content();

        try {
            return objectMapper.readValue(jsonResponse, ResourceNameSuggestion.class);
        } catch (Exception e) {
            log.error("AI의 리소스 이름 추천 응답을 파싱하는 데 실패했습니다.", e);
            return new ResourceNameSuggestion(technicalIdentifier, "AI 추천 이름 생성에 실패했습니다.");
        }
    }

    // ====================================================================
    // 아래는 아직 상세 로직이 구현되지 않은 메서드들입니다. (향후 구현 필요)
    // ====================================================================

    @Override
    public PolicyDto generatePolicyFromText(String naturalLanguageQuery) {
        log.warn("generatePolicyFromText is not fully implemented yet. Returning a mock response.");
        // TODO: Function Calling을 사용하여 실제 정책 생성 로직 구현
        return PolicyDto.builder().name("AI-Generated-Policy").description(naturalLanguageQuery).build();
    }

    /**
     * 특정 사용자에게 할당할 역할을 추천합니다.
     * 사용자와 유사한 다른 동료들의 권한 보유 패턴을 분석하여,
     * 해당 사용자에게 필요할 것으로 예측되는 역할을 제안합니다.
     */
    @Override
    public List<RecommendedRoleDto> recommendRolesForUser(Long userId) {
        // 1. 대상 사용자 정보 및 현재 보유 역할 조회
        Users targetUser = userRepository.findByIdWithGroupsRolesAndPermissions(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        Set<String> currentUserRoles = targetUser.getRoleNames().stream().collect(Collectors.toSet());

        // 2. RAG 패턴: Vector DB 에서 대상 사용자와 유사한 프로필을 가진 다른 사용자들을 검색
        String userProfileQuery = String.format("사용자: %s, 소속 그룹: %s",
                targetUser.getName(),
                targetUser.getUserGroups().stream()
                        .map(ug -> ug.getGroup().getName())
                        .collect(Collectors.joining(", ")));

        SearchRequest searchRequest = SearchRequest.builder()
                .query(userProfileQuery)
                .topK(10)
                .build();

        List<Document> similarUserDocs = vectorStore.similaritySearch(searchRequest);

        if (similarUserDocs.isEmpty()) {
            log.info("유사한 사용자를 찾을 수 없어 역할 추천을 생략합니다. User ID: {}", userId);
            return List.of();
        }

        // 3. AI에 전달할 프롬프트 구성
        String promptString = """
            당신은 조직의 역할(Role) 할당을 최적화하는 IAM 컨설턴트입니다.
            '대상 사용자'와 '유사 동료 그룹'의 정보를 바탕으로, 대상 사용자에게 가장 필요할 것으로 보이는 역할을 최대 3개까지 추천해주세요.
            
            **분석 정보:**
            - 대상 사용자: {targetUser}
            - 대상 사용자의 현재 역할: {currentUserRoles}
            - 유사 동료 그룹의 프로필 및 보유 역할 정보: {similarUsers}
            
            **당신의 임무:**
            1. 유사 동료 그룹이 공통적으로 가지고 있지만, 대상 사용자는 없는 역할을 후보로 식별합니다.
            2. 후보 역할들 중에서 대상 사용자의 프로필에 가장 적합하다고 판단되는 역할을 최대 3개 선정합니다.
            3. 각 추천 역할에 대해, 왜 추천하는지에 대한 명확한 한글 이유와 0.0에서 1.0 사이의 추천 신뢰도 점수를 부여합니다.
            
            **응답 형식 (JSON 배열만):**
            [{"roleId": 123, "roleName": "추천 역할명", "reason": "추천 이유", "confidence": 0.xx}]
            """;

        // 4. ChatClient를 사용하여 GPT에 추론 요청
        String jsonResponse = chatClient.prompt()
                .user(spec -> spec.text(promptString)
                        .param("targetUser", userProfileQuery)
                        .param("currentUserRoles", String.join(", ", currentUserRoles))
                        .param("similarUsers", similarUserDocs.stream().map(Document::getText).collect(Collectors.joining("\n---\n")))
                )
                .call()
                .content();

        // 5. AI의 JSON 응답을 DTO 리스트로 변환하여 반환
        try {
            return objectMapper.readValue(jsonResponse, new TypeReference<List<RecommendedRoleDto>>() {});
        } catch (Exception e) {
            log.error("AI 역할 추천 응답을 파싱하는 데 실패했습니다: {}", jsonResponse, e);
            return List.of();
        }
    }

    /**
     * 시스템의 전체 정책을 분석하여 잠재적인 보안 위험(SoD 위반 등)이나 비효율성을 찾아 보고합니다.
     */
    @Override
    public List<PolicyAnalysisReport> analyzeSecurityPosture() {
        // 1. 분석할 전체 정책 데이터를 조회
        List<String> allPolicies = policyRepository.findAllWithDetails().stream()
                .map(policy -> {
                    try {
                        return objectMapper.writeValueAsString(policy);
                    } catch (Exception e) { return null; }
                })
                .filter(s -> s != null)
                .toList();

        if (allPolicies.isEmpty()) {
            return List.of();
        }

        // 2. AI에 전달할 프롬프트 구성
        String promptString = """
            당신은 최고 수준의 IAM 보안 감사관입니다.
            다음은 우리 시스템에 존재하는 모든 접근 제어 정책 목록(JSON 형식)입니다.
            
            **전체 정책 목록:**
            {policies}
            
            **당신의 임무:**
            1. 전체 정책들을 면밀히 분석하여, 잠재적인 보안 위험이나 비효율성을 식별합니다.
            2. 특히 '직무 분리(SoD) 원칙 위배' 가능성, '과도한 권한(Over-permissioned)' 정책, '장기 미사용(Dormant)'으로 의심되는 권한 등을 중점적으로 찾아냅니다.
            3. 발견된 각 항목에 대해, 문제 유형, 상세 설명, 그리고 개선을 위한 권장 사항을 포함하여 보고서를 작성합니다.
            
            **응답 형식 (JSON 배열만):**
            [{"insightType": "문제 유형(예: SOD_VIOLATION)", "description": "상세 설명", "relatedEntityIds": [관련 정책/역할 ID], "recommendation": "개선 권장 사항"}]
            """;

        // 3. ChatClient를 사용하여 GPT에 분석 요청
        String jsonResponse = chatClient.prompt()
                .user(spec -> spec.text(promptString)
                        .param("policies", String.join("\n", allPolicies)))
                .call()
                .content();

        // 4. AI의 JSON 응답을 DTO 리스트로 변환하여 반환
        try {
            return objectMapper.readValue(jsonResponse, new TypeReference<List<PolicyAnalysisReport>>() {});
        } catch (Exception e) {
            log.error("AI 보안 상태 분석 응답을 파싱하는 데 실패했습니다: {}", jsonResponse, e);
            return List.of();
        }
    }
}
