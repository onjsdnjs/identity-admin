package io.spring.iam.aiam.protocol.types;

import java.time.LocalDateTime;

/**
 * 접근 기록 정보 클래스
 * ✅ SRP 준수: 개별 접근 기록 데이터만 담당
 */
public class AccessRecord {
    private final String recordId;
    private final String resourceId;
    private final String action;
    private final LocalDateTime timestamp;
    private final boolean successful;
    private final String sourceIp;
    
    public AccessRecord(String resourceId, String action, boolean successful, String sourceIp) {
        this.recordId = java.util.UUID.randomUUID().toString();
        this.resourceId = resourceId;
        this.action = action;
        this.successful = successful;
        this.sourceIp = sourceIp;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters
    public String getRecordId() { return recordId; }
    public String getResourceId() { return resourceId; }
    public String getAction() { return action; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isSuccessful() { return successful; }
    public String getSourceIp() { return sourceIp; }
    
    @Override
    public String toString() {
        return String.format("AccessRecord{id='%s', resource='%s', action='%s', success=%s}", 
                recordId, resourceId, action, successful);
    }
} 