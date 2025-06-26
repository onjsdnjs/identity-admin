package io.spring.identityadmin.aiam.operations;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.identityadmin.aiam.protocol.IAMContext;
import io.spring.identityadmin.aiam.protocol.IAMRequest;
import io.spring.identityadmin.aiam.protocol.IAMResponse;
import org.springframework.stereotype.Component;

/**
 * IAM과 AI Core 간의 타입 변환기
 * 
 * 🎯 완벽한 타입 안전성 보장
 * - IAM 타입 ↔ AI Core 타입 양방향 변환
 * - 메타데이터 보존
 * - 성능 최적화된 변환
 */
@Component
public class IAMTypeConverter {
    
    /**
     * IAM 요청을 AI Core 요청으로 변환
     */
    @SuppressWarnings("unchecked")
    public <T extends IAMContext> AIRequest<T> toAIRequest(IAMRequest<T> iamRequest) {
        // IAMRequest는 AIRequest를 확장하므로 안전한 캐스팅
        return (AIRequest<T>) iamRequest;
    }
    
    /**
     * 범용 요청을 IAM 요청으로 변환 (타입 안전성)
     */
    @SuppressWarnings("unchecked")
    public <T extends IAMContext> IAMRequest<T> toIAMRequest(Object request) {
        if (request instanceof IAMRequest) {
            return (IAMRequest<T>) request;
        }
        
        // 필요시 다른 타입의 요청을 IAMRequest로 변환하는 로직 추가
        throw new IllegalArgumentException("Cannot convert " + request.getClass() + " to IAMRequest");
    }
    
    /**
     * IAM 응답 타입에 대응하는 AI Core 응답 타입 반환
     */
    @SuppressWarnings("unchecked")
    public <R extends IAMResponse> Class<? extends AIResponse> toCoreResponseType(Class<R> iamResponseType) {
        // IAMResponse는 AIResponse를 확장하므로 안전한 캐스팅
        return (Class<? extends AIResponse>) iamResponseType;
    }
    
    /**
     * AI Core 응답을 IAM 응답으로 변환
     */
    @SuppressWarnings("unchecked")
    public <R extends IAMResponse> R toIAMResponse(AIResponse coreResponse, Class<R> targetType) {
        if (targetType.isInstance(coreResponse)) {
            return (R) coreResponse;
        }
        
        // 타입 변환이 필요한 경우의 로직
        throw new IllegalArgumentException("Cannot convert " + coreResponse.getClass() + " to " + targetType);
    }
    
    /**
     * 메타데이터 보존하며 응답 변환
     */
    public <R extends IAMResponse> R convertWithMetadata(AIResponse source, Class<R> targetType) {
        R converted = toIAMResponse(source, targetType);
        
        // 메타데이터 복사
        if (converted instanceof IAMResponse && source instanceof IAMResponse) {
            IAMResponse iamSource = (IAMResponse) source;
            IAMResponse iamTarget = (IAMResponse) converted;
            
            // 감사 정보 복사
            iamTarget.setAuditInfo(iamSource.getAuditInfo());
            iamTarget.setSecurityValidation(iamSource.getSecurityValidation());
            iamTarget.setComplianceInfo(iamSource.getComplianceInfo());
        }
        
        return converted;
    }
} 