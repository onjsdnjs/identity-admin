package io.spring.identityadmin.aiam.protocol.request;

import io.spring.identityadmin.aiam.protocol.IAMContext;
import io.spring.identityadmin.aiam.protocol.IAMRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 감사 분석 요청 클래스
 * 감사 로그 분석을 위한 요청
 */
public class AuditAnalysisRequest<T extends IAMContext> extends IAMRequest<T> {
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<String> logSources;
    private List<String> analysisTypes;
    private String analysisScope;
    private boolean includeRecommendations;
    
    public AuditAnalysisRequest(T context) {
        super(context, "AUDIT_ANALYSIS");
        this.endDate = LocalDateTime.now();
        this.startDate = endDate.minusDays(7); // 기본 7일
        this.analysisScope = "COMPREHENSIVE";
        this.includeRecommendations = true;
    }
    
    // Getters and Setters
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public List<String> getLogSources() { return logSources; }
    public void setLogSources(List<String> logSources) { this.logSources = logSources; }
    
    public List<String> getAnalysisTypes() { return analysisTypes; }
    public void setAnalysisTypes(List<String> analysisTypes) { this.analysisTypes = analysisTypes; }
    
    public String getAnalysisScope() { return analysisScope; }
    public void setAnalysisScope(String analysisScope) { this.analysisScope = analysisScope; }
    
    public boolean isIncludeRecommendations() { return includeRecommendations; }
    public void setIncludeRecommendations(boolean includeRecommendations) { this.includeRecommendations = includeRecommendations; }
    
    @Override
    public String toString() {
        return String.format("AuditAnalysisRequest{period=%s to %s, scope='%s'}", 
                startDate, endDate, analysisScope);
    }
} 