package io.spring.iam.aiam.labs.condition;

import io.spring.aicore.components.parser.JsonResponseParser;
import io.spring.aicore.components.prompt.PromptGenerator;
import io.spring.aicore.components.prompt.PromptGenerator.PromptGenerationResult;
import io.spring.aicore.components.retriever.ContextRetriever;
import io.spring.aicore.protocol.AIRequest;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.enums.AuditRequirement;
import io.spring.iam.aiam.protocol.enums.SecurityLevel;
import io.spring.iam.aiam.protocol.types.PolicyContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Component;

/**
 * 조건 템플릿 생성 전문 연구소
 * 
 * ✅ aicore components 완전 활용
 * 🔬 PromptGenerator로 동적 프롬프트 생성
 * 🧹 JsonResponseParser로 응답 정제
 * 📋 ContextRetriever로 컨텍스트 검색
 */
@Slf4j
@Component
public class ConditionTemplateGenerationLab {
    
    private final OllamaChatModel chatModel;
    private final PromptGenerator promptGenerator;
    private final JsonResponseParser jsonResponseParser;
    private final ContextRetriever contextRetriever;
    
    public ConditionTemplateGenerationLab(OllamaChatModel chatModel,
                                        PromptGenerator promptGenerator,
                                        JsonResponseParser jsonResponseParser,
                                        ContextRetriever contextRetriever) {
        this.chatModel = chatModel;
        this.promptGenerator = promptGenerator;
        this.jsonResponseParser = jsonResponseParser;
        this.contextRetriever = contextRetriever;
        log.info("🔬 ConditionTemplateGenerationLab initialized - aicore components integrated");
    }
    
    /**
     * 🤖 범용 조건 템플릿 생성 
     * 
     * ✅ PromptGenerator 활용하여 동적 프롬프트 생성
     */
    public String generateUniversalConditionTemplates() {
        log.info("🤖 AI 범용 조건 템플릿 생성 시작 - aicore components 활용");

        try {
            // ✅ AIRequest 생성하여 PromptGenerator에 전달
            AIRequest<IAMContext> aiRequest = createUniversalConditionRequest();
            
            // ✅ ContextRetriever로 관련 컨텍스트 검색
            ContextRetriever.ContextRetrievalResult contextResult = contextRetriever.retrieveContext(aiRequest);
            String contextInfo = contextResult.getContextInfo();
            
            // ✅ 시스템 메타데이터 구성
            String systemMetadata = buildSystemMetadata();
            
            // ✅ PromptGenerator로 동적 프롬프트 생성
            PromptGenerationResult promptResult = promptGenerator.generatePrompt(
                aiRequest, contextInfo, systemMetadata
            );
            
            log.debug("✅ 동적 프롬프트 생성 완료: system={}, user={}", 
                promptResult.getSystemPrompt().length(), 
                promptResult.getUserPrompt().length());

            // ✅ AI 모델 호출
            ChatResponse response = chatModel.call(promptResult.getPrompt());
            String aiResponse = response.getResult().getOutput().getText();

            log.debug("✅ AI 범용 템플릿 응답 수신: {} characters", aiResponse.length());

            // ✅ JsonResponseParser 활용하여 JSON 정제
            String cleanedJson = jsonResponseParser.extractAndCleanJson(aiResponse);
            
            // ✅ 기존과 동일한 검증 로직
            String trimmed = cleanedJson.trim();
            if (!trimmed.startsWith("[")) {
                log.error("🔥 AI가 JSON 배열이 아닌 형식으로 응답: {}", trimmed.substring(0, Math.min(50, trimmed.length())));
                return getFallbackUniversalTemplates();
            }

            return cleanedJson;

        } catch (Exception e) {
            log.error("🔥 AI 범용 템플릿 생성 실패", e);
            return getFallbackUniversalTemplates();
        }
    }
    
    /**
     * 🤖 특화 조건 템플릿 생성
     * 
     * ✅ PromptGenerator 활용하여 동적 프롬프트 생성
     */
    public String generateSpecificConditionTemplates(String resourceIdentifier, String methodInfo) {
        log.debug("🤖 AI 특화 조건 생성: {} - aicore components 활용", resourceIdentifier);
        log.info("📝 전달받은 메서드 정보: {}", methodInfo);

        try {
            // ✅ AIRequest 생성하여 PromptGenerator에 전달
            AIRequest<IAMContext> aiRequest = createSpecificConditionRequest(resourceIdentifier, methodInfo);
            
            // ✅ ContextRetriever로 관련 컨텍스트 검색
            ContextRetriever.ContextRetrievalResult contextResult = contextRetriever.retrieveContext(aiRequest);
            String contextInfo = contextResult.getContextInfo();
            
            // ✅ 시스템 메타데이터 구성
            String systemMetadata = buildSystemMetadata();
            
            // ✅ PromptGenerator로 동적 프롬프트 생성
            PromptGenerationResult promptResult = promptGenerator.generatePrompt(
                aiRequest, contextInfo, systemMetadata
            );
            
            log.debug("✅ 특화 조건 동적 프롬프트 생성 완료");

            // ✅ AI 모델 호출
            ChatResponse response = chatModel.call(promptResult.getPrompt());
            String aiResponse = response.getResult().getOutput().getText();

            log.debug("✅ AI 특화 템플릿 응답 수신: {} characters", aiResponse.length());
            log.info("🔍 AI 응답 전체 내용: {}", aiResponse);

            // ✅ JsonResponseParser 활용하여 JSON 정제
            return jsonResponseParser.extractAndCleanJson(aiResponse);

        } catch (Exception e) {
            log.error("🔥 AI 특화 조건 생성 실패: {}", resourceIdentifier, e);
            return generateFallbackHasPermissionCondition(resourceIdentifier, methodInfo);
        }
    }
    
    /**
     * ✅ 범용 조건 요청 생성
     */
    private AIRequest<IAMContext> createUniversalConditionRequest() {
        IAMContext context = new PolicyContext(SecurityLevel.STANDARD, AuditRequirement.BASIC);
        
        return new AIRequest<>(context, "universal_condition_template");
    }
    
    /**
     * ✅ 특화 조건 요청 생성
     */
    private AIRequest<IAMContext> createSpecificConditionRequest(String resourceIdentifier, String methodInfo) {
        IAMContext context = new PolicyContext(SecurityLevel.STANDARD, AuditRequirement.BASIC);
        
        AIRequest<IAMContext> request = new AIRequest<>(context, "specific_condition_template");
        // withParameter 메서드 사용
        request.withParameter("methodInfo", methodInfo);
        request.withParameter("resourceIdentifier", resourceIdentifier);
        return request;
    }
    
    /**
     * ✅ 시스템 메타데이터 구성
     */
    private String buildSystemMetadata() {
        return String.format("""
            시스템 정보:
            - 조건 템플릿 생성 전문 연구소
            - ABAC 기반 동적 권한 제어
            - 생성 시간: %s
            - 노드 ID: %s
            """, 
            java.time.LocalDateTime.now(),
            System.getProperty("node.id", "default-node")
        );
    }
    
    /**
     * 폴백 범용 템플릿 (기존과 100% 동일)
     */
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
            "category": "시간 제한",
            "classification": "UNIVERSAL"
          }
        ]
        """;
    }
    
    /**
     * 폴백 특화 조건 생성
     */
    private String generateFallbackHasPermissionCondition(String resourceIdentifier, String methodInfo) {
        return String.format("""
        [
          {
            "name": "%s 대상 검증",
            "description": "%s 리소스에 대한 접근 검증 조건",
            "spelTemplate": "hasPermission(#param, '%s', 'READ')",
            "category": "리소스 접근",
            "classification": "SPECIFIC"
          }
        ]
        """, resourceIdentifier, resourceIdentifier, resourceIdentifier);
    }
}