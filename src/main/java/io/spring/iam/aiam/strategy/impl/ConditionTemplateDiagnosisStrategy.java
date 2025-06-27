package io.spring.iam.aiam.strategy.impl;

import io.spring.iam.aiam.labs.condition.ConditionTemplateGenerationLab;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.iam.aiam.protocol.enums.DiagnosisType;
import io.spring.iam.aiam.strategy.DiagnosisException;
import io.spring.iam.aiam.strategy.DiagnosisStrategy;
import io.spring.aicore.protocol.AIResponse.ExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 🔬 조건 템플릿 생성 진단 전략
 * 
 * AINativeIAMSynapseArbiterFromOllama의 조건 템플릿 생성 기능을 완전히 대체
 * - generateUniversalConditionTemplates
 * - generateSpecificConditionTemplates
 * 
 * 🎯 역할:
 * 1. 요청 데이터 검증 및 전처리
 * 2. ConditionTemplateGenerationLab에 작업 위임
 * 3. 결과 후처리 및 응답 생성
 */
@Slf4j
@Component
public class ConditionTemplateDiagnosisStrategy implements DiagnosisStrategy<IAMContext, IAMResponse> {
    
    private final ConditionTemplateGenerationLab conditionTemplateGenerationLab;
    
    public ConditionTemplateDiagnosisStrategy(ConditionTemplateGenerationLab conditionTemplateGenerationLab) {
        this.conditionTemplateGenerationLab = conditionTemplateGenerationLab;
        log.info("🔬 ConditionTemplateDiagnosisStrategy initialized");
    }
    
    @Override
    public DiagnosisType getSupportedType() {
        return DiagnosisType.CONDITION_TEMPLATE;
    }
    
    @Override
    public int getPriority() {
        return 10; // 높은 우선순위
    }
    
    @Override
    public IAMResponse execute(IAMRequest<IAMContext> request, Class<IAMResponse> responseType) throws DiagnosisException {
        log.info("🔬 조건 템플릿 진단 전략 실행 시작 - 요청: {}", request.getRequestId());
        
        try {
            // 1. 요청 데이터 검증
            validateRequest(request);
            
            // 2. 요청 타입에 따른 분기 처리
            String templateType = request.getParameter("templateType", String.class);
            String result;
            
            if ("universal".equals(templateType)) {
                // 범용 조건 템플릿 생성
                log.debug("🌐 범용 조건 템플릿 생성 요청");
                result = conditionTemplateGenerationLab.generateUniversalConditionTemplates();
                
            } else if ("specific".equals(templateType)) {
                // 특화 조건 템플릿 생성
                String resourceIdentifier = request.getParameter("resourceIdentifier", String.class);
                String methodInfo = request.getParameter("methodInfo", String.class);
                
                log.debug("🎯 특화 조건 템플릿 생성 요청 - 리소스: {}", resourceIdentifier);
                result = conditionTemplateGenerationLab.generateSpecificConditionTemplates(resourceIdentifier, methodInfo);
                
            } else {
                throw new DiagnosisException("CONDITION_TEMPLATE", "INVALID_TEMPLATE_TYPE", 
                    "지원하지 않는 템플릿 타입입니다: " + templateType);
            }
            
            // 3. 응답 생성
            ConditionTemplateResponse response = new ConditionTemplateResponse(
                request.getRequestId(),
                ExecutionStatus.SUCCESS,
                result,
                templateType
            );
            
            log.info("✅ 조건 템플릿 진단 전략 실행 완료 - 요청: {}", request.getRequestId());
            return response;
            
        } catch (Exception e) {
            log.error("🔥 조건 템플릿 진단 전략 실행 실패 - 요청: {}", request.getRequestId(), e);
            throw new DiagnosisException("CONDITION_TEMPLATE", "EXECUTION_FAILED", 
                "조건 템플릿 생성 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 요청 데이터 검증
     */
    private void validateRequest(IAMRequest<IAMContext> request) throws DiagnosisException {
        if (request.getParameter("templateType", String.class) == null) {
            throw new DiagnosisException("CONDITION_TEMPLATE", "MISSING_TEMPLATE_TYPE", 
                "templateType 파라미터가 필요합니다");
        }
        
        String templateType = request.getParameter("templateType", String.class);
        if ("specific".equals(templateType)) {
            if (request.getParameter("resourceIdentifier", String.class) == null) {
                throw new DiagnosisException("CONDITION_TEMPLATE", "MISSING_RESOURCE_IDENTIFIER", 
                    "특화 템플릿 생성 시 resourceIdentifier 파라미터가 필요합니다");
            }
            if (request.getParameter("methodInfo", String.class) == null) {
                throw new DiagnosisException("CONDITION_TEMPLATE", "MISSING_METHOD_INFO", 
                    "특화 템플릿 생성 시 methodInfo 파라미터가 필요합니다");
            }
        }
    }
    
    /**
     * 조건 템플릿 응답 클래스
     */
    public static class ConditionTemplateResponse extends IAMResponse {
        private final String templateJson;
        private final String templateType;
        
        public ConditionTemplateResponse(String requestId, ExecutionStatus status, String templateJson, String templateType) {
            super(requestId, status);
            this.templateJson = templateJson;
            this.templateType = templateType;
        }
        
        @Override
        public String getResponseType() { 
            return "CONDITION_TEMPLATE"; 
        }
        
        @Override
        public Object getData() {
            // 템플릿 JSON과 타입을 포함한 데이터 맵 반환
            return Map.of(
                "templateJson", templateJson != null ? templateJson : "",
                "templateType", templateType != null ? templateType : "",
                "timestamp", getTimestamp(),
                "requestId", getRequestId()
            );
        }
        
        public String getTemplateJson() { return templateJson; }
        
        public String getTemplateType() { return templateType; }
        
        @Override
        public String toString() {
            return String.format("ConditionTemplateResponse{requestId='%s', status='%s', templateType='%s'}", 
                getResponseId(), getStatus(), templateType);
        }
    }
} 