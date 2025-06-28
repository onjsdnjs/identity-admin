package io.spring.iam.aiam.protocol.request;

import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.enums.AuditRequirement;
import io.spring.iam.aiam.protocol.enums.DiagnosisType;
import io.spring.iam.aiam.protocol.enums.SecurityLevel;
import io.spring.iam.aiam.protocol.types.ConditionTemplateContext;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 조건 템플릿 생성 요청 DTO
 * 
 * ✅ 타입 안전성: 구체적인 요청 타입
 * 🎯 범용/특화 조건 템플릿 생성 지원
 */
@Getter
@Builder
public class ConditionTemplateGenerationRequest extends IAMRequest<ConditionTemplateContext> {
    
    private final String templateType; // "universal" 또는 "specific"
    private final String resourceIdentifier; // 특화 조건용 (선택적)
    private final String methodInfo; // 특화 조건용 (선택적)
    private final Map<String, Object> additionalParameters; // 추가 파라미터
    
    @Builder
    private ConditionTemplateGenerationRequest(String templateType, String resourceIdentifier, 
                                              String methodInfo, Map<String, Object> additionalParameters) {
        super(createContext(templateType, resourceIdentifier, methodInfo), "conditionTemplateGeneration");
        
        this.templateType = templateType;
        this.resourceIdentifier = resourceIdentifier;
        this.methodInfo = methodInfo;
        this.additionalParameters = additionalParameters != null ? additionalParameters : Map.of();
        
        // 진단 타입 설정
        this.withDiagnosisType(DiagnosisType.CONDITION_TEMPLATE);
        
        // 컨텍스트에 추가 파라미터 설정
        if (additionalParameters != null) {
            additionalParameters.forEach(this.getContext()::putTemplateMetadata);
        }
    }
    
    /**
     * 범용 조건 템플릿 생성 요청
     */
    public static ConditionTemplateGenerationRequest forUniversalTemplate() {
        return builder()
                .templateType("universal")
                .build();
    }
    
    /**
     * 특화 조건 템플릿 생성 요청
     */
    public static ConditionTemplateGenerationRequest forSpecificTemplate(String resourceIdentifier, String methodInfo) {
        return builder()
                .templateType("specific")
                .resourceIdentifier(resourceIdentifier)
                .methodInfo(methodInfo)
                .build();
    }
    
    /**
     * 컨텍스트 생성 헬퍼 메서드
     */
    private static ConditionTemplateContext createContext(String templateType, String resourceIdentifier, String methodInfo) {
        if ("universal".equals(templateType)) {
            return ConditionTemplateContext.forUniversalTemplate();
        } else if ("specific".equals(templateType)) {
            return ConditionTemplateContext.forSpecificTemplate(resourceIdentifier, methodInfo);
        } else {
            throw new IllegalArgumentException("지원하지 않는 템플릿 타입: " + templateType);
        }
    }
    
    @Override
    public String toString() {
        return String.format("ConditionTemplateGenerationRequest{type='%s', resource='%s', requestId='%s'}", 
                templateType, resourceIdentifier, getRequestId());
    }
} 