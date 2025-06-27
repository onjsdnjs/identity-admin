package io.spring.iam.aiam.protocol.response;

import io.spring.iam.aiam.protocol.IAMResponse;

import java.util.List;
import java.util.Map;

/**
 * 위험 평가 응답 클래스
 * 위험도 분석 결과를 담는 응답
 */
public class RiskAssessmentResponse extends IAMResponse {
    
    private String riskLevel;
    private Double riskScore;
    private List<RiskFactor> identifiedRisks;
    private List<String> recommendations;
    private Map<String, Object> riskMetrics;
    private String assessmentSummary;
    
    public RiskAssessmentResponse(String requestId, ExecutionStatus status) {
        super(requestId, status);
    }
    
    public RiskAssessmentResponse(String requestId, ExecutionStatus status, String riskLevel, Double riskScore) {
        super(requestId, status);
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
    }
    
    @Override
    public Object getData() { 
        return identifiedRisks; 
    }
    
    @Override
    public String getResponseType() { 
        return "RISK_ASSESSMENT"; 
    }
    
    // Getters and Setters
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    
    public List<RiskFactor> getIdentifiedRisks() { return identifiedRisks; }
    public void setIdentifiedRisks(List<RiskFactor> identifiedRisks) { this.identifiedRisks = identifiedRisks; }
    
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    
    public Map<String, Object> getRiskMetrics() { return riskMetrics; }
    public void setRiskMetrics(Map<String, Object> riskMetrics) { this.riskMetrics = riskMetrics; }
    
    public String getAssessmentSummary() { return assessmentSummary; }
    public void setAssessmentSummary(String assessmentSummary) { this.assessmentSummary = assessmentSummary; }
    
    /**
     * 위험 요소를 담는 내부 클래스
     * 단순한 데이터 홀더이므로 내부 클래스로 적절함
     */
    public static class RiskFactor {
        private String factorType;
        private String description;
        private String severity;
        private Double impact;
        
        public RiskFactor(String factorType, String description, String severity) {
            this.factorType = factorType;
            this.description = description;
            this.severity = severity;
        }
        
        // Getters and Setters
        public String getFactorType() { return factorType; }
        public void setFactorType(String factorType) { this.factorType = factorType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public Double getImpact() { return impact; }
        public void setImpact(Double impact) { this.impact = impact; }
    }
    
    @Override
    public String toString() {
        return String.format("RiskAssessmentResponse{level='%s', score=%.2f, factors=%d}", 
                riskLevel, riskScore != null ? riskScore : 0.0, 
                identifiedRisks != null ? identifiedRisks.size() : 0);
    }
} 