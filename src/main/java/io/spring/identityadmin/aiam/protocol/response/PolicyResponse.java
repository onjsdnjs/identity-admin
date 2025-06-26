package io.spring.identityadmin.aiam.protocol.response;

import io.spring.identityadmin.aiam.protocol.IAMResponse;

import java.util.List;

/**
 * 정책 관련 응답 클래스
 * 정책 생성, 최적화, 검증 결과를 담는 응답
 */
public class PolicyResponse extends IAMResponse {
    
    private String generatedPolicy;
    private Double policyConfidenceScore;
    private List<String> appliedRules;
    private String policyFormat;
    private boolean optimized;
    
    public PolicyResponse(String requestId, ExecutionStatus status) {
        super(requestId, status);
    }
    
    public PolicyResponse(String requestId, ExecutionStatus status, String generatedPolicy) {
        super(requestId, status);
        this.generatedPolicy = generatedPolicy;
    }
    
    @Override
    public Object getData() { 
        return generatedPolicy; 
    }
    
    @Override
    public String getResponseType() { 
        return "POLICY"; 
    }
    
    // Getters and Setters
    public String getGeneratedPolicy() { return generatedPolicy; }
    public void setGeneratedPolicy(String generatedPolicy) { this.generatedPolicy = generatedPolicy; }
    
    public Double getPolicyConfidenceScore() { return policyConfidenceScore; }
    public void setPolicyConfidenceScore(Double policyConfidenceScore) { this.policyConfidenceScore = policyConfidenceScore; }
    
    public List<String> getAppliedRules() { return appliedRules; }
    public void setAppliedRules(List<String> appliedRules) { this.appliedRules = appliedRules; }
    
    public String getPolicyFormat() { return policyFormat; }
    public void setPolicyFormat(String policyFormat) { this.policyFormat = policyFormat; }
    
    public boolean isOptimized() { return optimized; }
    public void setOptimized(boolean optimized) { this.optimized = optimized; }
    
    @Override
    public String toString() {
        return String.format("PolicyResponse{status=%s, confidence=%.2f, optimized=%s}", 
                getStatus(), policyConfidenceScore != null ? policyConfidenceScore : 0.0, optimized);
    }
} 