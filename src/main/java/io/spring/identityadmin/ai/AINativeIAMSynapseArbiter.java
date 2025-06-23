package io.spring.identityadmin.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.identityadmin.ai.dto.*;
import io.spring.identityadmin.domain.dto.AiGeneratedPolicyDraftDto;
import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.domain.entity.ConditionTemplate;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.domain.entity.Role;
import io.spring.identityadmin.domain.entity.Users;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.repository.*;
import io.spring.identityadmin.security.xacml.pap.service.BusinessPolicyService;
import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.spring.identityadmin.domain.entity.policy.Policy.Effect.ALLOW;

@Slf4j
@Service
public class AINativeIAMSynapseArbiter implements AINativeIAMAdvisor {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final BusinessPolicyService businessPolicyService;
    private final ModelMapper modelMapper;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ConditionTemplateRepository conditionTemplateRepository;

    public AINativeIAMSynapseArbiter(
            ChatClient chatClient,
            VectorStore vectorStore,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            PolicyRepository policyRepository,
            @Lazy BusinessPolicyService businessPolicyService, // <-- 핵심 수정 사항
            ModelMapper modelMapper,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
        ConditionTemplateRepository conditionTemplateRepository) {

        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.policyRepository = policyRepository;
        this.businessPolicyService = businessPolicyService;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.conditionTemplateRepository = conditionTemplateRepository;
        this.modelMapper = modelMapper;
    }

    public Flux<String> generatePolicyFromTextStream(String naturalLanguageQuery) {
        log.info("🔥 AI 스트리밍 정책 초안 생성을 시작합니다: {}", naturalLanguageQuery);

        // 1. RAG - Vector DB 에서 관련 정보 검색
        SearchRequest searchRequest = SearchRequest.builder()
                .query(naturalLanguageQuery)
                .topK(10)
                .build();
        List<Document> contextDocs = vectorStore.similaritySearch(searchRequest);
        String contextInfo = contextDocs.stream()
                .map(doc -> "- " + doc.getText())
                .collect(Collectors.joining("\n"));

        // 2. 시스템 메타데이터 구성 (실제 DB 데이터)
        String systemMetadata = buildSystemMetadata();

        // 🔥 3. 자연스러운 한국어 프롬프트 - 인코딩 안정성 확보하면서 한글 유지
        String systemPrompt = String.format("""
        당신은 IAM 정책 분석 AI '아비터'입니다. 
        
        🎯 임무: 자연어 요구사항을 분석하여 구체적인 정책 구성 요소로 변환
        
        📋 시스템 정보:
        %s
        
        🔄 작업 단계:
        1. 키워드 분석: 요구사항에서 주요 키워드 추출
        2. 컨텍스트 매핑: 시스템의 실제 역할/권한과 매핑
        3. 조건 해석: 시간/장소/상황 조건들 식별
        4. JSON 구성: 최종 정책 구조 생성
        
        ⚠️ 중요 규칙:
        - 모든 ID는 위 시스템 정보에서 실제로 존재하는 숫자여야 함
        - 역할명/권한명으로 추론하되, 가장 가까운 실제 ID 사용
        - JSON은 반드시 정확한 형식으로 출력
        - 분석 과정을 단계별로 한국어로 설명 후 JSON 출력
        - 마지막에 반드시 명확한 JSON 마커 사용
        
        [매우 중요] roleIds, permissionIds 배열에는 반드시 이름이 아닌 '숫자 ID'를 포함해야 합니다.
        conditions 맵의 키 또한 '숫자 ID'여야 합니다.
        
        📤 최종 출력 형식:
        [한국어로 분석 과정 설명...]
        
        ===JSON시작===
        {
          "policyName": "정책 이름",
          "description": "정책 설명", 
          "roleIds": [실제_역할_ID들],
          "permissionIds": [실제_권한_ID들],
          "conditions": {"조건템플릿_ID": ["파라미터값"]},
          "aiRiskAssessmentEnabled": false,
          "requiredTrustScore": 0.7,
          "customConditionSpel": "",
          "effect": "ALLOW"
        }
        ===JSON끝===
        """, systemMetadata);

        String userPrompt = String.format("""
        **자연어 요구사항:**
        "%s"
        
        **참고 컨텍스트:**
        %s
        
        위 요구사항을 분석하여 정책을 구성해주세요.
        """, naturalLanguageQuery, contextInfo);

        // 4. ChatClient 스트리밍 호출 with 인코딩 안정화
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                // 🔥 인코딩 안정성을 위한 처리
                .map(this::cleanTextChunk)
                .filter(chunk -> !chunk.trim().isEmpty()) // 빈 청크 필터링
                .delayElements(Duration.ofMillis(10)) // 안정적인 전송을 위한 딜레이
                .doOnNext(chunk -> {
                    String logChunk = chunk.length() > 50 ? chunk.substring(0, 50) + "..." : chunk;
                    log.debug("🔥 AI 응답 청크: [{}]", logChunk);
                })
                .doOnError(error -> log.error("🔥 AI 스트리밍 오류", error))
                .onErrorResume(error -> {
                    log.error("🔥 AI 스트리밍 실패, 에러 메시지 반환", error);
                    return Flux.just("ERROR: AI 서비스 연결 실패: " + error.getMessage());
                });
    }

    /**
     * 🔥 텍스트 청크 정제 - 한글 인코딩 안정성 확보
     */
    private String cleanTextChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return "";
        }

        try {
            // 1. UTF-8 인코딩 안정성 검증
            byte[] bytes = chunk.getBytes(StandardCharsets.UTF_8);
            String decoded = new String(bytes, StandardCharsets.UTF_8);

            // 2. 불필요한 제어 문자만 제거 (한글은 보존)
            String cleaned = decoded.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

            return cleaned;
        } catch (Exception e) {
            log.warn("🔥 텍스트 청크 정제 실패: {}", e.getMessage());
            return chunk; // 실패 시 원본 반환
        }
    }

    /**
     * 시스템의 실제 메타데이터를 구성합니다.
     */
    private String buildSystemMetadata() {
        StringBuilder metadata = new StringBuilder();

        // 역할 정보
        List<Role> roles = roleRepository.findAll();
        metadata.append("📋 사용 가능한 역할:\n");
        roles.forEach(role ->
                metadata.append(String.format("- ID: %d, 이름: %s\n", role.getId(), role.getRoleName())));

        // 권한 정보
        List<Permission> permissions = permissionRepository.findAll();
        metadata.append("\n🔑 사용 가능한 권한:\n");
        permissions.forEach(perm ->
                metadata.append(String.format("- ID: %d, 이름: %s\n", perm.getId(), perm.getFriendlyName())));

        // 조건 템플릿 정보
        List<ConditionTemplate> conditions = conditionTemplateRepository.findAll();
        metadata.append("\n⏰ 사용 가능한 조건 템플릿:\n");
        conditions.forEach(cond ->
                metadata.append(String.format("- ID: %d, 이름: %s\n", cond.getId(), cond.getName())));

        return metadata.toString();
    }

    /**
     * [기존 유지] 일반 방식의 정책 생성 (fallback용)
     */
    @Override
    public AiGeneratedPolicyDraftDto generatePolicyFromTextByAi(String naturalLanguageQuery) {
        // RAG 검색
        SearchRequest searchRequest = SearchRequest.builder()
                .query(naturalLanguageQuery)
                .topK(10)
                .build();
        List<Document> contextDocs = vectorStore.similaritySearch(searchRequest);
        String contextInfo = contextDocs.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));

        String systemMetadata = buildSystemMetadata();

        String systemPrompt = String.format("""
            당신은 사용자의 자연어 요구사항을 분석하여, IAM 시스템이 이해할 수 있는 BusinessPolicyDto JSON 객체로 변환하는 AI 에이전트입니다.
            
            시스템 정보:
            %s
            
            요청을 분석하여 주체(역할), 리소스(권한), 행위, 그리고 조건 등을 추출해야 합니다.
            제공된 시스템 정보를 활용하여 실제 존재하는 ID를 사용해야 합니다.
            
            [매우 중요] roleIds, permissionIds 배열에는 반드시 이름이 아닌 '숫자 ID'를 포함해야 합니다.
            conditions 맵의 키 또한 '숫자 ID'여야 합니다.
            응답 시 마크다운 코드 블록(```)을 사용하지 말고 순수 JSON만 출력하세요.
            
            **출력 JSON 형식:**
            {
              "policyName": "AI가 생성한 정책 이름",
              "description": "AI가 생성한 정책 설명",
              "roleIds": [실제_역할_ID들],
              "permissionIds": [실제_권한_ID들],
              "conditional": true,
              "conditions": {"조건템플릿_ID": ["파라미터1"]},
              "aiRiskAssessmentEnabled": false,
              "requiredTrustScore": 0.7,
              "customConditionSpel": "",
              "effect": "ALLOW"
            }
            """, systemMetadata);

        String userPrompt = String.format("""
            **자연어 요구사항:**
            "%s"
            
            **참고 컨텍스트:**
            %s
            """, naturalLanguageQuery, contextInfo);

        String jsonResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        try {
            // JSON 정제 적용
            String cleanedJson = extractAndCleanJson(jsonResponse);
            AiResponseDto aiResponse = objectMapper.readValue(cleanedJson, AiResponseDto.class);
            BusinessPolicyDto policyData = translateAiResponseToBusinessDto(aiResponse);

            Map<String, String> roleIdToNameMap = getRoleNames(policyData.getRoleIds());
            Map<String, String> permissionIdToNameMap = getPermissionNames(policyData.getPermissionIds());
            Map<String, String> conditionIdToNameMap = getConditionTemplateNames(policyData.getConditions());

            return new AiGeneratedPolicyDraftDto(policyData, roleIdToNameMap, permissionIdToNameMap, conditionIdToNameMap);

        } catch (Exception e) {
            log.error("AI 정책 생성 또는 파싱에 실패했습니다. AI Response: {}", jsonResponse, e);

            // 💡 Fallback: 기본 정책 데이터 생성
            return createFallbackPolicyData(naturalLanguageQuery);
        }
    }

    /**
     * 개선된 JSON 추출 및 정제 메서드 - 한글 마커 지원
     */
    private String extractAndCleanJson(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("AI 응답이 비어있습니다.");
        }

        String response = aiResponse.trim();
        log.debug("🔥 원본 AI 응답 길이: {}, 첫 200자: {}", response.length(),
                response.substring(0, Math.min(response.length(), 200)));

        // 1. 한글 마커로 JSON 추출 (===JSON시작===, ===JSON끝===)
        String jsonStartMarker = "===JSON시작===";
        String jsonEndMarker = "===JSON끝===";

        int startIndex = response.indexOf(jsonStartMarker);
        int endIndex = response.indexOf(jsonEndMarker);

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            String extractedJson = response.substring(startIndex + jsonStartMarker.length(), endIndex).trim();
            log.debug("🔥 한글 마커로 추출된 JSON: {}", extractedJson);
            return cleanJsonString(extractedJson);
        }

        // 2. 영어 마커로 JSON 추출 (JSON_RESULT_START, JSON_RESULT_END)
        jsonStartMarker = "JSON_RESULT_START";
        jsonEndMarker = "JSON_RESULT_END";
        startIndex = response.indexOf(jsonStartMarker);
        endIndex = response.indexOf(jsonEndMarker);

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            String extractedJson = response.substring(startIndex + jsonStartMarker.length(), endIndex).trim();
            log.debug("🔥 영어 마커로 추출된 JSON: {}", extractedJson);
            return cleanJsonString(extractedJson);
        }

        // 3. 마크다운 코드 블록 제거
        String[] patterns = {
                "```json\\s*([\\s\\S]*?)\\s*```",
                "```\\s*([\\s\\S]*?)\\s*```"
        };

        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(response);
            if (m.find()) {
                String extractedJson = m.group(1).trim();
                log.debug("🔥 마크다운 패턴으로 추출된 JSON: {}", extractedJson);
                return cleanJsonString(extractedJson);
            }
        }

        // 4. JSON 객체 직접 추출 ({ ... })
        int jsonStart = response.indexOf('{');
        int jsonEnd = findMatchingBrace(response, jsonStart);

        if (jsonStart != -1 && jsonEnd != -1) {
            String extractedJson = response.substring(jsonStart, jsonEnd + 1).trim();
            log.debug("🔥 중괄호로 추출된 JSON: {}", extractedJson);
            return cleanJsonString(extractedJson);
        }

        throw new IllegalArgumentException("JSON 추출에 실패했습니다");
    }

    /**
     * 매칭되는 중괄호를 찾는 헬퍼 메서드
     */
    private int findMatchingBrace(String text, int start) {
        if (start == -1 || start >= text.length() || text.charAt(start) != '{') {
            return -1;
        }

        int braceCount = 1;
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * JSON 문자열 정제 메서드 개선
     */
    private String cleanJsonString(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return jsonStr;
        }

        log.debug("🔥 JSON 정제 시작, 원본 길이: {}", jsonStr.length());

        // 1. 기본 정제 - 한글 보존
        String cleaned = jsonStr
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")  // 제어 문자만 제거
                .replaceAll("\\n\\s*\\n", "\n")
                .trim();

        // 2. JSON 시작과 끝 찾기
        int jsonStart = cleaned.indexOf('{');
        int jsonEnd = findMatchingBrace(cleaned, jsonStart);

        if (jsonStart != -1 && jsonEnd != -1) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
        }

        // 3. 잘못된 쉼표 제거
        cleaned = cleaned.replaceAll(",\\s*([}\\]])", "$1");

        log.debug("🔥 정제된 JSON 길이: {}", cleaned.length());
        return cleaned;
    }

    /**
     * AI 파싱 실패 시 기본 정책 데이터를 생성하는 fallback 메서드
     */
    private AiGeneratedPolicyDraftDto createFallbackPolicyData(String naturalLanguageQuery) {
        log.info("🔥 AI 파싱 실패, fallback 정책 데이터 생성");

        BusinessPolicyDto fallbackDto = new BusinessPolicyDto();
        fallbackDto.setPolicyName("AI 생성 정책 (기본)");
        fallbackDto.setDescription("AI가 분석한 요구사항: " + naturalLanguageQuery);
        fallbackDto.setRoleIds(new HashSet<>());
        fallbackDto.setPermissionIds(new HashSet<>());
        fallbackDto.setConditions(new HashMap<>());
        fallbackDto.setEffect(ALLOW);
        fallbackDto.setAiRiskAssessmentEnabled(false);
        fallbackDto.setRequiredTrustScore(0.7);
        fallbackDto.setCustomConditionSpel("");

        // 키워드 기반으로 기본 매핑 시도
        tryBasicKeywordMapping(naturalLanguageQuery, fallbackDto);

        return new AiGeneratedPolicyDraftDto(
                fallbackDto,
                getRoleNames(fallbackDto.getRoleIds()),
                getPermissionNames(fallbackDto.getPermissionIds()),
                getConditionTemplateNames(fallbackDto.getConditions())
        );
    }

    /**
     * 키워드 기반 기본 매핑 - 더 정확한 한글 키워드 검색
     */
    private void tryBasicKeywordMapping(String query, BusinessPolicyDto dto) {
        String lowerQuery = query.toLowerCase();

        // 역할 매핑 - 다양한 키워드 패턴
        List<Role> allRoles = roleRepository.findAll();
        for (Role role : allRoles) {
            String roleName = role.getRoleName().toLowerCase();
            if (lowerQuery.contains(roleName) ||
                    (lowerQuery.contains("개발") && roleName.contains("개발")) ||
                    (lowerQuery.contains("관리자") && roleName.contains("관리")) ||
                    (lowerQuery.contains("사용자") && roleName.contains("사용자")) ||
                    (lowerQuery.contains("팀") && roleName.contains("팀"))) {
                dto.getRoleIds().add(role.getId());
                log.info("🔥 키워드 매핑 - 역할 추가: {} (ID: {})", role.getRoleName(), role.getId());
                break;
            }
        }

        // 권한 매핑 - 다양한 권한 키워드
        List<Permission> allPermissions = permissionRepository.findAll();
        for (Permission perm : allPermissions) {
            String permName = perm.getFriendlyName().toLowerCase();
            if (lowerQuery.contains(permName) ||
                    (lowerQuery.contains("조회") && permName.contains("조회")) ||
                    (lowerQuery.contains("데이터") && permName.contains("데이터")) ||
                    (lowerQuery.contains("고객") && permName.contains("고객")) ||
                    (lowerQuery.contains("수정") && permName.contains("수정")) ||
                    (lowerQuery.contains("삭제") && permName.contains("삭제")) ||
                    (lowerQuery.contains("읽기") && permName.contains("읽기"))) {
                dto.getPermissionIds().add(perm.getId());
                log.info("🔥 키워드 매핑 - 권한 추가: {} (ID: {})", perm.getFriendlyName(), perm.getId());
                break;
            }
        }

        // 조건 매핑 - 시간 관련 키워드
        List<ConditionTemplate> allConditions = conditionTemplateRepository.findAll();
        for (ConditionTemplate cond : allConditions) {
            String condName = cond.getName().toLowerCase();
            if ((lowerQuery.contains("업무시간") || lowerQuery.contains("평일") || lowerQuery.contains("근무시간")) &&
                    (condName.contains("업무") || condName.contains("시간"))) {
                dto.getConditions().put(cond.getId(), Arrays.asList("09:00-18:00"));
                log.info("🔥 키워드 매핑 - 조건 추가: {} (ID: {})", cond.getName(), cond.getId());
                break;
            }
        }
    }

    /**
     * [신규] AI 응답 DTO를 시스템 내부용 DTO로 변환하는 헬퍼 메서드.
     * 역할/권한 이름이 문자열로 들어와도 DB 조회를 통해 ID로 변환합니다.
     */
    private BusinessPolicyDto translateAiResponseToBusinessDto(AiResponseDto aiResponse) {
        Set<Long> resolvedRoleIds = aiResponse.roleIds().stream()
                .map(this::resolveRoleId)
                .collect(Collectors.toSet());

        Set<Long> resolvedPermissionIds = aiResponse.permissionIds().stream()
                .map(this::resolvePermissionId)
                .collect(Collectors.toSet());

        Map<Long, List<String>> resolvedConditions = aiResponse.conditions().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> resolveConditionTemplateId(entry.getKey()),
                        Map.Entry::getValue
                ));

        BusinessPolicyDto dto = new BusinessPolicyDto();
        dto.setPolicyName(aiResponse.policyName());
        dto.setDescription(aiResponse.description());
        dto.setRoleIds(resolvedRoleIds);
        dto.setPermissionIds(resolvedPermissionIds);
        dto.setConditional(aiResponse.conditional());
        dto.setConditions(resolvedConditions);
        dto.setAiRiskAssessmentEnabled(aiResponse.aiRiskAssessmentEnabled());
        dto.setRequiredTrustScore(aiResponse.requiredTrustScore());
        dto.setCustomConditionSpel(aiResponse.customConditionSpel());
        dto.setEffect(aiResponse.effect());

        return dto;
    }

    // --- ID 변환 헬퍼 메서드들 ---
    private Long resolveRoleId(Object idOrName) {
        if (idOrName instanceof Number) {
            return ((Number) idOrName).longValue();
        }
        String name = idOrName.toString();
        return roleRepository.findByRoleName(name).map(Role::getId)
                .orElseThrow(() -> new IllegalArgumentException("AI가 반환한 역할을 찾을 수 없습니다: " + name));
    }

    private Long resolvePermissionId(Object idOrName) {
        if (idOrName instanceof Number) {
            return ((Number) idOrName).longValue();
        }
        String name = idOrName.toString();
        return permissionRepository.findByName(name).map(Permission::getId)
                .orElseThrow(() -> new IllegalArgumentException("AI가 반환한 권한을 찾을 수 없습니다: " + name));
    }

    private Long resolveConditionTemplateId(String idOrName) {
        try {
            return Long.parseLong(idOrName);
        } catch (NumberFormatException e) {
            // 이름으로 찾는 로직 추가 (필요시)
            throw new IllegalArgumentException("AI가 반환한 조건 템플릿을 찾을 수 없습니다: " + idOrName);
        }
    }

    private Map<String, String> getRoleNames(Set<Long> ids) {
        System.out.println("🔥 getRoleNames 호출됨, IDs: " + ids);

        if (CollectionUtils.isEmpty(ids)) {
            System.out.println("🔥 roleIds가 비어있음!");
            return Map.of();
        }

        List<Role> roles = roleRepository.findAllById(ids);
        System.out.println("🔥 DB에서 찾은 역할들: " + roles);

        Map<String, String> result = roles.stream()
                .collect(Collectors.toMap(
                        role -> String.valueOf(role.getId()),
                        Role::getRoleName
                ));

        System.out.println("🔥 최종 roleIdToNameMap: " + result);
        return result;
    }

    private Map<String, String> getPermissionNames(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) return Map.of();
        return permissionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(permission -> String.valueOf(permission.getId()), Permission::getFriendlyName));
    }
    private Map<String, String> getConditionTemplateNames(Map<Long, List<String>> conditionsMap) {
        if (CollectionUtils.isEmpty(conditionsMap)) {
            return Collections.emptyMap();
        }

        // 1. 맵에서 조건 템플릿 ID 목록만 추출합니다.
        Set<Long> templateIds = conditionsMap.keySet();

        // 2. ID 목록을 사용하여 DB에서 ConditionTemplate 엔티티들을 한번에 조회합니다.
        List<ConditionTemplate> templates = conditionTemplateRepository.findAllById(templateIds);

        // 3. 조회된 엔티티 리스트를 'ID(String) -> 이름(String)' 형태의 맵으로 변환합니다.
        return templates.stream()
                .collect(Collectors.toMap(
                        template -> String.valueOf(template.getId()),
                        ConditionTemplate::getName,
                        (name1, name2) -> name1 // 혹시 모를 중복 키 발생 시 처리
                ));
    }

    @Override
    public TrustAssessment assessContext(AuthorizationContext context) {
        // 1. RAG 패턴: Vector DB 에서 관련 과거 접근 기록 검색 (기존과 유사)
        SearchRequest searchRequest = SearchRequest.builder()
                .query(context.subject().getName() + " " + context.resource().identifier())
                .topK(5)
                .build();
        List<Document> history = vectorStore.similaritySearch(searchRequest);
        String historyContent = history.stream().map(Document::getText).collect(Collectors.joining("\n"));

        UserDto user = (UserDto) context.subject().getPrincipal();

        // 2. [수정] AI의 '연쇄적 추론'을 유도하는 강화된 프롬프트
        String userPromptTemplate = """
            **1. 현재 접근 요청 상세 정보:**
            - 사용자: {name} (ID: {userId})
            - 역할: {roles}
            - 소속 그룹: {groups}
            - 접근 리소스: {resource}
            - 요청 행위: {action}
            - 접속 IP 주소: {ip}
            
            **2. 해당 사용자의 과거 접근 패턴 요약 (최근 5건):**
            {history}
            
            **3. 분석 및 평가:**
            위 정보를 바탕으로, 다음 단계에 따라 현재 접근 요청의 위험도를 분석하고 신뢰도를 평가하라.
            - **Anomalies (이상 징후):** 과거 패턴과 비교하여 현재 요청에서 나타나는 이상 징후(예: 새로운 IP, 평소와 다른 시간대, 접근한 적 없는 리소스)를 모두 찾아 목록으로 나열하라.
            - **Reasoning (추론 과정):** 식별된 이상 징후와 사용자의 역할/권한을 종합하여, 이 요청이 왜 위험하거나 안전하다고 판단했는지 그 이유를 단계별로 설명하라.
            - **Final Assessment (최종 판결):** 위 분석을 바탕으로 최종 신뢰도 점수(score), 위험 태그(riskTags), 그리고 한국어 요약(summary)을 결정하라.
            """;

        String jsonResponse = chatClient.prompt()
                .system("""
                    당신은 IAM 시스템의 모든 컨텍스트를 분석하여 접근 요청의 신뢰도를 판결하는 AI 보안 전문가 '아비터(Arbiter)'입니다.
                    당신은 반드시 연쇄적 추론(Chain-of-Thought) 방식으로 분석을 수행한 뒤, 최종 결론을 JSON 형식으로만 반환해야 합니다.
                    JSON 형식: {"score": 0.xx, "riskTags": ["위험_태그"], "summary": "한국어 요약 설명"}
                    """)
                .user(userSpec -> userSpec
                        .text(userPromptTemplate)
                        .param("userId", user.getUsername())
                        .param("name", user.getName())
                        .param("roles", context.attributes().getOrDefault("userRoles", "N/A"))
                        .param("groups", context.attributes().getOrDefault("userGroups", "N/A"))
                        .param("resource", context.resource().identifier())
                        .param("action", context.action())
                        .param("history", historyContent)
                        .param("ip", context.environment().remoteIp() != null ? context.environment().remoteIp() : "알 수 없음")
                )
                .call()
                .content();

        // 3. AI의 JSON 응답을 DTO 객체로 변환하여 반환
        try {
            return objectMapper.readValue(jsonResponse, TrustAssessment.class);
        } catch (Exception e) {
            log.error("AI 신뢰도 판결 응답 파싱 실패: {}", jsonResponse, e);
            // AI 실패 시 안전을 위해 보수적인 점수 반환
            return new TrustAssessment(0.3, List.of("AI_SYSTEM_ERROR"), "AI 시스템 오류로 신뢰도를 평가할 수 없습니다.");
        }
    }

    @Override
    public Map<String, ResourceNameSuggestion> suggestResourceNamesInBatch(List<Map<String, String>> resourcesToSuggest) {
        if (resourcesToSuggest == null || resourcesToSuggest.isEmpty()) {
            return Map.of();
        }

        // 1. AI에 전달할 프롬프트 구성
        String systemPrompt = """
            당신은 소프트웨어의 기술적 용어를 일반 비즈니스 사용자가 이해하기 쉬운 이름과 설명으로 만드는 네이밍 전문가입니다.
            주어진 JSON 배열 형태의 기술 정보 목록을 받아서, 각 항목에 대해 명확하고 직관적인 'friendlyName'과 'description'을 한국어로 추천해주세요.
            
            응답은 반드시 아래 명시된 '기술 식별자(identifier)를 Key로 갖는 JSON 객체' 형식으로만 제공해야 합니다.
            입력된 모든 항목에 대한 응답을 포함해야 합니다.
            
            응답 JSON 형식:
            {
              "기술_식별자_1": {"friendlyName": "추천 이름 1", "description": "상세 설명 1"},
              "기술_식별자_2": {"friendlyName": "추천 이름 2", "description": "상세 설명 2"}
            }
            """;

        try {
            // 2. 추천이 필요한 리소스 목록을 JSON 문자열로 변환하여 프롬프트에 삽입
            String resourcesJson = objectMapper.writeValueAsString(resourcesToSuggest);

            String jsonResponse = chatClient.prompt()
                    .system(systemPrompt)
                    .user(resourcesJson)
                    .call()
                    .content();

            // 3. AI의 응답을 Map 형태로 변환하여 반환
            return objectMapper.readValue(jsonResponse, new TypeReference<>() {});

        } catch (Exception e) {
            log.error("AI 리소스 이름 배치 추천 실패", e);
            // 실패 시 빈 Map 반환
            return Map.of();
        }
    }

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

    @Override
    public List<RecommendedRoleDto> recommendRolesForUser(Long userId) {
        // 1. 대상 사용자 정보 및 현재 보유 역할 조회
        Users targetUser = userRepository.findByIdWithGroupsRolesAndPermissions(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        Set<String> currentUserRoles = new HashSet<>(targetUser.getRoleNames());

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

    /**
     * 관리자의 자연어 요구사항을 분석하여, 시스템이 실행할 수 있는 정책(Policy) 초안을 생성합니다.
     * AI를 사용하여 자연어를 구조화된 BusinessPolicyDto JSON으로 변환한 뒤,
     * 이를 BusinessPolicyService에 전달하여 실제 정책을 생성합니다.
     */
    @Override
    @Transactional
    public PolicyDto generatePolicyFromText(String naturalLanguageQuery) {
        // 1. AI에 전달할 프롬프트 구성
        String systemPrompt = """
            당신은 사용자의 자연어 요청을 분석하여, IAM 시스템이 이해할 수 있는 구조화된 JSON 데이터로 변환하는 AI 에이전트입니다.
            요청을 분석하여 주체(subjects), 리소스(resources), 행위(actions), 그리고 SpEL 형식의 조건(condition)을 추출해야 합니다.
            - 주체, 리소스, 행위는 'GROUP_이름', 'ROLE_이름', 'PERM_이름' 과 같은 시스템 식별자로 변환해야 합니다.
            - 시간, 장소와 같은 제약 조건은 SpEL(Spring Expression Language) 형식의 문자열로 변환해야 합니다.
            - 분석이 불가능하거나 정보가 부족하면, 필수 필드를 null이 아닌 빈 배열([])로 설정하여 응답해야 합니다.
            
            응답은 반드시 아래 명시된 JSON 형식으로만 제공해야 합니다.
            JSON 형식:
            {
              "name": "정책 이름 (자연어 요청을 기반으로 생성)",
              "description": "정책 설명 (자연어 요청을 기반으로 생성)",
              "subjects": ["GROUP_DEV", "ROLE_ADMIN"],
              "resources": ["PERM_CUSTOMER_DATA_READ"],
              "actions": ["ACTION_VIEW"],
              "condition": "hasIpAddress('192.168.1.0/24') && #isBusinessHours()",
              "effect": "ALLOW"
            }
            """;

        // 2. ChatClient를 사용하여 AI 모델에 JSON 생성 요청
        String jsonResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(naturalLanguageQuery)
                .call()
                .content();

        try {
            // 3. AI가 생성한 JSON 응답을 BusinessPolicyDto 객체로 변환
            BusinessPolicyDto businessPolicyDto = objectMapper.readValue(jsonResponse, BusinessPolicyDto.class);

            // 4. 변환된 DTO를 사용하여 실제 정책 생성 서비스를 호출
            Policy createdPolicy = businessPolicyService.createPolicyFromBusinessRule(businessPolicyDto);
            log.info("AI-generated policy has been successfully created. Policy ID: {}", createdPolicy.getId());

            // 5. 생성된 Policy 엔티티를 PolicyDto로 변환하여 반환
            return modelMapper.map(createdPolicy, PolicyDto.class);

        } catch (Exception e) {
            log.error("AI 정책 생성 또는 파싱에 실패했습니다. Natural Query: {}, AI Response: {}", naturalLanguageQuery, jsonResponse, e);
            throw new IllegalStateException("AI를 통한 정책 생성에 실패했습니다. AI 응답을 확인해주세요.", e);
        }
    }
}
