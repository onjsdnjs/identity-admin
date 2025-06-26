package io.spring.identityadmin.aiam.protocol.request;

import io.spring.identityadmin.aiam.protocol.IAMContext;
import io.spring.identityadmin.aiam.protocol.IAMRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 감사 로그 분석 요청 클래스
 * 대용량 감사 로그를 분석하여 보안 이벤트, 패턴, 이상 징후를 감지하기 위한 요청
 */
public class AuditAnalysisRequest<T extends IAMContext> extends IAMRequest<T> {
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<String> logSources;
    private String analysisType;
    private int batchSize;
    private long analysisTimeoutSeconds;
    
    public AuditAnalysisRequest(T context) {
        super(context, "AUDIT_LOG_ANALYSIS");
        this.analysisType = "COMPREHENSIVE";
        this.batchSize = 1000;
        this.analysisTimeoutSeconds = 300L;
    }
    
    // Getters and Setters
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public List<String> getLogSources() { return logSources; }
    public void setLogSources(List<String> logSources) { this.logSources = logSources; }
    
    public String getAnalysisType() { return analysisType; }
    public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }
    
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    
    public long getAnalysisTimeoutSeconds() { return analysisTimeoutSeconds; }
    public void setAnalysisTimeoutSeconds(long analysisTimeoutSeconds) { this.analysisTimeoutSeconds = analysisTimeoutSeconds; }
    
    @Override
    public String toString() {
        return String.format("AuditAnalysisRequest{period=%s to %s, type='%s', batch=%d}", 
                startDate, endDate, analysisType, batchSize);
    }
} 