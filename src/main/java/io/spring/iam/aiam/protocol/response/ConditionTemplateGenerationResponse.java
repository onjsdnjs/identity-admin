package io.spring.iam.aiam.protocol.response;

import io.spring.aicore.protocol.AIResponse;
import io.spring.iam.aiam.protocol.IAMResponse;
import lombok.Getter;

import java.util.Map;

/**
 * 조건 템플릿 생성 응답 DTO
 * 
 * ✅ 타입 안전성: 구체적인 응답 타입 (AIResponse 상속)
 * 🎯 조건 템플릿 결과 및 메타데이터 포함
 */
@Getter
public class ConditionTemplateGenerationResponse extends IAMResponse {
    
    private final String templateResult; // JSON 형태의 템플릿 결과
    private final String templateType; // "universal" 또는 "specific"
    private final String resourceIdentifier; // 특화 조건용 (선택적)
    private final Map<String, Object> processingMetadata; // 처리 메타데이터
    
    public ConditionTemplateGenerationResponse(String requestId, AIResponse.ExecutionStatus status,
                                             String templateResult, String templateType,
                                             String resourceIdentifier, Map<String, Object> processingMetadata) {
        super(requestId, status);
        this.templateResult = templateResult;
        this.templateType = templateType;
        this.resourceIdentifier = resourceIdentifier;
        this.processingMetadata = processingMetadata != null ? processingMetadata : Map.of();
    }
    
    /**
     * 성공 응답 생성
     */
    public static ConditionTemplateGenerationResponse success(String requestId, String templateResult, 
                                                            String templateType, String resourceIdentifier) {
        return new ConditionTemplateGenerationResponse(
                requestId, 
                AIResponse.ExecutionStatus.SUCCESS, 
                templateResult, 
                templateType,
                resourceIdentifier,
                Map.of("generatedAt", System.currentTimeMillis())
        );
    }
    
    /**
     * 실패 응답 생성
     */
    public static ConditionTemplateGenerationResponse failure(String requestId, String templateType, 
                                                            String resourceIdentifier, String errorMessage) {
        return new ConditionTemplateGenerationResponse(
                requestId, 
                AIResponse.ExecutionStatus.FAILURE, 
                "[]", // 빈 템플릿 배열
                templateType,
                resourceIdentifier,
                Map.of("error", errorMessage, "failedAt", System.currentTimeMillis())
        );
    }
    
    @Override
    public String getResponseType() {
        return "CONDITION_TEMPLATE_GENERATION";
    }
    
    @Override
    public Object getData() {
        return Map.of(
                "templates", templateResult != null ? templateResult : "[]",
                "templateType", templateType != null ? templateType : "",
                "resourceIdentifier", resourceIdentifier != null ? resourceIdentifier : "",
                "metadata", processingMetadata,
                "timestamp", getTimestamp(),
                "requestId", getRequestId()
        );
    }
    
    /**
     * 템플릿 결과가 비어있는지 확인
     */
    public boolean hasTemplates() {
        return templateResult != null && 
               !templateResult.trim().isEmpty() && 
               !templateResult.trim().equals("[]");
    }
    
    @Override
    public String toString() {
        return String.format("ConditionTemplateGenerationResponse{type='%s', resource='%s', status='%s', hasTemplates=%s}", 
                templateType, resourceIdentifier, getStatus(), hasTemplates());
    }
} 