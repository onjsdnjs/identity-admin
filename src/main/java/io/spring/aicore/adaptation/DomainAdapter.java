package io.spring.aicore.adaptation;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.aicore.protocol.DomainContext;

import java.util.List;
import java.util.Optional;

/**
 * 도메인 어댑터 인터페이스
 * 도메인 특화 객체와 AI Core 범용 객체 간의 변환을 담당
 * 
 * @param <T> 도메인 컨텍스트 타입
 * @param <D> 도메인 특화 객체 타입
 */
public interface DomainAdapter<T extends DomainContext, D> {
    
    /**
     * 도메인 요청을 AI 요청으로 변환합니다
     * @param domainRequest 도메인 특화 요청
     * @return AI 요청
     */
    AIRequest<T> adaptRequest(D domainRequest);
    
    /**
     * AI 응답을 도메인 응답으로 변환합니다
     * @param aiResponse AI 응답
     * @param targetType 목표 도메인 타입
     * @return 도메인 특화 응답
     */
    <R> R adaptResponse(AIResponse aiResponse, Class<R> targetType);
    
    /**
     * 배치 요청을 변환합니다
     * @param domainRequests 도메인 요청 목록
     * @return AI 요청 목록
     */
    List<AIRequest<T>> adaptRequests(List<D> domainRequests);
    
    /**
     * 배치 응답을 변환합니다
     * @param aiResponses AI 응답 목록
     * @param targetType 목표 도메인 타입
     * @return 도메인 응답 목록
     */
    <R> List<R> adaptResponses(List<AIResponse> aiResponses, Class<R> targetType);
    
    /**
     * 도메인 컨텍스트를 풍부하게 만듭니다
     * @param context 기본 컨텍스트
     * @param domainData 도메인 데이터
     * @return 풍부해진 컨텍스트
     */
    T enrichContext(T context, D domainData);
    
    /**
     * 지원하는 도메인 타입을 반환합니다
     * @return 도메인 타입
     */
    Class<D> getSupportedDomainType();
    
    /**
     * 지원하는 컨텍스트 타입을 반환합니다
     * @return 컨텍스트 타입
     */
    Class<T> getSupportedContextType();
    
    /**
     * 특정 작업을 지원하는지 확인합니다
     * @param operation 작업명
     * @return 지원 여부
     */
    boolean supportsOperation(String operation);
    
    /**
     * 변환 가능한지 검증합니다
     * @param domainRequest 도메인 요청
     * @return 검증 결과
     */
    ValidationResult validateRequest(D domainRequest);
    
    /**
     * 변환 결과를 검증합니다
     * @param aiResponse AI 응답
     * @param targetType 목표 타입
     * @return 검증 결과
     */
    <R> ValidationResult validateResponse(AIResponse aiResponse, Class<R> targetType);
    
    /**
     * 검증 결과 클래스
     */
    class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final List<String> warnings;
        
        private ValidationResult(boolean valid, String errorMessage, List<String> warnings) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.warnings = warnings;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null, List.of());
        }
        
        public static ValidationResult success(List<String> warnings) {
            return new ValidationResult(true, null, warnings);
        }
        
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage, List.of());
        }
        
        public static ValidationResult failure(String errorMessage, List<String> warnings) {
            return new ValidationResult(false, errorMessage, warnings);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public List<String> getWarnings() { return warnings; }
        public boolean hasWarnings() { return warnings != null && !warnings.isEmpty(); }
        
        @Override
        public String toString() {
            if (valid) {
                return hasWarnings() ? "Valid with warnings: " + warnings : "Valid";
            } else {
                return "Invalid: " + errorMessage;
            }
        }
    }
}
