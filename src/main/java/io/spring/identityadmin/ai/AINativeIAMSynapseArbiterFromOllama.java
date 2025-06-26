package io.spring.identityadmin.ai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.spring.identityadmin.ai.dto.*;
import io.spring.identityadmin.domain.dto.AiGeneratedPolicyDraftDto;
import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.domain.entity.*;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.repository.*;
import io.spring.identityadmin.security.xacml.pap.service.BusinessPolicyService;
import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;
import io.spring.identityadmin.resource.service.ConditionCompatibilityService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
//import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.spring.identityadmin.domain.entity.policy.Policy.Effect.ALLOW;

@Slf4j
@Service
public class AINativeIAMSynapseArbiterFromOllama implements AINativeIAMAdvisor {

    private final OllamaChatModel chatModel;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final BusinessPolicyService businessPolicyService;
    private final ModelMapper modelMapper;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ConditionTemplateRepository conditionTemplateRepository;
    private final ManagedResourceRepository managedResourceRepository;
    private final ConditionCompatibilityService conditionCompatibilityService;

    public AINativeIAMSynapseArbiterFromOllama(
            OllamaChatModel chatModel,
            VectorStore vectorStore,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            PolicyRepository policyRepository,
            @Lazy BusinessPolicyService businessPolicyService, // <-- 핵심 수정 사항
            ModelMapper modelMapper,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            ConditionTemplateRepository conditionTemplateRepository,
            ManagedResourceRepository managedResourceRepository,
            ConditionCompatibilityService conditionCompatibilityService) {

        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.policyRepository = policyRepository;
        this.businessPolicyService = businessPolicyService;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.conditionTemplateRepository = conditionTemplateRepository;
        this.managedResourceRepository = managedResourceRepository;
        this.conditionCompatibilityService = conditionCompatibilityService;
        this.modelMapper = modelMapper;
    }

    public Flux<String> generatePolicyFromTextStream(String naturalLanguageQuery) {
        return generatePolicyFromTextStream(naturalLanguageQuery, null);
    }

    public Flux<String> generatePolicyFromTextStream(String naturalLanguageQuery, PolicyGenerationRequest.AvailableItems availableItems) {
        log.info("🔥 AI 스트리밍 정책 초안 생성을 시작합니다: {}", naturalLanguageQuery);
        if (availableItems != null) {
            log.info("🎯 사용 가능한 항목들 포함: 역할 {}개, 권한 {}개, 조건 {}개", 
                availableItems.roles() != null ? availableItems.roles().size() : 0,
                availableItems.permissions() != null ? availableItems.permissions().size() : 0,
                availableItems.conditions() != null ? availableItems.conditions().size() : 0);
        }

        // 1. RAG - Vector DB 에서 관련 정보 검색
        SearchRequest searchRequest = SearchRequest.builder()
                .query(naturalLanguageQuery)
                .topK(10)
                .build();
        List<Document> contextDocs = vectorStore.similaritySearch(searchRequest);
        String contextInfo = contextDocs.stream()
                .map(doc -> "- " + doc.getText())
                .collect(Collectors.joining("\n"));

        // 2. 시스템 메타데이터 구성 (사용 가능한 항목들 포함)
        String systemMetadata = buildSystemMetadata(availableItems);

        // 3. 시스템 메시지와 사용자 메시지 구성
        String systemPrompt = String.format("""
    당신은 IAM 정책 분석 AI '아비터'입니다. 
    
    🎯 임무: 자연어 요구사항을 분석하여 구체적인 정책 구성 요소로 변환
    
    📋 시스템 정보:
    %s
    
    ⚠️ 절대적 JSON 규칙 (위반 시 시스템 오류 발생):
    1. JSON에는 절대 주석을 포함하지 마세요 (// 또는 /* */ 절대 금지)
    2. JSON 내부에 설명 텍스트 절대 금지
    3. 각 필드는 한 번만 포함 (중복 절대 금지)
    4. 모든 ID는 반드시 숫자만 사용
    5. 문자열 값은 반드시 쌍따옴표로 감싸기
    6. 마지막 항목 뒤에 쉼표 절대 금지
    7. 빈 값은 빈 문자열("")이나 빈 배열([]) 사용
    
    🔥 JSON 파싱 오류 방지를 위한 추가 규칙:
    - 키는 반드시 쌍따옴표로 감싸기: "key"
    - 값도 반드시 적절한 타입으로: "string", 123, true, []
    - 객체나 배열이 비어있으면: {}, []
    - 특수문자는 이스케이프: \", \\, \n
    
    📤 필수 JSON 형식 (정확히 이 형식만 사용):
    
    ===JSON시작===
    {
      "policyName": "정책이름",
      "description": "정책설명", 
      "roleIds": [2],
      "permissionIds": [3],
      "conditions": {"1": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]},
      "aiRiskAssessmentEnabled": false,
      "requiredTrustScore": 0.7,
      "customConditionSpel": "",
      "effect": "ALLOW"
    }
    ===JSON끝===
    
    분석 과정이나 설명은 JSON 블록 앞에 작성하고, JSON은 완벽하게 파싱 가능한 형태로만 작성하세요.
    """, systemMetadata);

        String userPrompt = String.format("""
    **자연어 요구사항:**
    "%s"
    
    **참고 컨텍스트:**
    %s
    
    위 요구사항을 분석하여 정책을 구성해주세요.
    """, naturalLanguageQuery, contextInfo);

        // 4. 개선된 스트리밍 처리
        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(userPrompt);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        // 텍스트 버퍼와 JSON 감지 상태 관리
        AtomicReference<StringBuilder> textBuffer = new AtomicReference<>(new StringBuilder());
        AtomicBoolean jsonStarted = new AtomicBoolean(false);
        AtomicBoolean jsonEnded = new AtomicBoolean(false);
        AtomicReference<StringBuilder> jsonBuffer = new AtomicReference<>(new StringBuilder());

        return chatModel.stream(prompt)
                .filter(Objects::nonNull)
                .filter(chatResponse -> chatResponse.getResult() != null)
                .filter(chatResponse -> chatResponse.getResult().getOutput() != null)
                .map(chatResponse -> {
                    try {
                        String content = chatResponse.getResult().getOutput().getText();
                        return content != null ? content : "";
                    } catch (Exception e) {
                        log.warn("🔥 컨텐츠 추출 실패: {}", e.getMessage());
                        return "";
                    }
                })
                .filter(content -> !content.isEmpty())
                .map(this::cleanTextChunk)
                .filter(chunk -> !chunk.trim().isEmpty())
                .flatMap(chunk -> {
                    // 버퍼에 청크 추가
                    textBuffer.get().append(chunk);

                    // JSON 시작 감지
                    if (!jsonStarted.get() && textBuffer.get().toString().contains("===JSON시작===")) {
                        jsonStarted.set(true);
                        int startIndex = textBuffer.get().toString().indexOf("===JSON시작===");

                        // JSON 시작 전의 텍스트 반환
                        String beforeJson = textBuffer.get().substring(0, startIndex);

                        // JSON 부분만 버퍼에 남기기
                        String afterJsonMarker = textBuffer.get().substring(startIndex + "===JSON시작===".length());
                        textBuffer.set(new StringBuilder(afterJsonMarker));
                        jsonBuffer.set(new StringBuilder());

                        return Flux.just(beforeJson);
                    }

                    // JSON 수집 중
                    if (jsonStarted.get() && !jsonEnded.get()) {
                        String currentText = textBuffer.get().toString();

                        // JSON 종료 감지
                        if (currentText.contains("===JSON끝===")) {
                            jsonEnded.set(true);
                            int endIndex = currentText.indexOf("===JSON끝===");

                            // JSON 컨텐츠 추출
                            String jsonContent = currentText.substring(0, endIndex);
                            jsonBuffer.get().append(jsonContent);

                            // 완전한 JSON 반환
                            String completeJson = "===JSON시작===" + jsonBuffer.get().toString() + "===JSON끝===";

                            // 남은 텍스트 처리
                            String afterJson = currentText.substring(endIndex + "===JSON끝===".length());

                            if (!afterJson.trim().isEmpty()) {
                                return Flux.just(completeJson, afterJson);
                            } else {
                                return Flux.just(completeJson);
                            }
                        } else {
                            // JSON 버퍼에 추가하고 빈 응답 반환 (JSON이 완성될 때까지 대기)
                            jsonBuffer.get().append(currentText);
                            textBuffer.set(new StringBuilder());
                            return Flux.empty();
                        }
                    }

                    // 일반 텍스트 스트리밍
                    if (!jsonStarted.get()) {
                        String content = textBuffer.get().toString();
                        textBuffer.set(new StringBuilder());
                        return Flux.just(content);
                    }

                    // JSON 종료 후 텍스트
                    if (jsonEnded.get()) {
                        String content = textBuffer.get().toString();
                        textBuffer.set(new StringBuilder());
                        return Flux.just(content);
                    }

                    return Flux.empty();
                })
                .filter(text -> !text.isEmpty())
                .doOnNext(chunk -> {
                    if (chunk.contains("===JSON시작===")) {
                        log.debug("🔥 JSON 블록 시작 감지");
                    }
                    if (chunk.contains("===JSON끝===")) {
                        log.debug("🔥 JSON 블록 완료");
                    }
                })
                .doOnError(error -> log.error("🔥 AI 스트리밍 오류", error))
                .onErrorResume(error -> {
                    log.error("🔥 AI 스트리밍 실패, 에러 메시지 반환", error);
                    return Flux.just("ERROR: AI 서비스 연결 실패: " + error.getMessage());
                });
    }

    /**
     * 텍스트 청크 정제 - 한글 인코딩 안정성 확보
     */
    private String cleanTextChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return "";
        }

        try {
            // UTF-8 인코딩 안정성 검증
            byte[] bytes = chunk.getBytes(StandardCharsets.UTF_8);
            String decoded = new String(bytes, StandardCharsets.UTF_8);

            // 불필요한 제어 문자만 제거 (한글은 보존)
            String cleaned = decoded.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

            return cleaned;
        } catch (Exception e) {
            log.warn("🔥 텍스트 청크 정제 실패: {}", e.getMessage());
            return chunk;
        }
    }

    /**
     * 시스템의 실제 메타데이터를 구성합니다.
     */
    private String buildSystemMetadata() {
        return buildSystemMetadata(null);
    }

    private String buildSystemMetadata(io.spring.identityadmin.ai.dto.PolicyGenerationRequest.AvailableItems availableItems) {
        StringBuilder metadata = new StringBuilder();

        if (availableItems != null) {
            // 프론트엔드에서 제공된 사용 가능한 항목들 사용
            metadata.append("🎯 현재 사용 가능한 항목들 (반드시 이 ID들만 사용하세요):\n\n");
            
            // 역할 정보
            if (availableItems.roles() != null && !availableItems.roles().isEmpty()) {
                metadata.append("📋 사용 가능한 역할:\n");
                availableItems.roles().forEach(role ->
                        metadata.append(String.format("- ID: %d, 이름: %s, 설명: %s\n", 
                            role.id(), role.name(), role.description() != null ? role.description() : "")));
            } else {
                metadata.append("📋 사용 가능한 역할: 없음\n");
            }

            // 권한 정보
            if (availableItems.permissions() != null && !availableItems.permissions().isEmpty()) {
                metadata.append("\n🔑 사용 가능한 권한:\n");
                availableItems.permissions().forEach(perm ->
                        metadata.append(String.format("- ID: %d, 이름: %s, 설명: %s\n", 
                            perm.id(), perm.name(), perm.description() != null ? perm.description() : "")));
            } else {
                metadata.append("\n🔑 사용 가능한 권한: 없음\n");
            }

            // 조건 템플릿 정보
            if (availableItems.conditions() != null && !availableItems.conditions().isEmpty()) {
                metadata.append("\n⏰ 사용 가능한 조건 템플릿:\n");
                availableItems.conditions().forEach(cond ->
                        metadata.append(String.format("- ID: %d, 이름: %s, 설명: %s, 호환가능: %s\n", 
                            cond.id(), cond.name(), 
                            cond.description() != null ? cond.description() : "",
                            cond.isCompatible() != null ? cond.isCompatible() : true)));
            } else {
                metadata.append("\n⏰ 사용 가능한 조건 템플릿: 없음\n");
            }
            
            metadata.append("\n⚠️ 경고: 위에 나열된 ID들 외의 다른 ID는 절대 사용하지 마세요. 존재하지 않는 ID를 사용하면 시스템 오류가 발생합니다.\n");
            
        } else {
            // 기존 방식: DB에서 모든 항목 조회
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
        }

        return metadata.toString();
    }

    /**
     * 일반 방식의 정책 생성 (fallback용)
     */
    @Override
    public AiGeneratedPolicyDraftDto generatePolicyFromTextByAi(String naturalLanguageQuery) {
        return generatePolicyFromTextByAi(naturalLanguageQuery, null);
    }

    /**
     * 사용 가능한 항목들을 포함한 정책 생성
     */
    public AiGeneratedPolicyDraftDto generatePolicyFromTextByAi(String naturalLanguageQuery, io.spring.identityadmin.ai.dto.PolicyGenerationRequest.AvailableItems availableItems) {
        // RAG 검색
        SearchRequest searchRequest = SearchRequest.builder()
                .query(naturalLanguageQuery)
                .topK(10)
                .build();
        List<Document> contextDocs = vectorStore.similaritySearch(searchRequest);
        String contextInfo = contextDocs.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));

        String systemMetadata = buildSystemMetadata(availableItems);

        String systemPrompt = String.format("""
            당신은 사용자의 자연어 요구사항을 분석하여, IAM 시스템이 이해할 수 있는 BusinessPolicyDto JSON 객체로 변환하는 AI 에이전트입니다.
            
            🎯 중요: 반드시 아래 제공된 사용 가능한 항목들 중에서만 선택해야 합니다. 존재하지 않는 ID는 절대 사용하지 마세요.
            
            시스템 정보:
            %s
            
            ⚠️ 절대적 JSON 파싱 규칙 (위반 시 시스템 오류):
            1. JSON에 주석 절대 금지 (// 또는 /* */ 사용 금지)
            2. 마크다운 코드 블록 절대 금지 (``` 사용 금지)
            3. JSON 외부에 설명 텍스트 절대 금지
            4. 모든 키와 문자열 값은 쌍따옴표 필수
            5. 마지막 항목 뒤 쉼표 절대 금지
            6. roleIds, permissionIds는 반드시 숫자 배열
            7. conditions 맵의 키는 반드시 문자열 형태의 숫자 ID
            
            **필수 JSON 형식 (정확히 이 형식만 사용):**
            {
              "policyName": "AI가 생성한 정책 이름",
              "description": "AI가 생성한 정책 설명",
              "roleIds": [2, 3],
              "permissionIds": [1, 4],
              "conditional": true,
              "conditions": {"1": ["MONDAY", "TUESDAY"]},
              "aiRiskAssessmentEnabled": false,
              "requiredTrustScore": 0.7,
              "customConditionSpel": "",
              "effect": "ALLOW"
            }
            
            위 형식을 정확히 따라 완벽하게 파싱 가능한 JSON만 출력하세요.
            """, systemMetadata);

        String userPrompt = String.format("""
            **자연어 요구사항:**
            "%s"
            
            **참고 컨텍스트:**
            %s
            """, naturalLanguageQuery, contextInfo);

        // ChatModel을 직접 사용하여 호출
        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(userPrompt);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        ChatResponse response = chatModel.call(prompt);
        String jsonResponse = response.getResult().getOutput().getText();

        try {
            // JSON 정제 적용
            String cleanedJson = extractAndCleanJson(jsonResponse);
            
            // 더 관대한 ObjectMapper 설정으로 파싱
            ObjectMapper lenientMapper = createLenientObjectMapper();
            AiResponseDto aiResponse = lenientMapper.readValue(cleanedJson, AiResponseDto.class);
            BusinessPolicyDto policyData = translateAiResponseToBusinessDto(aiResponse);

            Map<String, String> roleIdToNameMap = getRoleNames(policyData.getRoleIds());
            Map<String, String> permissionIdToNameMap = getPermissionNames(policyData.getPermissionIds());
            Map<String, String> conditionIdToNameMap = getConditionTemplateNames(policyData.getConditions());

            return new AiGeneratedPolicyDraftDto(policyData, roleIdToNameMap, permissionIdToNameMap, conditionIdToNameMap);

        } catch (com.fasterxml.jackson.core.JsonParseException jpe) {
            log.error("🔥 JSON 파싱 오류 (JsonParseException): {} - AI Response: {}", jpe.getMessage(), jsonResponse);
            log.error("🔥 오류 위치: {}", jpe.getLocation() != null ? jpe.getLocation().toString() : "unknown");
            
            // JsonParseException의 경우 fallback 처리
            return createFallbackPolicyData(naturalLanguageQuery);
            
        } catch (JsonProcessingException jpe) {
            log.error("🔥 JSON 처리 오류 (JsonProcessingException): {} - AI Response: {}", jpe.getMessage(), jsonResponse);
            
            // JSON 처리 오류의 경우도 fallback 처리
            return createFallbackPolicyData(naturalLanguageQuery);
            
        } catch (Exception e) {
            log.error("🔥 AI 정책 생성 또는 파싱에 실패했습니다. AI Response: {}", jsonResponse, e);

            // 기타 오류의 경우 fallback 처리
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
     * JSON 문자열 정제 메서드 개선 - 주석 제거 및 파싱 오류 방지
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

        // 2. 주석 제거 (JsonParseException 방지)
        cleaned = removeJsonComments(cleaned);

        // 3. JSON 시작과 끝 찾기
        int jsonStart = cleaned.indexOf('{');
        int jsonEnd = findMatchingBrace(cleaned, jsonStart);

        if (jsonStart != -1 && jsonEnd != -1) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
        }

        // 4. 잘못된 쉼표 제거
        cleaned = cleaned.replaceAll(",\\s*([}\\]])", "$1");

        // 5. 추가 JSON 구조 검증 및 수정
        cleaned = fixJsonStructure(cleaned);

        log.debug("🔥 정제된 JSON 길이: {}", cleaned.length());
        return cleaned;
    }

    /**
     * JSON에서 주석 제거 (JsonParseException 방지)
     */
    private String removeJsonComments(String json) {
        // 1. 한 줄 주석 제거 (//)
        json = json.replaceAll("//.*", "");
        
        // 2. 블록 주석 제거 (/* */)
        json = json.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        
        // 3. 문자열 내부가 아닌 곳의 주석만 제거하도록 개선
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            char next = (i + 1 < json.length()) ? json.charAt(i + 1) : '\0';
            
            if (escaped) {
                result.append(c);
                escaped = false;
                continue;
            }
            
            if (c == '\\' && inString) {
                escaped = true;
                result.append(c);
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                result.append(c);
                continue;
            }
            
            if (!inString && c == '/' && next == '/') {
                // 한 줄 주석 시작 - 줄 끝까지 스킵
                while (i < json.length() && json.charAt(i) != '\n') {
                    i++;
                }
                if (i < json.length()) {
                    result.append('\n'); // 줄바꿈 유지
                }
                continue;
            }
            
            if (!inString && c == '/' && next == '*') {
                // 블록 주석 시작 - */ 까지 스킵
                i += 2;
                while (i + 1 < json.length() && !(json.charAt(i) == '*' && json.charAt(i + 1) == '/')) {
                    i++;
                }
                i++; // */ 의 / 까지 스킵
                continue;
            }
            
            result.append(c);
        }
        
        return result.toString();
    }

    /**
     * JSON 구조 수정 (추가 오류 방지)
     */
    private String fixJsonStructure(String json) {
        // 1. 빈 값 처리
        json = json.replaceAll(":\\s*,", ": \"\",");
        json = json.replaceAll(":\\s*}", ": \"\"");
        
        // 2. 중복 쉼표 제거
        json = json.replaceAll(",\\s*,", ",");
        
        // 3. 시작/끝 쉼표 제거
        json = json.replaceAll("\\{\\s*,", "{");
        json = json.replaceAll(",\\s*}", "}");
        
        // 4. 따옴표 없는 키 수정 (간단한 경우만)
        json = json.replaceAll("(\\w+)\\s*:", "\"$1\":");
        
        return json;
    }

    /**
     * 관대한 ObjectMapper 생성 (JSON 파싱 오류 방지)
     */
    private ObjectMapper createLenientObjectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)  // 주석 허용
                .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)  // 마지막 쉼표 허용
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)  // 단일 따옴표 허용
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)  // 따옴표 없는 필드명 허용
                .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)  // 백슬래시 이스케이프 허용
                .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);  // NaN, Infinity 허용
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
        // 1. RAG 패턴: Vector DB 에서 관련 과거 접근 기록 검색
        SearchRequest searchRequest = SearchRequest.builder()
                .query(context.subject().getName() + " " + context.resource().identifier())
                .topK(5)
                .build();
        List<Document> history = vectorStore.similaritySearch(searchRequest);
        String historyContent = history.stream().map(Document::getText).collect(Collectors.joining("\n"));

        UserDto user = (UserDto) context.subject().getPrincipal();

        // 2. 시스템 및 사용자 메시지 구성
        String systemPrompt = """
            당신은 IAM 시스템의 모든 컨텍스트를 분석하여 접근 요청의 신뢰도를 판결하는 AI 보안 전문가 '아비터(Arbiter)'입니다.
            당신은 반드시 연쇄적 추론(Chain-of-Thought) 방식으로 분석을 수행한 뒤, 최종 결론을 JSON 형식으로만 반환해야 합니다.
            JSON 형식: {"score": 0.xx, "riskTags": ["위험_태그"], "summary": "한국어 요약 설명"}
            """;

        String userPrompt = String.format("""
            **1. 현재 접근 요청 상세 정보:**
            - 사용자: %s (ID: %s)
            - 역할: %s
            - 소속 그룹: %s
            - 접근 리소스: %s
            - 요청 행위: %s
            - 접속 IP 주소: %s
            
            **2. 해당 사용자의 과거 접근 패턴 요약 (최근 5건):**
            %s
            
            **3. 분석 및 평가:**
            위 정보를 바탕으로, 다음 단계에 따라 현재 접근 요청의 위험도를 분석하고 신뢰도를 평가하라.
            - **Anomalies (이상 징후):** 과거 패턴과 비교하여 현재 요청에서 나타나는 이상 징후(예: 새로운 IP, 평소와 다른 시간대, 접근한 적 없는 리소스)를 모두 찾아 목록으로 나열하라.
            - **Reasoning (추론 과정):** 식별된 이상 징후와 사용자의 역할/권한을 종합하여, 이 요청이 왜 위험하거나 안전하다고 판단했는지 그 이유를 단계별로 설명하라.
            - **Final Assessment (최종 판결):** 위 분석을 바탕으로 최종 신뢰도 점수(score), 위험 태그(riskTags), 그리고 한국어 요약(summary)을 결정하라.
            """,
                user.getName(), user.getUsername(),
                context.attributes().getOrDefault("userRoles", "N/A"),
                context.attributes().getOrDefault("userGroups", "N/A"),
                context.resource().identifier(),
                context.action(),
                context.environment().remoteIp() != null ? context.environment().remoteIp() : "알 수 없음",
                historyContent
        );

        // ChatModel 직접 사용
        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(userPrompt);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        ChatResponse response = chatModel.call(prompt);
        String jsonResponse = response.getResult().getOutput().getText();

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
            log.warn("🔥 suggestResourceNamesInBatch: 입력 데이터가 비어있습니다.");
            return Map.of();
        }

        log.info("🔥 AI 배치 추천 시작 - 요청 리소스 수: {}", resourcesToSuggest.size());

        // 입력 데이터 로깅
        resourcesToSuggest.forEach(resource ->
                log.debug("🔥 요청 리소스: identifier={}, owner={}",
                        resource.get("identifier"), resource.get("owner")));

        // 배치 크기 제한 (AI 응답 품질 향상을 위해)
        final int BATCH_SIZE = 5; // 10에서 5로 줄여서 AI 정확도 향상
        Map<String, ResourceNameSuggestion> allResults = new HashMap<>();

        // 배치 처리
        for (int i = 0; i < resourcesToSuggest.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, resourcesToSuggest.size());
            List<Map<String, String>> batch = resourcesToSuggest.subList(i, endIndex);

            log.info("🔥 배치 처리 중: {}/{} (배치 크기: {})",
                    i + 1, resourcesToSuggest.size(), batch.size());

            Map<String, ResourceNameSuggestion> batchResult = processBatch(batch);
            allResults.putAll(batchResult);
        }

        // 누락된 항목에 대한 fallback 처리 - AI 디버깅을 위해 주석처리
    /*
    for (Map<String, String> resource : resourcesToSuggest) {
        String identifier = resource.get("identifier");
        if (!allResults.containsKey(identifier)) {
            log.warn("🔥 AI 응답에서 누락된 항목 발견, fallback 생성: {}", identifier);
            allResults.put(identifier, new ResourceNameSuggestion(
                    generateFallbackFriendlyName(identifier),
                    "AI 추천을 받지 못한 항목입니다."
            ));
        }
    }
    */

        // AI 응답 누락 검증 (fallback 없이 경고만)
        for (Map<String, String> resource : resourcesToSuggest) {
            String identifier = resource.get("identifier");
            if (!allResults.containsKey(identifier)) {
                log.error("🔥 [AI 오류] 응답에서 누락됨: {}", identifier);
            }
        }

        log.info("🔥 최종 결과 - 총 항목 수: {}", allResults.size());
        return allResults;
    }

    private Map<String, ResourceNameSuggestion> processBatch(List<Map<String, String>> batch) {
        // 개선된 시스템 프롬프트
        String systemPrompt = """
    당신은 소프트웨어의 기술적 용어를 일반 비즈니스 사용자가 이해하기 쉬운 이름과 설명으로 만드는 네이밍 전문가입니다.
    
    **매우 중요한 규칙:**
    1. 제공된 모든 항목(identifier)에 대해 예외 없이 응답해야 합니다
    2. 각 항목마다 반드시 friendlyName과 description을 모두 제공해야 합니다
    3. 순수한 JSON 형식으로만 응답하세요 (설명 텍스트 없음)
    4. 한글로 친화적이고 명확한 이름과 설명을 작성하세요
    5. 영문 메서드명도 반드시 포함하여 응답하세요
    6. 입력된 순서대로 모든 항목을 응답하세요
    
    **처리 규칙:**
    - camelCase나 snake_case는 읽기 쉬운 한글로 변환
    - URL 경로는 기능 이름으로 변환 (예: /admin/users → 사용자 관리)
    - 메서드명은 동작을 나타내는 한글로 변환 (예: updateUser → 사용자 정보 수정)
    - CRUD 작업은 명확한 동사 사용 (생성, 조회, 수정, 삭제)
    
    **응답 형식 (반드시 이 형식을 따르세요):**
    {
      "첫번째_identifier": {
        "friendlyName": "친화적 이름",
        "description": "상세 설명"
      },
      "두번째_identifier": {
        "friendlyName": "친화적 이름",
        "description": "상세 설명"
      }
    }
    
    절대 항목을 누락하지 마세요. 모든 입력에 대해 응답하세요.
    """;

        try {
            // 입력 데이터를 더 간단한 형식으로 변환
            List<String> identifiersList = new ArrayList<>();
            for (Map<String, String> resource : batch) {
                String identifier = resource.get("identifier");
                identifiersList.add(identifier);
            }

            // 리스트 형식으로 입력 제공 (더 명확함)
            String inputText = "다음 " + identifiersList.size() + "개의 기술 항목에 대해 모두 응답하세요:\n\n";
            for (int i = 0; i < identifiersList.size(); i++) {
                inputText += (i + 1) + ". " + identifiersList.get(i) + "\n";
            }

            log.info("🔥 AI에게 전송할 배치 (크기: {}):\n{}", batch.size(), inputText);

            SystemMessage systemMessage = new SystemMessage(systemPrompt);
            UserMessage userMessage = new UserMessage(inputText);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

            ChatResponse response = chatModel.call(prompt);
            String jsonResponse = response.getResult().getOutput().getText();

            log.info("🔥 AI 원본 응답 길이: {}", jsonResponse.length());
            log.debug("🔥 AI 원본 응답: {}", jsonResponse);

            // 강화된 JSON 파싱
            Map<String, ResourceNameSuggestion> result = parseAiResponseEnhanced(jsonResponse, batch);

            // 응답 검증
            log.info("🔥 배치 크기: {}, 파싱된 항목 수: {}", batch.size(), result.size());
            if (result.size() < batch.size()) {
                log.error("🔥 [AI 오류] 일부 항목 누락! 요청: {}, 응답: {}", batch.size(), result.size());

                // 누락된 항목 상세 로깅
                Set<String> requested = identifiersList.stream().collect(Collectors.toSet());
                Set<String> responded = result.keySet();
                requested.removeAll(responded);
                log.error("🔥 [AI 오류] 누락된 항목들: {}", requested);
            }

            return result;

        } catch (Exception e) {
            log.error("🔥 배치 처리 중 오류 발생", e);

            // AI 오류 시 빈 맵 반환하여 문제점 명확히 파악
            log.error("🔥 [AI 오류] 배치 처리 완전 실패, 빈 결과 반환");
            return new HashMap<>();
        }
    }

    /**
     * 강화된 AI 응답 파싱 메서드
     */
    private Map<String, ResourceNameSuggestion> parseAiResponseEnhanced(String jsonResponse, List<Map<String, String>> originalBatch) {
        Map<String, ResourceNameSuggestion> result = new HashMap<>();

        try {
            // 1단계: JSON 정제
            String cleanedJson = cleanJsonResponse(jsonResponse);
            log.debug("🔥 정제된 JSON: {}", cleanedJson);

            // 2단계: 다양한 파싱 전략 시도
            result = tryMultipleParsingStrategies(cleanedJson);

            // 3단계: 파싱 결과 검증
            if (result.isEmpty()) {
                log.warn("🔥 모든 파싱 전략 실패, 정규식 파싱 시도");
                result = regexParsing(cleanedJson);
            }

            // 4단계: 누락된 항목 확인 및 보완 - AI 디버깅을 위해 주석처리
            Set<String> requestedIdentifiers = originalBatch.stream()
                    .map(m -> m.get("identifier"))
                    .collect(Collectors.toSet());

            Set<String> parsedIdentifiers = result.keySet();
            Set<String> missingIdentifiers = new HashSet<>(requestedIdentifiers);
            missingIdentifiers.removeAll(parsedIdentifiers);

            if (!missingIdentifiers.isEmpty()) {
                log.error("🔥 [AI 오류] 파싱 후에도 누락된 항목: {}", missingIdentifiers);
                // fallback 처리 주석
            /*
            for (String missing : missingIdentifiers) {
                result.put(missing, new ResourceNameSuggestion(
                        generateFallbackFriendlyName(missing),
                        "AI 응답에서 누락된 항목입니다."
                ));
            }
            */
            }

        } catch (Exception e) {
            log.error("🔥 강화된 파싱 실패", e);

            // 전체 실패 시 모든 항목에 대해 fallback - AI 디버깅을 위해 주석처리
        /*
        for (Map<String, String> resource : originalBatch) {
            String identifier = resource.get("identifier");
            result.put(identifier, new ResourceNameSuggestion(
                    generateFallbackFriendlyName(identifier),
                    "파싱 오류로 인한 기본값"
            ));
        }
        */

            // AI 오류를 명확히 파악하기 위해 빈 결과 반환
            log.error("🔥 [AI 오류] 모든 파싱 전략 실패");
        }

        return result;
    }

    /**
     * JSON 응답 정제 - 더 강력한 정제
     */
    private String cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }

        String cleaned = response.trim();

        // 1. 마크다운 제거
        cleaned = cleaned.replaceAll("```json\\s*", "");
        cleaned = cleaned.replaceAll("```\\s*", "");

        // 2. JSON 앞뒤 텍스트 제거
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        // 3. 이스케이프 문자 정규화
        cleaned = normalizeEscapes(cleaned);

        // 4. 유니코드 이스케이프 처리
        cleaned = decodeUnicode(cleaned);

        // 5. 잘못된 쉼표 제거
        cleaned = cleaned.replaceAll(",\\s*}", "}");
        cleaned = cleaned.replaceAll(",\\s*]", "]");

        return cleaned;
    }

    /**
     * 다양한 파싱 전략 시도
     */
    private Map<String, ResourceNameSuggestion> tryMultipleParsingStrategies(String json) {
        Map<String, ResourceNameSuggestion> result = new HashMap<>();

        // 전략 1: 표준 ObjectMapper
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                    .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

            Map<String, Map<String, String>> parsed = mapper.readValue(
                    json,
                    new TypeReference<Map<String, Map<String, String>>>() {}
            );

            for (Map.Entry<String, Map<String, String>> entry : parsed.entrySet()) {
                String friendlyName = entry.getValue().get("friendlyName");
                String description = entry.getValue().get("description");

                if (friendlyName != null && description != null) {
                    result.put(entry.getKey(), new ResourceNameSuggestion(friendlyName, description));
                }
            }

            if (!result.isEmpty()) {
                log.info("🔥 표준 파싱 성공, 항목 수: {}", result.size());
                return result;
            }
        } catch (Exception e) {
            log.debug("🔥 표준 파싱 실패: {}", e.getMessage());
        }

        // 전략 2: JsonNode 사용
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            if (root.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String key = field.getKey();
                    JsonNode value = field.getValue();

                    if (value.has("friendlyName") && value.has("description")) {
                        String friendlyName = value.get("friendlyName").asText();
                        String description = value.get("description").asText();
                        result.put(key, new ResourceNameSuggestion(friendlyName, description));
                    }
                }
            }

            if (!result.isEmpty()) {
                log.info("🔥 JsonNode 파싱 성공, 항목 수: {}", result.size());
                return result;
            }
        } catch (Exception e) {
            log.debug("🔥 JsonNode 파싱 실패: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 정규식을 사용한 최후의 파싱
     */
    private Map<String, ResourceNameSuggestion> regexParsing(String json) {
        Map<String, ResourceNameSuggestion> result = new HashMap<>();

        // 패턴: "identifier": {"friendlyName": "name", "description": "desc"}
        Pattern pattern = Pattern.compile(
                "\"([^\"]+)\"\\s*:\\s*\\{\\s*\"friendlyName\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"description\"\\s*:\\s*\"([^\"]+)\"\\s*\\}",
                Pattern.MULTILINE | Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            String identifier = matcher.group(1);
            String friendlyName = matcher.group(2);
            String description = matcher.group(3);

            if (identifier != null && friendlyName != null && description != null) {
                result.put(identifier, new ResourceNameSuggestion(friendlyName, description));
                log.debug("🔥 정규식 파싱 성공: {} -> {}", identifier, friendlyName);
            }
        }

        log.info("🔥 정규식 파싱 결과, 항목 수: {}", result.size());
        return result;
    }

    /**
     * 이스케이프 문자 정규화
     */
    private String normalizeEscapes(String text) {
        // 줄바꿈 정규화
        text = text.replace("\\n", " ");
        text = text.replace("\\r", "");
        text = text.replace("\\t", " ");

        // 연속된 공백 제거
        text = text.replaceAll("\\s+", " ");

        return text;
    }

    /**
     * 유니코드 이스케이프 디코딩
     */
    private String decodeUnicode(String text) {
        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(text);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int codePoint = Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(sb, String.valueOf((char) codePoint));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * AI 응답을 파싱하는 개선된 메서드
     */
    private Map<String, ResourceNameSuggestion> parseAiResponse(String jsonStr) throws Exception {
        log.debug("🔥 파싱 시작, JSON 길이: {}, 첫 100자: {}",
                jsonStr.length(),
                jsonStr.substring(0, Math.min(100, jsonStr.length())));

        // 빈 JSON 체크
        if (jsonStr.trim().equals("{}") || jsonStr.trim().isEmpty()) {
            log.warn("🔥 빈 JSON 응답 감지");
            return new HashMap<>();
        }

        // 더 유연한 ObjectMapper 사용
        ObjectMapper lenientMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

        try {
            // 1차 시도: 일반 파싱
            Map<String, Map<String, String>> rawResponseMap = lenientMapper.readValue(
                    jsonStr,
                    new TypeReference<Map<String, Map<String, String>>>() {}
            );

            // ResourceNameSuggestion 객체로 변환
            return convertToResourceNameSuggestions(rawResponseMap);

        } catch (Exception e) {
            log.warn("🔥 1차 파싱 실패, 복구 시도: {}", e.getMessage());

            // 2차 시도: JSON 구조 분석 후 복구
            String analyzedJson = analyzeAndFixJsonStructure(jsonStr);

            if (analyzedJson != null && !analyzedJson.equals(jsonStr)) {
                try {
                    Map<String, Map<String, String>> rawResponseMap = lenientMapper.readValue(
                            analyzedJson,
                            new TypeReference<Map<String, Map<String, String>>>() {}
                    );
                    return convertToResourceNameSuggestions(rawResponseMap);
                } catch (Exception e2) {
                    log.warn("🔥 구조 분석 후 파싱도 실패: {}", e2.getMessage());
                }
            }

            // 3차 시도: JSON 복구
            String repairedJson = repairJson(jsonStr);
            log.debug("🔥 복구된 JSON: {}", repairedJson);

            try {
                Map<String, Map<String, String>> rawResponseMap = lenientMapper.readValue(
                        repairedJson,
                        new TypeReference<Map<String, Map<String, String>>>() {}
                );

                return convertToResourceNameSuggestions(rawResponseMap);
            } catch (Exception e3) {
                log.error("🔥 3차 파싱도 실패: {}", e3.getMessage());

                // 4차 시도: 수동 파싱
                return manualJsonParse(jsonStr);
            }
        }
    }

    /**
     * JSON 구조를 분석하고 수정하는 메서드
     */
    private String analyzeAndFixJsonStructure(String json) {
        try {
            // 잘못된 형식 패턴 감지 및 수정
            // 패턴 1: {"friendlyName": "이름", "description": "설명"} 형태가 최상위에 있는 경우
            if (json.trim().startsWith("{") && json.contains("\"friendlyName\"") && !json.contains(":{")) {
                log.info("🔥 잘못된 JSON 구조 감지: 최상위에 friendlyName이 직접 있음");
                // 임시 키로 감싸기
                return "{\"temp_key\": " + json + "}";
            }

            // 패턴 2: 값이 문자열로만 되어 있는 경우
            // 예: {"key": "value"} -> {"key": {"friendlyName": "value", "description": "설명 없음"}}
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode root = mapper.readTree(json);
                if (root.isObject()) {
                    ObjectNode newRoot = mapper.createObjectNode();
                    Iterator<Map.Entry<String, JsonNode>> fields = root.fields();

                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        String key = field.getKey();
                        JsonNode value = field.getValue();

                        if (value.isTextual()) {
                            // 문자열 값을 객체로 변환
                            ObjectNode newValue = mapper.createObjectNode();
                            newValue.put("friendlyName", value.asText());
                            newValue.put("description", "AI가 설명을 제공하지 않았습니다.");
                            newRoot.set(key, newValue);
                        } else if (value.isObject() && (!value.has("friendlyName") || !value.has("description"))) {
                            // 필수 필드가 없는 객체 수정
                            ObjectNode objValue = (ObjectNode) value;
                            if (!objValue.has("friendlyName")) {
                                objValue.put("friendlyName", key);
                            }
                            if (!objValue.has("description")) {
                                objValue.put("description", "설명 없음");
                            }
                            newRoot.set(key, objValue);
                        } else {
                            newRoot.set(key, value);
                        }
                    }

                    return mapper.writeValueAsString(newRoot);
                }
            } catch (Exception e) {
                log.debug("🔥 JSON 구조 분석 중 오류: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("🔥 JSON 구조 수정 실패: {}", e.getMessage());
        }

        return json;
    }

    /**
     * JSON 복구 메서드 (개선된 버전)
     */
    private String repairJson(String json) {
        String repaired = json.trim();

        // 1. 잘못된 백슬래시 수정
        repaired = repaired.replaceAll("\\\\(?![\"\\\\nrtbf/])", "\\\\\\\\");

        // 2. 잘못된 쉼표 제거
        repaired = repaired.replaceAll(",\\s*}", "}");
        repaired = repaired.replaceAll(",\\s*]", "]");

        // 3. 이스케이프되지 않은 따옴표 처리
        // 문자열 내부의 따옴표만 이스케이프
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < repaired.length(); i++) {
            char c = repaired.charAt(i);

            if (!escaped && c == '"') {
                if (inString && i + 1 < repaired.length() && repaired.charAt(i + 1) == '"') {
                    // 연속된 따옴표 발견
                    sb.append("\\\"");
                    i++; // 다음 따옴표 건너뛰기
                } else {
                    inString = !inString;
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }

            escaped = (c == '\\' && !escaped);
        }

        repaired = sb.toString();

        // 4. 줄바꿈 문자 이스케이프
        if (!repaired.contains("\\n")) {
            repaired = repaired.replaceAll("\n", "\\\\n");
        }
        if (!repaired.contains("\\r")) {
            repaired = repaired.replaceAll("\r", "\\\\r");
        }

        // 5. 불완전한 JSON 마무리
        long openBraces = repaired.chars().filter(c -> c == '{').count();
        long closeBraces = repaired.chars().filter(c -> c == '}').count();

        while (openBraces > closeBraces) {
            // 마지막 항목이 완전한지 확인
            int lastComma = repaired.lastIndexOf(',');
            int lastCloseBrace = repaired.lastIndexOf('}');

            if (lastComma > lastCloseBrace) {
                // 불완전한 항목 제거
                repaired = repaired.substring(0, lastComma);
            }

            repaired += "}";
            closeBraces++;
        }

        return repaired;
    }

    /**
     * 수동 JSON 파싱 (최후의 수단) - 개선된 버전
     */
    private Map<String, ResourceNameSuggestion> manualJsonParse(String json) {
        log.info("🔥 수동 JSON 파싱 시작");
        Map<String, ResourceNameSuggestion> result = new HashMap<>();

        try {
            // 여러 패턴 시도
            List<Pattern> patterns = Arrays.asList(
                    // 패턴 1: 표준 형식
                    Pattern.compile(
                            "\"([^\"]+)\"\\s*:\\s*\\{\\s*\"friendlyName\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"description\"\\s*:\\s*\"([^\"]+)\"\\s*\\}",
                            Pattern.MULTILINE | Pattern.DOTALL
                    ),
                    // 패턴 2: description이 먼저 오는 경우
                    Pattern.compile(
                            "\"([^\"]+)\"\\s*:\\s*\\{\\s*\"description\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"friendlyName\"\\s*:\\s*\"([^\"]+)\"\\s*\\}",
                            Pattern.MULTILINE | Pattern.DOTALL
                    ),
                    // 패턴 3: 한 필드만 있는 경우 (friendlyName만)
                    Pattern.compile(
                            "\"([^\"]+)\"\\s*:\\s*\\{\\s*\"friendlyName\"\\s*:\\s*\"([^\"]+)\"\\s*\\}",
                            Pattern.MULTILINE | Pattern.DOTALL
                    ),
                    // 패턴 4: 단순 키-값 형태
                    Pattern.compile(
                            "\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"",
                            Pattern.MULTILINE
                    )
            );

            for (int i = 0; i < patterns.size(); i++) {
                Pattern pattern = patterns.get(i);
                Matcher matcher = pattern.matcher(json);

                while (matcher.find()) {
                    String identifier = matcher.group(1);

                    if (i == 0) {
                        // 표준 형식
                        String friendlyName = matcher.group(2);
                        String description = matcher.group(3);
                        result.put(identifier.trim(), new ResourceNameSuggestion(friendlyName.trim(), description.trim()));
                    } else if (i == 1) {
                        // description이 먼저
                        String description = matcher.group(2);
                        String friendlyName = matcher.group(3);
                        result.put(identifier.trim(), new ResourceNameSuggestion(friendlyName.trim(), description.trim()));
                    } else if (i == 2) {
                        // friendlyName만
                        String friendlyName = matcher.group(2);
                        result.put(identifier.trim(), new ResourceNameSuggestion(friendlyName.trim(), "설명 없음"));
                    } else if (i == 3 && !result.containsKey(identifier)) {
                        // 단순 키-값 (이미 파싱된 항목은 덮어쓰지 않음)
                        String value = matcher.group(2);
                        result.put(identifier.trim(), new ResourceNameSuggestion(value.trim(), "AI가 설명을 제공하지 않았습니다."));
                    }

                    log.debug("🔥 수동 파싱 성공 (패턴 {}): {} -> {}",
                            i + 1, identifier, result.get(identifier.trim()).friendlyName());
                }
            }

            if (result.isEmpty()) {
                log.warn("🔥 수동 파싱으로도 항목을 찾을 수 없음");

                // 최후의 시도: JsonNode로 부분 파싱
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(json);

                    if (root.isObject()) {
                        Iterator<String> fieldNames = root.fieldNames();
                        while (fieldNames.hasNext()) {
                            String fieldName = fieldNames.next();
                            JsonNode value = root.get(fieldName);

                            if (value.isTextual()) {
                                // 텍스트 값만 있는 경우
                                result.put(fieldName, new ResourceNameSuggestion(
                                        value.asText(),
                                        "AI가 설명을 제공하지 않았습니다."
                                ));
                            } else if (value.isObject()) {
                                // 객체인 경우 가능한 필드 추출
                                String friendlyName = fieldName;
                                String description = "설명 없음";

                                if (value.has("friendlyName")) {
                                    friendlyName = value.get("friendlyName").asText();
                                }
                                if (value.has("description")) {
                                    description = value.get("description").asText();
                                }

                                result.put(fieldName, new ResourceNameSuggestion(friendlyName, description));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("🔥 JsonNode 파싱도 실패: {}", e.getMessage());
                }
            }

            log.info("🔥 수동 파싱 완료, 찾은 항목 수: {}", result.size());

        } catch (Exception e) {
            log.error("🔥 수동 파싱 실패: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Map을 ResourceNameSuggestion으로 변환
     */
    private Map<String, ResourceNameSuggestion> convertToResourceNameSuggestions(
            Map<String, Map<String, String>> rawResponseMap) {

        Map<String, ResourceNameSuggestion> result = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : rawResponseMap.entrySet()) {
            String key = entry.getKey();
            Map<String, String> suggestionData = entry.getValue();

            String friendlyName = suggestionData.get("friendlyName");
            String description = suggestionData.get("description");

            // 필수 필드 검증
            if (friendlyName == null || friendlyName.trim().isEmpty()) {
                friendlyName = generateFallbackFriendlyName(key);
                log.warn("🔥 friendlyName이 없어 기본값 사용: {}", friendlyName);
            }

            if (description == null || description.trim().isEmpty()) {
                description = "AI가 설명을 생성하지 못했습니다.";
                log.warn("🔥 description이 없어 기본값 사용");
            }

            result.put(key, new ResourceNameSuggestion(friendlyName.trim(), description.trim()));
        }

        return result;
    }

    /**
     * Fallback용 기본 친화적 이름 생성 (기존 메서드 유지)
     */
    private String generateFallbackFriendlyName(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "알 수 없는 리소스";
        }

        // URL 경로에서 마지막 부분 추출
        if (identifier.startsWith("/")) {
            String[] parts = identifier.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                if (!parts[i].isEmpty() && !parts[i].matches("\\{.*\\}")) {
                    return parts[i] + " 기능";
                }
            }
        }

        // 메서드명에서 이름 추출
        if (identifier.contains(".")) {
            String[] parts = identifier.split("\\.");
            String lastPart = parts[parts.length - 1];
            if (lastPart.contains("()")) {
                lastPart = lastPart.replace("()", "");
            }
            // camelCase를 공백으로 분리
            String formatted = lastPart.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
            return formatted + " 기능";
        }

        return identifier + " 기능";
    }

    @Override
    public ResourceNameSuggestion suggestResourceName(String technicalIdentifier, String serviceOwner) {
        String systemPrompt = """
            당신은 소프트웨어의 기술적 용어를 일반 비즈니스 사용자가 이해하기 쉬운 이름과 설명으로 만드는 네이밍 전문가입니다.
            주어진 기술 정보를 바탕으로, IAM 관리자가 쉽게 이해할 수 있도록 명확하고 직관적인 '친화적 이름(friendlyName)'과 '설명(description)'을 한국어로 추천해주세요.
            응답은 반드시 아래 명시된 JSON 형식으로만 제공해야 합니다.
            JSON 형식: {"friendlyName": "추천 이름", "description": "상세 설명"}
            """;

        String userPrompt = String.format("""
            - 소유 서비스: %s
            - 기술 식별자: %s
            """, serviceOwner, technicalIdentifier);

        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(userPrompt);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        ChatResponse response = chatModel.call(prompt);
        String jsonResponse = response.getResult().getOutput().getText();

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

        // 3. AI 프롬프트 구성 및 ChatModel 호출
        String systemPrompt = """
            당신은 조직의 역할(Role) 할당을 최적화하는 IAM 컨설턴트입니다.
            '대상 사용자'와 '유사 동료 그룹'의 정보를 바탕으로, 대상 사용자에게 가장 필요할 것으로 보이는 역할을 최대 3개까지 추천해주세요.
            
            **당신의 임무:**
            1. 유사 동료 그룹이 공통적으로 가지고 있지만, 대상 사용자는 없는 역할을 후보로 식별합니다.
            2. 후보 역할들 중에서 대상 사용자의 프로필에 가장 적합하다고 판단되는 역할을 최대 3개 선정합니다.
            3. 각 추천 역할에 대해, 왜 추천하는지에 대한 명확한 한글 이유와 0.0에서 1.0 사이의 추천 신뢰도 점수를 부여합니다.
            
            **응답 형식 (JSON 배열만):**
            [{"roleId": 123, "roleName": "추천 역할명", "reason": "추천 이유", "confidence": 0.xx}]
            """;

        String userPrompt = String.format("""
            **분석 정보:**
            - 대상 사용자: %s
            - 대상 사용자의 현재 역할: %s
            - 유사 동료 그룹의 프로필 및 보유 역할 정보: %s
            """,
                userProfileQuery,
                String.join(", ", currentUserRoles),
                similarUserDocs.stream().map(Document::getText).collect(Collectors.joining("\n---\n"))
        );

        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(userPrompt);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        ChatResponse response = chatModel.call(prompt);
        String jsonResponse = response.getResult().getOutput().getText();

        // 4. AI의 JSON 응답을 DTO 리스트로 변환하여 반환
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

        String systemPrompt = """
            당신은 최고 수준의 IAM 보안 감사관입니다.
            다음은 우리 시스템에 존재하는 모든 접근 제어 정책 목록(JSON 형식)입니다.
            
            **당신의 임무:**
            1. 전체 정책들을 면밀히 분석하여, 잠재적인 보안 위험이나 비효율성을 식별합니다.
            2. 특히 '직무 분리(SoD) 원칙 위배' 가능성, '과도한 권한(Over-permissioned)' 정책, '장기 미사용(Dormant)'으로 의심되는 권한 등을 중점적으로 찾아냅니다.
            3. 발견된 각 항목에 대해, 문제 유형, 상세 설명, 그리고 개선을 위한 권장 사항을 포함하여 보고서를 작성합니다.
            
            **응답 형식 (JSON 배열만):**
            [{"insightType": "문제 유형(예: SOD_VIOLATION)", "description": "상세 설명", "relatedEntityIds": [관련 정책/역할 ID], "recommendation": "개선 권장 사항"}]
            """;

        String userPrompt = String.format("""
            **전체 정책 목록:**
            %s
            """, String.join("\n", allPolicies));

        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(userPrompt);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        ChatResponse response = chatModel.call(prompt);
        String jsonResponse = response.getResult().getOutput().getText();

        // 4. AI의 JSON 응답을 DTO 리스트로 변환하여 반환
        try {
            return objectMapper.readValue(jsonResponse, new TypeReference<List<PolicyAnalysisReport>>() {});
        } catch (Exception e) {
            log.error("AI 보안 상태 분석 응답을 파싱하는 데 실패했습니다: {}", jsonResponse, e);
            return List.of();
        }
    }

    @Override
    @Transactional
    public PolicyDto generatePolicyFromText(String naturalLanguageQuery) {
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

        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(naturalLanguageQuery);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        ChatResponse response = chatModel.call(prompt);
        String jsonResponse = response.getResult().getOutput().getText();

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

    /**
     * 🔄 [3단계 완성] 조건 호환성 서비스 + AI 고급 검증을 결합한 Just-in-Time Validation
     * 관리자가 빌더에서 조건을 선택하는 순간, 3단계 검증을 수행합니다.
     */
    @Override
    public ConditionValidationResponse validateCondition(String resourceIdentifier, String conditionSpel) {
        log.info("🔍 3단계 조건 검증 시작: 리소스={}, SpEL={}", resourceIdentifier, conditionSpel);
        
        try {
            // 0단계: 리소스 정보 조회
            ManagedResource resource = managedResourceRepository.findByResourceIdentifier(resourceIdentifier)
                    .orElse(null);
            
            if (resource == null) {
                log.warn("⚠️ 리소스를 찾을 수 없습니다: {}", resourceIdentifier);
                return new ConditionValidationResponse(false, "리소스를 찾을 수 없습니다: " + resourceIdentifier);
            }

            // 🔄 1단계: 조건 호환성 서비스를 통한 기본 호환성 검증
            ConditionTemplate tempCondition = new ConditionTemplate();
            tempCondition.setSpelTemplate(conditionSpel);
            tempCondition.setClassification(ConditionTemplate.ConditionClassification.CUSTOM_COMPLEX);
            
            ConditionCompatibilityService.CompatibilityResult compatibilityResult = 
                conditionCompatibilityService.checkCompatibility(tempCondition, resource);
                
            log.debug("🔍 1단계 호환성 검사 결과: {}", compatibilityResult.isCompatible());
            
            // 호환성 검사 실패 시 즉시 반환 (AI 검증 생략)
            if (!compatibilityResult.isCompatible()) {
                log.info("❌ 1단계 실패: {}", compatibilityResult.getReason());
                return new ConditionValidationResponse(false, 
                    "🔍 기본 호환성 검사 실패: " + compatibilityResult.getReason());
            }

                         // 🔄 2단계: AI를 통한 고급 문법 및 보안 검증 (호환성 통과한 경우만)
             String contextInfo = String.format("""
                 리소스 정보:
                 - 식별자: %s
                 - 타입: %s
                 - 친숙한 이름: %s
                 - 반환 타입: %s
                 - 파라미터: %s
                 - 사용 가능한 변수: %s
                 """, 
                 resource.getResourceIdentifier(),
                 resource.getResourceType(),
                 resource.getFriendlyName(),
                 resource.getReturnType(),
                 resource.getParameterTypes(),
                 String.join(", ", compatibilityResult.getAvailableVariables()));

            String systemPrompt = """
                당신은 Spring SpEL 표현식 보안 및 품질 검증 전문가입니다. 
                기본 호환성 검사는 이미 통과했으므로, 다음 고급 검증을 수행해주세요:
                
                🔍 검증 항목:
                1. SpEL 문법의 정확성과 실행 가능성
                2. 보안상 위험한 패턴 감지 (예: 무제한 메서드 호출, 시스템 접근)
                3. 성능상 문제가 될 수 있는 구조 (예: 복잡한 반복문, 외부 호출)
                4. 논리적 모순이나 항상 true/false인 조건
                5. 권장 개선사항
                
                [매우 중요] 응답은 반드시 순수한 JSON 형식이어야 합니다.
                
                정확히 다음 형식으로만 응답하세요:
                {
                  "isCompatible": true/false,
                  "reason": "검증 결과에 대한 상세 설명",
                  "securityRisk": "LOW/MEDIUM/HIGH",
                  "performanceIssue": true/false,
                  "suggestions": "개선 제안사항 (선택적)"
                }
                """;

            String userPrompt = String.format("""
                다음 SpEL 표현식에 대해 고급 검증을 수행해주세요:
                
                **리소스 컨텍스트:**
                %s
                
                **검증할 SpEL 표현식:**
                %s
                
                위 표현식의 문법, 보안성, 성능, 논리성을 종합적으로 평가하고 순수 JSON으로만 응답해주세요.
                """, contextInfo, conditionSpel);

            SystemMessage systemMessage = new SystemMessage(systemPrompt);
            UserMessage userMessage = new UserMessage(userPrompt);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

            ChatResponse response = chatModel.call(prompt);
            String aiResponse = response.getResult().getOutput().getText();

            log.debug("🤖 AI 고급 검증 원본 응답: {}", aiResponse);

            // JSON 정제 및 파싱
            String cleanedJson = cleanJsonForValidation(aiResponse);
            log.debug("🤖 정제된 JSON: {}", cleanedJson);

            try {
                JsonNode jsonNode = objectMapper.readTree(cleanedJson);
                boolean aiCompatible = jsonNode.get("isCompatible").asBoolean();
                String aiReason = jsonNode.get("reason").asText();
                String securityRisk = jsonNode.has("securityRisk") ? jsonNode.get("securityRisk").asText() : "UNKNOWN";
                boolean performanceIssue = jsonNode.has("performanceIssue") ? jsonNode.get("performanceIssue").asBoolean() : false;
                String suggestions = jsonNode.has("suggestions") ? jsonNode.get("suggestions").asText() : "";

                // 🔄 3단계: 종합 결과 구성 (호환성 + AI 검증)
                String finalReason = String.format("""
                    ✅ 1단계 호환성: %s
                    🤖 2단계 AI 검증: %s
                    🛡️ 보안 위험도: %s
                    ⚡ 성능 이슈: %s%s
                    """, 
                    compatibilityResult.getReason(),
                    aiReason,
                    securityRisk,
                    performanceIssue ? "있음" : "없음",
                    suggestions.isEmpty() ? "" : "\n💡 개선 제안: " + suggestions);

                boolean finalResult = aiCompatible; // AI 검증 결과를 최종 결과로 사용
                
                log.info("✅ 3단계 조건 검증 완료: 최종결과={}, 상세={}", finalResult, finalReason.replace("\n", " | "));
                return new ConditionValidationResponse(finalResult, finalReason.trim());

            } catch (Exception parseException) {
                log.warn("⚠️ AI 응답 파싱 실패, 1단계 호환성 결과만 사용: {}", parseException.getMessage());
                
                // Fallback: 기본 호환성 검사 결과만 사용
                return new ConditionValidationResponse(true, 
                    "✅ 1단계 호환성 검사 통과: " + compatibilityResult.getReason() + 
                    " | ⚠️ 2단계 AI 고급 검증 실패");
            }

        } catch (Exception e) {
            log.error("🔥 조건 검증 중 오류 발생", e);
            return new ConditionValidationResponse(false, "검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * AI 응답에서 JSON만 추출하는 메서드
     */
    private String cleanJsonForValidation(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }

        String cleaned = response.trim();

        // 1. 마크다운 코드 블록 제거
        cleaned = cleaned.replaceAll("```json\\s*", "");
        cleaned = cleaned.replaceAll("```\\s*", "");

        // 2. JSON 객체만 추출 (첫 번째 { 부터 마지막 } 까지)
        int startIdx = cleaned.indexOf('{');
        int endIdx = cleaned.lastIndexOf('}');

        if (startIdx >= 0 && endIdx > startIdx) {
            cleaned = cleaned.substring(startIdx, endIdx + 1);
        }

        // 3. 잘못된 쉼표 제거
        cleaned = cleaned.replaceAll(",\\s*}", "}");

        // 4. 이스케이프 문자 정규화
        cleaned = cleaned.replace("\\n", " ");
        cleaned = cleaned.replace("\\r", "");
        cleaned = cleaned.replace("\\t", " ");

        return cleaned;
    }

    // AINativeIAMSynapseArbiterFromOllama.java에 추가/수정할 메서드들

    @Override
    public String generateUniversalConditionTemplates() {
        log.info("🤖 AI 범용 조건 템플릿 생성 시작");

        String systemPrompt = """
        당신은 ABAC 범용 조건 생성 전문가입니다.
        반드시 JSON 배열 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요.
        
        **필수 JSON 응답 형식:**
        [
          {
            "name": "사용자 인증 상태 확인",
            "description": "사용자 인증 상태를 확인하는 조건",
            "spelTemplate": "isAuthenticated()",
            "category": "인증 상태",
            "classification": "UNIVERSAL"
          }
        ]
        
        **생성할 범용 조건 (정확히 3개만):**
        1. isAuthenticated() - 사용자 인증 상태 확인
        2. hasRole('ROLE_ADMIN') - 관리자 역할 확인  
        3. 업무시간 접근 제한 (9시-18시)
        
        **주의사항:**
        - "~권한" 용어 사용 금지
        - "~확인", "~제한" 용어 사용
        - 정확히 3개만 생성
        
        JSON만 출력하세요. 설명 텍스트 금지.
        """;

        String userPrompt = """
        🎯 정확히 3개의 범용 조건만 생성하세요:
        
        1. 사용자 인증 상태 확인 - isAuthenticated()
        2. 관리자 역할 확인 - hasRole('ROLE_ADMIN')  
        3. 업무시간 접근 제한 - T(java.time.LocalTime).now().hour >= 9 && T(java.time.LocalTime).now().hour <= 18
        
        ❌ 절대 금지:
        - 4개 이상 생성
        - hasPermission() 사용 (범용 조건에서는 금지)
        - 존재하지 않는 파라미터 사용
        """;

        try {
            SystemMessage systemMessage = new SystemMessage(systemPrompt);
            UserMessage userMessage = new UserMessage(userPrompt);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

            ChatResponse response = chatModel.call(prompt);
            String aiResponse = response.getResult().getOutput().getText();

            log.debug("✅ AI 범용 템플릿 응답 수신: {} characters", aiResponse.length());

            // JSON 검증
            String trimmed = aiResponse.trim();
            if (!trimmed.startsWith("[")) {
                log.error("🔥 AI가 JSON 배열이 아닌 형식으로 응답: {}", trimmed.substring(0, Math.min(50, trimmed.length())));
                return getFallbackUniversalTemplates();
            }

            return aiResponse;

        } catch (Exception e) {
            log.error("🔥 AI 범용 템플릿 생성 실패", e);
            return getFallbackUniversalTemplates();
        }
    }

    private String getFallbackUniversalTemplates() {
        return """
        [
          {
            "name": "사용자 인증 상태 확인",
            "description": "사용자가 인증되었는지 확인하는 조건",
            "spelTemplate": "isAuthenticated()",
            "category": "인증 상태",
            "classification": "UNIVERSAL"
          },
          {
            "name": "관리자 역할 확인",
            "description": "관리자 역할을 가진 사용자인지 확인하는 조건",
            "spelTemplate": "hasRole('ROLE_ADMIN')",
            "category": "역할 확인",
            "classification": "UNIVERSAL"
          },
          {
            "name": "업무시간 접근 제한",
            "description": "오전 9시부터 오후 6시까지만 접근을 허용하는 조건",
            "spelTemplate": "T(java.time.LocalTime).now().hour >= 9 && T(java.time.LocalTime).now().hour <= 18",
            "category": "시간 기반",
            "classification": "UNIVERSAL"
          }
        ]
        """;
    }

    @Override
    public String generateSpecificConditionTemplates(String resourceIdentifier, String methodInfo) {
        log.debug("🤖 AI 특화 조건 생성: {}", resourceIdentifier);
        log.info("📝 전달받은 메서드 정보: {}", methodInfo);

        String systemPrompt = """
        🚨 극도로 제한된 ABAC 조건 생성기 🚨
        
        당신은 hasPermission() 전용 조건 생성기입니다.
        반드시 hasPermission(파라미터, 리소스타입, 액션) 형식만 사용하세요.
        
                 🔒 절대적 제약사항:
         1. hasPermission() 함수만 사용 (올바른 형식으로)
         2. 제공된 파라미터만 사용 (추가 파라미터 절대 금지)
         3. 정확히 하나의 조건만 생성 (여러 개 절대 금지)
         4. "~검증", "~확인" 용어만 사용 ("~권한" 절대 금지)
         5. 액션은 CREATE, READ, UPDATE, DELETE만 사용
        
                 🎯 허용된 형식:
         
         **ID 파라미터인 경우 (반드시 3개 파라미터):**
         - hasPermission(#id, 'GROUP', 'READ') - Long id 파라미터용
         - hasPermission(#id, 'GROUP', 'DELETE') - Long id 파라미터용  
         - hasPermission(#idx, 'USER', 'DELETE') - Long idx 파라미터용
         - hasPermission(#id, 'USER', 'READ') - Long id 파라미터용
         
         **객체 파라미터인 경우 (반드시 2개 파라미터):**
         - hasPermission(#group, 'CREATE') - Group 객체용 (절대 3개 파라미터 금지!)
         - hasPermission(#group, 'UPDATE') - Group 객체용 (절대 3개 파라미터 금지!)
         - hasPermission(#userDto, 'UPDATE') - UserDto 객체용 (절대 3개 파라미터 금지!)
         
         **실제 파라미터 예시:**
         - createGroup(Group group, List<Long> selectedRoleIds) → #group, #selectedRoleIds 사용
         - modifyUser(UserDto userDto) → #userDto 사용 (2개 파라미터 형식!)
         - getGroup(Long id) → #id 사용 (3개 파라미터 형식!)
         - deleteUser(Long idx) → #idx 사용 (3개 파라미터 형식!)
        
                 ❌ 절대 금지 (시스템 크래시 발생):
         - #document, #currentUser, #user, #rootScope (절대 존재하지 않음)
         - hasPermission(#userDto, 'USER', 'UPDATE') (UserDto는 객체이므로 2개 파라미터만!)
         - hasPermission(#group, 'GROUP', 'CREATE') (Group은 객체이므로 2개 파라미터만!)
         - hasPermission(#id, 'READ') (ID는 3개 파라미터 필수!)
         - DOCUMENT, ROLE, SYSTEM 리소스 타입 (존재하지 않음)
         - #groupExists(), getCurrentUser() (존재하지 않는 함수)
         - && || 연산자 (복합 조건 금지)
         - 여러 조건 생성
         - "권한" 용어 사용 ("검증", "확인"만 허용)
         
         🚨 특별 주의사항:
         - createGroup 메서드에서 #document 파라미터 사용 절대 금지!
         - modifyUser 메서드에서 hasPermission(#userDto, 'USER', 'UPDATE') 형식 절대 금지!
        
                 **응답 형식 (정확히 하나만):**
         [
           {
             "name": "그룹 수정 대상 검증",
             "description": "수정하려는 그룹에 대한 UPDATE 권한을 검증하는 조건",
             "spelTemplate": "hasPermission(#group, 'UPDATE')",
             "category": "권한 검증",
             "classification": "CONTEXT_DEPENDENT"
           }
         ]
         
         **ID 파라미터 예시:**
         [
           {
             "name": "그룹 조회 권한 검증",
             "description": "특정 ID의 그룹에 대한 READ 권한을 검증하는 조건",
             "spelTemplate": "hasPermission(#id, 'GROUP', 'READ')",
             "category": "권한 검증",
             "classification": "CONTEXT_DEPENDENT"
           }
         ]
        
        🚨 경고: 위 제약사항을 위반하면 시스템 오류가 발생합니다!
        """;

        try {
            SystemMessage systemMessage = new SystemMessage(systemPrompt);
            UserMessage userMessage = new UserMessage(methodInfo);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

            ChatResponse response = chatModel.call(prompt);
            String aiResponse = response.getResult().getOutput().getText();

            log.debug("✅ AI 특화 템플릿 응답 수신: {} characters", aiResponse.length());
            log.info("🔍 AI 응답 전체 내용: {}", aiResponse);

            return aiResponse;

        } catch (Exception e) {
            log.warn("🔥 AI 특화 템플릿 생성 실패: {}", resourceIdentifier, e);
            return "[]";
        }
    }

    /**
     * hasPermission 형식의 fallback 조건 생성 - 주석 처리
     */
    private String generateFallbackHasPermissionCondition(String resourceIdentifier, String methodInfo) {
        // fallback 주석 처리 - AI 응답 분석 필요
    /*
    // 메서드명에서 동작과 엔티티 추출
    String methodName = extractMethodNameFromResourceId(resourceIdentifier);

    // 엔티티 타입 추론
    String entityType = "RESOURCE"; // 기본값
    if (resourceIdentifier.contains("User")) entityType = "USER";
    else if (resourceIdentifier.contains("Group")) entityType = "GROUP";
    else if (resourceIdentifier.contains("Document")) entityType = "DOCUMENT";
    else if (resourceIdentifier.contains("Role")) entityType = "ROLE";
    else if (resourceIdentifier.contains("Permission")) entityType = "PERMISSION";
    else if (resourceIdentifier.contains("Policy")) entityType = "POLICY";

    // 파라미터 타입 확인
    boolean hasIdParam = methodInfo.contains("Long id") || methodInfo.contains("Long") || methodInfo.contains("userId");
    boolean hasObjectParam = methodInfo.contains(entityType.toLowerCase());

    // CREATE 패턴
    if (methodName.contains("create") || methodName.contains("add")) {
        return String.format("""
            [
              {
                "name": "%s 생성 조건",
                "description": "%s를 생성할 수 있는 권한을 확인하는 조건",
                "spelTemplate": "hasPermission(#%s, 'CREATE')",
                "category": "권한 확인",
                "classification": "CONTEXT_DEPENDENT"
              }
            ]
            """, entityType.toLowerCase(), entityType.toLowerCase(),
            hasObjectParam ? entityType.toLowerCase() : "object");
    }

    // READ/GET 패턴
    else if (methodName.contains("get") || methodName.contains("find") || methodName.contains("read")) {
        if (hasIdParam) {
            return String.format("""
                [
                  {
                    "name": "%s 조회 조건",
                    "description": "%s를 조회할 수 있는 권한을 확인하는 조건",
                    "spelTemplate": "hasPermission(#id, '%s', 'READ')",
                    "category": "권한 확인",
                    "classification": "CONTEXT_DEPENDENT"
                  }
                ]
                """, entityType.toLowerCase(), entityType.toLowerCase(), entityType);
        }
    }

    // UPDATE 패턴
    else if (methodName.contains("update") || methodName.contains("modify") || methodName.contains("edit")) {
        if (hasObjectParam) {
            return String.format("""
                [
                  {
                    "name": "%s 수정 조건",
                    "description": "%s를 수정할 수 있는 권한을 확인하는 조건",
                    "spelTemplate": "hasPermission(#%s, 'UPDATE')",
                    "category": "권한 확인",
                    "classification": "CONTEXT_DEPENDENT"
                  }
                ]
                """, entityType.toLowerCase(), entityType.toLowerCase(), entityType.toLowerCase());
        } else if (hasIdParam) {
            return String.format("""
                [
                  {
                    "name": "%s 수정 조건",
                    "description": "%s를 수정할 수 있는 권한을 확인하는 조건",
                    "spelTemplate": "hasPermission(#id, '%s', 'UPDATE')",
                    "category": "권한 확인",
                    "classification": "CONTEXT_DEPENDENT"
                  }
                ]
                """, entityType.toLowerCase(), entityType.toLowerCase(), entityType);
        }
    }

    // DELETE 패턴
    else if (methodName.contains("delete") || methodName.contains("remove")) {
        if (hasIdParam) {
            return String.format("""
                [
                  {
                    "name": "%s 삭제 조건",
                    "description": "%s를 삭제할 수 있는 권한을 확인하는 조건",
                    "spelTemplate": "hasPermission(#id, '%s', 'DELETE')",
                    "category": "권한 확인",
                    "classification": "CONTEXT_DEPENDENT"
                  }
                ]
                """, entityType.toLowerCase(), entityType.toLowerCase(), entityType);
        }
    }

    // 기본값: 빈 배열 (파라미터가 없거나 패턴이 맞지 않는 경우)
    return "[]";
    */

        return "[]"; // 빈 배열 반환
    }
}
