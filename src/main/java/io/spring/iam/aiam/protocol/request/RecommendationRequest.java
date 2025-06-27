package io.spring.iam.aiam.protocol.request;

import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;

import java.util.List;
import java.util.Map;

/**
 * 추천 요청 클래스
 * AI 기반 스마트 추천을 위한 요청
 */
public class RecommendationRequest<T extends IAMContext> extends IAMRequest<T> {
    
    private String recommendationType;
    private String targetEntity;
    private List<String> preferredCategories;
    private Map<String, Object> constraints;
    private int maxRecommendations;
    private double minConfidenceThreshold;
    
    public RecommendationRequest(T context) {
        super(context, "RECOMMENDATION");
        this.maxRecommendations = 10;
        this.minConfidenceThreshold = 0.5;
        this.recommendationType = "GENERAL";
    }
    
    // Getters and Setters
    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
    
    public String getTargetEntity() { return targetEntity; }
    public void setTargetEntity(String targetEntity) { this.targetEntity = targetEntity; }
    
    public List<String> getPreferredCategories() { return preferredCategories; }
    public void setPreferredCategories(List<String> preferredCategories) { this.preferredCategories = preferredCategories; }
    
    public Map<String, Object> getConstraints() { return constraints; }
    public void setConstraints(Map<String, Object> constraints) { this.constraints = constraints; }
    
    public int getMaxRecommendations() { return maxRecommendations; }
    public void setMaxRecommendations(int maxRecommendations) { this.maxRecommendations = maxRecommendations; }
    
    public double getMinConfidenceThreshold() { return minConfidenceThreshold; }
    public void setMinConfidenceThreshold(double minConfidenceThreshold) { this.minConfidenceThreshold = minConfidenceThreshold; }
    
    @Override
    public String toString() {
        return String.format("RecommendationRequest{type='%s', target='%s', maxCount=%d}", 
                recommendationType, targetEntity, maxRecommendations);
    }
} 