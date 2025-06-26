package io.spring.identityadmin.aiam.protocol.response;

import io.spring.identityadmin.aiam.protocol.IAMResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 감사 분석 응답 클래스
 * 감사 로그 분석 결과를 담는 응답
 */
public class AuditAnalysisResponse extends IAMResponse {
    
    private LocalDateTime analysisStartTime;
    private LocalDateTime analysisEndTime;
    private int totalLogsAnalyzed;
    private List<AuditFinding> findings;
    private Map<String, Object> analysisMetrics;
    private List<String> securityRecommendations;
    private Double riskScore;
    
    public AuditAnalysisResponse(String requestId, ExecutionStatus status) {
        super(requestId, status);
    }
    
    public AuditAnalysisResponse(String requestId, ExecutionStatus status, List<AuditFinding> findings) {
        super(requestId, status);
        this.findings = findings;
    }
    
    @Override
    public Object getData() { 
        return findings; 
    }
    
    @Override
    public String getResponseType() { 
        return "AUDIT_ANALYSIS"; 
    }
    
    // Getters and Setters
    public LocalDateTime getAnalysisStartTime() { return analysisStartTime; }
    public void setAnalysisStartTime(LocalDateTime analysisStartTime) { this.analysisStartTime = analysisStartTime; }
    
    public LocalDateTime getAnalysisEndTime() { return analysisEndTime; }
    public void setAnalysisEndTime(LocalDateTime analysisEndTime) { this.analysisEndTime = analysisEndTime; }
    
    public int getTotalLogsAnalyzed() { return totalLogsAnalyzed; }
    public void setTotalLogsAnalyzed(int totalLogsAnalyzed) { this.totalLogsAnalyzed = totalLogsAnalyzed; }
    
    public List<AuditFinding> getFindings() { return findings; }
    public void setFindings(List<AuditFinding> findings) { this.findings = findings; }
    
    public Map<String, Object> getAnalysisMetrics() { return analysisMetrics; }
    public void setAnalysisMetrics(Map<String, Object> analysisMetrics) { this.analysisMetrics = analysisMetrics; }
    
    public List<String> getSecurityRecommendations() { return securityRecommendations; }
    public void setSecurityRecommendations(List<String> securityRecommendations) { this.securityRecommendations = securityRecommendations; }
    
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    
    /**
     * 감사 발견사항을 담는 내부 클래스
     * 단순한 데이터 홀더이므로 내부 클래스로 적절함
     */
    public static class AuditFinding {
        private String findingId;
        private String findingType;
        private String description;
        private String severity;
        private LocalDateTime timestamp;
        private String affectedEntity;
        
        public AuditFinding(String findingType, String description, String severity) {
            this.findingId = java.util.UUID.randomUUID().toString();
            this.findingType = findingType;
            this.description = description;
            this.severity = severity;
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters and Setters
        public String getFindingId() { return findingId; }
        public void setFindingId(String findingId) { this.findingId = findingId; }
        
        public String getFindingType() { return findingType; }
        public void setFindingType(String findingType) { this.findingType = findingType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public String getAffectedEntity() { return affectedEntity; }
        public void setAffectedEntity(String affectedEntity) { this.affectedEntity = affectedEntity; }
    }
    
    @Override
    public String toString() {
        return String.format("AuditAnalysisResponse{logsAnalyzed=%d, findings=%d, riskScore=%.2f}", 
                totalLogsAnalyzed, findings != null ? findings.size() : 0, riskScore != null ? riskScore : 0.0);
    }
} 