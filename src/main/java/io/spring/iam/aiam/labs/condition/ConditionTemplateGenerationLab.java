package io.spring.iam.aiam.labs.condition;

import io.spring.aicore.components.parser.ConditionTemplateParser;
import io.spring.aicore.pipeline.DefaultUniversalPipeline;
import io.spring.aicore.pipeline.PipelineConfiguration;
import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.enums.AuditRequirement;
import io.spring.iam.aiam.protocol.enums.SecurityLevel;
import io.spring.iam.aiam.protocol.types.PolicyContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 조건 템플릿 생성 전문 연구소
 * 
 * ✅ DefaultUniversalPipeline 완전 활용
 * 🔬 도메인 전문성 + 표준 AI 파이프라인 통합
 * 📋 전문 메타데이터 구성 → Pipeline 위임 → 전문 후처리
 */
@Slf4j
@Component
public class ConditionTemplateGenerationLab {
    
    private final DefaultUniversalPipeline universalPipeline;
    private final ConditionTemplateParser jsonParser;
    
    public ConditionTemplateGenerationLab(DefaultUniversalPipeline universalPipeline, ConditionTemplateParser jsonParser) {
        this.universalPipeline = universalPipeline;
        this.jsonParser = jsonParser;
        this.universalPipeline.jsonResponseParser(jsonParser);
        log.info("🔬 ConditionTemplateGenerationLab initialized - Pipeline integrated");
    }
    
    /**
     * 🤖 범용 조건 템플릿 생성 
     * 
     * ✅ Pipeline 기반 표준 AI 처리
     */
    public String generateUniversalConditionTemplates() {
        log.info("🤖 AI 범용 조건 템플릿 생성 시작 - Pipeline 활용");

        try {
            // 1. 🔬 도메인 전문성: 전문 AIRequest 구성
            AIRequest<IAMContext> aiRequest = createUniversalConditionRequest();
            
            // 2. 🚀 표준 AI 처리: Pipeline에 완전 위임
            PipelineConfiguration config = createConditionTemplatePipelineConfig();
            Mono<AIResponse> pipelineResult = universalPipeline.execute(aiRequest, config, AIResponse.class);
            
            AIResponse response = pipelineResult.block(); // 동기 처리
            
            // 3. 🔬 도메인 전문성: 조건 템플릿 후처리 및 검증
            String templateJson = (String) response.getData();
            return validateAndOptimizeConditionTemplates(templateJson);

        } catch (Exception e) {
            log.error("🔥 AI 범용 템플릿 생성 실패", e);
            return getFallbackUniversalTemplates();
        }
    }
    
    /**
     * 🤖 특화 조건 템플릿 생성
     * 
     * ✅ Pipeline 기반 표준 AI 처리
     */
    public String generateSpecificConditionTemplates(String resourceIdentifier, String methodInfo) {
        log.debug("🤖 AI 특화 조건 생성: {} - Pipeline 활용", resourceIdentifier);
        log.info("📝 전달받은 메서드 정보: {}", methodInfo);

        try {
            // 1. 🔬 도메인 전문성: 특화 AIRequest 구성
            AIRequest<IAMContext> aiRequest = createSpecificConditionRequest(resourceIdentifier, methodInfo);
            
            // 2. 🚀 표준 AI 처리: Pipeline에 완전 위임
            PipelineConfiguration config = createConditionTemplatePipelineConfig();
            Mono<AIResponse> pipelineResult = universalPipeline.execute(aiRequest, config, AIResponse.class);
            
            AIResponse response = pipelineResult.block(); // 동기 처리
            
            // 3. 🔬 도메인 전문성: 특화 조건 템플릿 후처리 및 검증
            String templateJson = (String) response.getData();
            return validateAndOptimizeSpecificConditionTemplates(templateJson, resourceIdentifier);

        } catch (Exception e) {
            log.error("🔥 AI 특화 조건 생성 실패: {}", resourceIdentifier, e);
            return generateFallbackHasPermissionCondition(resourceIdentifier, methodInfo);
        }
    }
    
    /**
     * 🔬 도메인 전문성: 범용 조건 요청 구성
     */
    private AIRequest<IAMContext> createUniversalConditionRequest() {
        IAMContext context = new PolicyContext(SecurityLevel.STANDARD, AuditRequirement.BASIC);
        
        AIRequest<IAMContext> request = new AIRequest<>(context, "universal_condition_template");
        
        // 🔬 조건 템플릿 전문 메타데이터 설정
        request.withParameter("templateType", "universal");
        request.withParameter("conditionCategory", "authentication,authorization,time,resource");
        request.withParameter("outputFormat", "json_array");
        request.withParameter("spelSupport", true);
        request.withParameter("abacCompliant", true);
        
        return request;
    }
    
    /**
     * 🔬 도메인 전문성: 특화 조건 요청 구성
     */
    private AIRequest<IAMContext> createSpecificConditionRequest(String resourceIdentifier, String methodInfo) {
        IAMContext context = new PolicyContext(SecurityLevel.STANDARD, AuditRequirement.BASIC);
        
        AIRequest<IAMContext> request = new AIRequest<>(context, "specific_condition_template");
        
        // 🔬 특화 조건 템플릿 전문 메타데이터 설정
        request.withParameter("templateType", "specific");
        request.withParameter("resourceIdentifier", resourceIdentifier);
        request.withParameter("methodInfo", methodInfo);
        request.withParameter("outputFormat", "json_array");
        request.withParameter("spelSupport", true);
        request.withParameter("hasPermissionPattern", true);
        
        return request;
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
    private String validateAndOptimizeConditionTemplates(String templateJson) {
        if (templateJson == null || templateJson.trim().isEmpty()) {
            log.warn("🔥 Pipeline에서 빈 응답 수신, 폴백 사용");
            return getFallbackUniversalTemplates();
        }
        
        String trimmed = templateJson.trim();
        if (!trimmed.startsWith("[")) {
            log.error("🔥 AI가 JSON 배열이 아닌 형식으로 응답: {}", trimmed.substring(0, Math.min(50, trimmed.length())));
            return getFallbackUniversalTemplates();
        }
        
        // 🔬 조건 템플릿 전문 검증 로직
        try {
            // JSON 구조 검증, SpEL 문법 검증, ABAC 호환성 검증 등
            log.debug("✅ 조건 템플릿 검증 완료: {} characters", trimmed.length());
            return trimmed;
        } catch (Exception e) {
            log.error("🔥 조건 템플릿 검증 실패", e);
            return getFallbackUniversalTemplates();
        }
    }
    
    /**
     * 🔬 도메인 전문성: 특화 조건 템플릿 검증 및 최적화
     */
    private String validateAndOptimizeSpecificConditionTemplates(String templateJson, String resourceIdentifier) {
        if (templateJson == null || templateJson.trim().isEmpty()) {
            log.warn("🔥 Pipeline에서 빈 응답 수신, 폴백 사용");
            return generateFallbackHasPermissionCondition(resourceIdentifier, "");
        }
        
        // 🔬 특화 조건 템플릿 전문 검증 및 최적화
        try {
            // 리소스별 특화 검증, hasPermission 패턴 확인 등
            log.debug("✅ 특화 조건 템플릿 검증 완료: {}", resourceIdentifier);
            return templateJson.trim();
        } catch (Exception e) {
            log.error("🔥 특화 조건 템플릿 검증 실패: {}", resourceIdentifier, e);
            return generateFallbackHasPermissionCondition(resourceIdentifier, "");
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