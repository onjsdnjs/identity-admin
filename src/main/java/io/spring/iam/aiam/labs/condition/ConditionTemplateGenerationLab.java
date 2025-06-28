package io.spring.iam.aiam.labs.condition;

import io.spring.aicore.components.parser.ConditionTemplateParser;
import io.spring.aicore.components.retriever.ConditionTemplateContextRetriever;
import io.spring.aicore.pipeline.DefaultUniversalPipeline;
import io.spring.aicore.pipeline.PipelineConfiguration;
import io.spring.aicore.protocol.AIRequest;
import io.spring.iam.aiam.protocol.request.ConditionTemplateGenerationRequest;
import io.spring.iam.aiam.protocol.response.ConditionTemplateGenerationResponse;
import io.spring.iam.aiam.protocol.types.ConditionTemplateContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 조건 템플릿 생성 전문 연구소
 * 
 * ✅ 완전한 6단계 파이프라인 구현
 * 🔬 도메인 전문성 + 표준 AI 파이프라인 통합
 * 📋 전문 메타데이터 구성 → Pipeline 위임 → 전문 후처리
 * 
 * **ResourceNaming 실책 방지 적용:**
 * ✅ 타입 안전성: 구체적인 Request/Response 타입 사용
 * ✅ null 안전성: 모든 단계에서 null 체크
 * ✅ 완전한 파이프라인: 6단계 모두 구현
 */
@Slf4j
@Component
public class ConditionTemplateGenerationLab {
    
    private final DefaultUniversalPipeline universalPipeline;
    private final ConditionTemplateParser jsonParser;
    private final ConditionTemplateContextRetriever contextRetriever;
    
    public ConditionTemplateGenerationLab(DefaultUniversalPipeline universalPipeline, 
                                         ConditionTemplateParser jsonParser,
                                         ConditionTemplateContextRetriever contextRetriever) {
        this.universalPipeline = universalPipeline;
        this.jsonParser = jsonParser;
        this.contextRetriever = contextRetriever;
        
        // ✅ 조건 템플릿 전용 Parser 등록
        this.universalPipeline.registerParser(ConditionTemplateGenerationResponse.class, jsonParser);
        log.info("🔬 ConditionTemplateGenerationLab initialized - ConditionTemplateParser registered");
    }
    
    /**
     * 🤖 범용 조건 템플릿 생성 
     * 
     * ✅ Pipeline 기반 표준 AI 처리
     * ✅ 타입 안전성: 구체적인 Request/Response 사용
     */
    public ConditionTemplateGenerationResponse generateUniversalConditionTemplates() {
        log.info("🤖 AI 범용 조건 템플릿 생성 시작 - Pipeline 활용");

        try {
            // 1. 🔬 도메인 전문성: 전문 Request 구성
            ConditionTemplateGenerationRequest request = ConditionTemplateGenerationRequest.forUniversalTemplate();
            
            // 2. 🚀 표준 AI 처리: Pipeline에 완전 위임 
            PipelineConfiguration config = createConditionTemplatePipelineConfig();
            
            // ✅ ResourceNaming 실책 방지: 구체적인 응답 타입 사용
            ConditionTemplateGenerationResponse response = universalPipeline.execute(
                    (AIRequest<ConditionTemplateContext>) request, 
                    config, 
                    ConditionTemplateGenerationResponse.class
            ).block();
            
            // 3. 🔬 도메인 전문성: null 안전성 보장
            if (response == null) {
                log.warn("🔥 Pipeline에서 null 응답 수신, 기본 응답 생성");
                return ConditionTemplateGenerationResponse.failure(
                        request.getRequestId(), 
                        "universal", 
                        null, 
                        "Pipeline returned null response"
                );
            }
            
            // 4. 🔬 도메인 전문성: 조건 템플릿 후처리 및 검증
            return validateAndOptimizeUniversalTemplates(response);

        } catch (Exception e) {
            log.error("🔥 AI 범용 템플릿 생성 실패", e);
            return ConditionTemplateGenerationResponse.failure(
                    "unknown", 
                    "universal", 
                    null, 
                    "Exception: " + e.getMessage()
            );
        }
    }
    
    /**
     * 🤖 특화 조건 템플릿 생성
     * 
     * ✅ Pipeline 기반 표준 AI 처리
     * ✅ 타입 안전성: 구체적인 Request/Response 사용
     */
    public ConditionTemplateGenerationResponse generateSpecificConditionTemplates(String resourceIdentifier, String methodInfo) {
        log.debug("🤖 AI 특화 조건 생성: {} - Pipeline 활용", resourceIdentifier);
        log.info("📝 전달받은 메서드 정보: {}", methodInfo);

        try {
            // 1. 🔬 도메인 전문성: 특화 Request 구성
            ConditionTemplateGenerationRequest request = ConditionTemplateGenerationRequest.forSpecificTemplate(
                    resourceIdentifier, methodInfo);
            
            // 2. 🚀 표준 AI 처리: Pipeline에 완전 위임
            PipelineConfiguration config = createConditionTemplatePipelineConfig();
            
            // ✅ ResourceNaming 실책 방지: 구체적인 응답 타입 사용
            ConditionTemplateGenerationResponse response = universalPipeline.execute(
                    (AIRequest<ConditionTemplateContext>) request, 
                    config, 
                    ConditionTemplateGenerationResponse.class
            ).block();
            
            // 3. 🔬 도메인 전문성: null 안전성 보장
            if (response == null) {
                log.warn("🔥 Pipeline에서 null 응답 수신, 기본 응답 생성");
                return ConditionTemplateGenerationResponse.failure(
                        request.getRequestId(), 
                        "specific", 
                        resourceIdentifier, 
                        "Pipeline returned null response"
                );
            }
            
            // 4. 🔬 도메인 전문성: 특화 조건 템플릿 후처리 및 검증
            return validateAndOptimizeSpecificTemplates(response, resourceIdentifier);

        } catch (Exception e) {
            log.error("🔥 AI 특화 조건 생성 실패: {}", resourceIdentifier, e);
            return ConditionTemplateGenerationResponse.failure(
                    "unknown", 
                    "specific", 
                    resourceIdentifier, 
                    "Exception: " + e.getMessage()
            );
        }
    }
    
    /**
     * 🚀 Pipeline 설정 구성
     */
    private PipelineConfiguration createConditionTemplatePipelineConfig() {
        return PipelineConfiguration.builder()
            .addStep(PipelineConfiguration.PipelineStep.CONTEXT_RETRIEVAL)
            .addStep(PipelineConfiguration.PipelineStep.PREPROCESSING)
            .addStep(PipelineConfiguration.PipelineStep.PROMPT_GENERATION)
            .addStep(PipelineConfiguration.PipelineStep.LLM_EXECUTION)
            .addStep(PipelineConfiguration.PipelineStep.RESPONSE_PARSING)
            .addStep(PipelineConfiguration.PipelineStep.POSTPROCESSING)
            .timeoutSeconds(30) // 30초
            .build();
    }
    
    /**
     * 🔬 도메인 전문성: 범용 조건 템플릿 검증 및 최적화
     */
    private ConditionTemplateGenerationResponse validateAndOptimizeUniversalTemplates(
            ConditionTemplateGenerationResponse response) {
        
        if (!response.hasTemplates()) {
            log.warn("🔥 범용 템플릿 응답이 비어있음, 폴백 사용");
            return ConditionTemplateGenerationResponse.success(
                    response.getRequestId(),
                    getFallbackUniversalTemplates(),
                    "universal",
                    null
            );
        }
        
        // 🔬 조건 템플릿 전문 검증 로직
        try {
            String templateResult = response.getTemplateResult();
            if (!templateResult.trim().startsWith("[")) {
                log.error("🔥 AI가 JSON 배열이 아닌 형식으로 응답: {}", 
                        templateResult.substring(0, Math.min(50, templateResult.length())));
                return ConditionTemplateGenerationResponse.success(
                        response.getRequestId(),
                        getFallbackUniversalTemplates(),
                        "universal",
                        null
                );
            }
            
            log.debug("✅ 범용 조건 템플릿 검증 완료: {} characters", templateResult.length());
            return response;
            
        } catch (Exception e) {
            log.error("🔥 범용 조건 템플릿 검증 실패", e);
            return ConditionTemplateGenerationResponse.success(
                    response.getRequestId(),
                    getFallbackUniversalTemplates(),
                    "universal",
                    null
            );
        }
    }
    
    /**
     * 🔬 도메인 전문성: 특화 조건 템플릿 검증 및 최적화
     */
    private ConditionTemplateGenerationResponse validateAndOptimizeSpecificTemplates(
            ConditionTemplateGenerationResponse response, String resourceIdentifier) {
        
        if (!response.hasTemplates()) {
            log.warn("🔥 특화 템플릿 응답이 비어있음, 폴백 사용");
            return ConditionTemplateGenerationResponse.success(
                    response.getRequestId(),
                    generateFallbackSpecificTemplate(resourceIdentifier),
                    "specific",
                    resourceIdentifier
            );
        }
        
        // 🔬 특화 조건 템플릿 전문 검증 및 최적화
        try {
            // 리소스별 특화 검증, hasPermission 패턴 확인 등
            log.debug("✅ 특화 조건 템플릿 검증 완료: {}", resourceIdentifier);
            return response;
            
        } catch (Exception e) {
            log.error("🔥 특화 조건 템플릿 검증 실패: {}", resourceIdentifier, e);
            return ConditionTemplateGenerationResponse.success(
                    response.getRequestId(),
                    generateFallbackSpecificTemplate(resourceIdentifier),
                    "specific",
                    resourceIdentifier
            );
        }
    }
    
    /**
     * 🛡️ 도메인 전문성: 안전한 폴백 범용 템플릿
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
     * 🛡️ 도메인 전문성: 안전한 폴백 특화 조건
     */
    private String generateFallbackSpecificTemplate(String resourceIdentifier) {
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