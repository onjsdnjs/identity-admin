package io.spring.iam.aiam.protocol.response;

import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMResponse;

import java.util.List;

/**
 * 추천 응답 클래스
 * AI 기반 스마트 추천 결과를 담는 응답
 */
public class RecommendationResponse<T extends IAMContext> extends IAMResponse {
    
    private List<Recommendation<T>> recommendations;
    private String recommendationType;
    private Double confidenceScore;
    private String rationale;
    
    public RecommendationResponse(String requestId, ExecutionStatus status) {
        super(requestId, status);
    }
    
    public RecommendationResponse(String requestId, ExecutionStatus status, List<Recommendation<T>> recommendations) {
        super(requestId, status);
        this.recommendations = recommendations;
    }
    
    @Override
    public Object getData() { 
        return recommendations; 
    }
    
    @Override
    public String getResponseType() { 
        return "RECOMMENDATION"; 
    }
    
    // Getters and Setters
    public List<Recommendation<T>> getRecommendations() { return recommendations; }
    public void setRecommendations(List<Recommendation<T>> recommendations) { this.recommendations = recommendations; }
    
    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
    
    public Double getRecommendationConfidence() { return confidenceScore; }
    public void setRecommendationConfidence(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    
    /**
     * 개별 추천 항목을 담는 내부 클래스
     * 단순한 데이터 홀더이므로 내부 클래스로 적절함
     */
    public static class Recommendation<T extends IAMContext> {
        private String recommendationId;
        private String title;
        private String description;
        private String category;
        private Double priority;
        private T relatedContext;
        
        public Recommendation(String recommendationId, String title, String description) {
            this.recommendationId = recommendationId;
            this.title = title;
            this.description = description;
        }
        
        // Getters and Setters
        public String getRecommendationId() { return recommendationId; }
        public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public Double getPriority() { return priority; }
        public void setPriority(Double priority) { this.priority = priority; }
        
        public T getRelatedContext() { return relatedContext; }
        public void setRelatedContext(T relatedContext) { this.relatedContext = relatedContext; }
    }
    
    @Override
    public String toString() {
        return String.format("RecommendationResponse{type=%s, count=%d, confidence=%.2f}", 
                recommendationType, recommendations != null ? recommendations.size() : 0, 
                confidenceScore != null ? confidenceScore : 0.0);
    }
} 