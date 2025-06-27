package io.spring.iam.aiam.protocol.response;

import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.iam.aiam.protocol.enums.UserAnalysisType;

import java.util.List;
import java.util.Map;

/**
 * 사용자 분석 응답 클래스
 * 사용자 행동, 권한, 위험도 분석 결과를 담는 응답
 */
public class UserAnalysisResponse extends IAMResponse {
    
    private String analyzedUserId;
    private UserAnalysisType analysisType;
    private UserAnalysisResult analysisResult;
    private List<String> identifiedRisks;
    private List<String> recommendations;
    private Map<String, Object> behaviorPatterns;
    private Double riskScore;
    
    public UserAnalysisResponse(String requestId, ExecutionStatus status) {
        super(requestId, status);
    }
    
    public UserAnalysisResponse(String requestId, ExecutionStatus status, UserAnalysisResult result) {
        super(requestId, status);
        this.analysisResult = result;
    }
    
    @Override
    public Object getData() { 
        return analysisResult; 
    }
    
    @Override
    public String getResponseType() { 
        return "USER_ANALYSIS"; 
    }
    
    // Getters and Setters
    public String getAnalyzedUserId() { return analyzedUserId; }
    public void setAnalyzedUserId(String analyzedUserId) { this.analyzedUserId = analyzedUserId; }
    
    public UserAnalysisType getAnalysisType() { return analysisType; }
    public void setAnalysisType(UserAnalysisType analysisType) { this.analysisType = analysisType; }
    
    public UserAnalysisResult getAnalysisResult() { return analysisResult; }
    public void setAnalysisResult(UserAnalysisResult analysisResult) { this.analysisResult = analysisResult; }
    
    public List<String> getIdentifiedRisks() { return identifiedRisks; }
    public void setIdentifiedRisks(List<String> identifiedRisks) { this.identifiedRisks = identifiedRisks; }
    
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    
    public Map<String, Object> getBehaviorPatterns() { return behaviorPatterns; }
    public void setBehaviorPatterns(Map<String, Object> behaviorPatterns) { this.behaviorPatterns = behaviorPatterns; }
    
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    
    /**
     * 사용자 분석 결과를 담는 내부 클래스
     * 단순한 데이터 홀더이므로 내부 클래스로 적절함
     */
    public static class UserAnalysisResult {
        private String summary;
        private Map<String, Object> detailedFindings;
        private Double overallScore;
        private String analysisTimestamp;
        
        public UserAnalysisResult(String summary, Double overallScore) {
            this.summary = summary;
            this.overallScore = overallScore;
            this.analysisTimestamp = java.time.LocalDateTime.now().toString();
        }
        
        // Getters and Setters
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public Map<String, Object> getDetailedFindings() { return detailedFindings; }
        public void setDetailedFindings(Map<String, Object> detailedFindings) { this.detailedFindings = detailedFindings; }
        
        public Double getOverallScore() { return overallScore; }
        public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }
        
        public String getAnalysisTimestamp() { return analysisTimestamp; }
        public void setAnalysisTimestamp(String analysisTimestamp) { this.analysisTimestamp = analysisTimestamp; }
    }
    
    @Override
    public String toString() {
        return String.format("UserAnalysisResponse{user='%s', type=%s, riskScore=%.2f}", 
                analyzedUserId, analysisType, riskScore != null ? riskScore : 0.0);
    }
} 