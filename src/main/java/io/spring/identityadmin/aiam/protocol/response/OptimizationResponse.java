package io.spring.identityadmin.aiam.protocol.response;

import io.spring.identityadmin.aiam.protocol.IAMResponse;

import java.util.List;
import java.util.Map;

/**
 * 최적화 응답 클래스
 * 정책 최적화 결과를 담는 응답
 */
public class OptimizationResponse extends IAMResponse {
    
    private String originalPolicyId;
    private String optimizedPolicy;
    private List<OptimizationSuggestion> suggestions;
    private Map<String, Object> optimizationMetrics;
    private Double improvementScore;
    private boolean isOptimizationSuccessful;
    
    public OptimizationResponse(String requestId, ExecutionStatus status) {
        super(requestId, status);
        this.isOptimizationSuccessful = false;
    }
    
    public OptimizationResponse(String requestId, ExecutionStatus status, String optimizedPolicy) {
        super(requestId, status);
        this.optimizedPolicy = optimizedPolicy;
        this.isOptimizationSuccessful = true;
    }
    
    @Override
    public Object getData() { 
        return optimizedPolicy; 
    }
    
    @Override
    public String getResponseType() { 
        return "OPTIMIZATION"; 
    }
    
    // Getters and Setters
    public String getOriginalPolicyId() { return originalPolicyId; }
    public void setOriginalPolicyId(String originalPolicyId) { this.originalPolicyId = originalPolicyId; }
    
    public String getOptimizedPolicy() { return optimizedPolicy; }
    public void setOptimizedPolicy(String optimizedPolicy) { this.optimizedPolicy = optimizedPolicy; }
    
    public List<OptimizationSuggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<OptimizationSuggestion> suggestions) { this.suggestions = suggestions; }
    
    public Map<String, Object> getOptimizationMetrics() { return optimizationMetrics; }
    public void setOptimizationMetrics(Map<String, Object> optimizationMetrics) { this.optimizationMetrics = optimizationMetrics; }
    
    public Double getImprovementScore() { return improvementScore; }
    public void setImprovementScore(Double improvementScore) { this.improvementScore = improvementScore; }
    
    public boolean isOptimizationSuccessful() { return isOptimizationSuccessful; }
    public void setOptimizationSuccessful(boolean optimizationSuccessful) { isOptimizationSuccessful = optimizationSuccessful; }
    
    /**
     * 최적화 제안사항을 담는 내부 클래스
     * 단순한 데이터 홀더이므로 내부 클래스로 적절함
     */
    public static class OptimizationSuggestion {
        private String suggestionType;
        private String description;
        private String impact;
        private Double priority;
        
        public OptimizationSuggestion(String suggestionType, String description) {
            this.suggestionType = suggestionType;
            this.description = description;
            this.priority = 1.0;
        }
        
        // Getters and Setters
        public String getSuggestionType() { return suggestionType; }
        public void setSuggestionType(String suggestionType) { this.suggestionType = suggestionType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getImpact() { return impact; }
        public void setImpact(String impact) { this.impact = impact; }
        
        public Double getPriority() { return priority; }
        public void setPriority(Double priority) { this.priority = priority; }
    }
    
    @Override
    public String toString() {
        return String.format("OptimizationResponse{policy='%s', successful=%s, improvement=%.2f}", 
                originalPolicyId, isOptimizationSuccessful, improvementScore != null ? improvementScore : 0.0);
    }
} 