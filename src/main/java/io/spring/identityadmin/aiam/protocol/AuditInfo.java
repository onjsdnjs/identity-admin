package io.spring.identityadmin.aiam.protocol;

import java.time.LocalDateTime;

/**
 * 감사 정보 클래스
 * ✅ SRP 준수: 감사 정보 관리만 담당
 */
public class AuditInfo {
    private final LocalDateTime auditTimestamp;
    private String auditTrailId;
    private String userId;
    private String action;
    private boolean auditRequired;
    
    public AuditInfo() {
        this.auditTimestamp = LocalDateTime.now();
        this.auditRequired = true;
    }
    
    public void recordAction(String userId, String action) {
        this.userId = userId;
        this.action = action;
        this.auditTrailId = generateAuditTrailId();
    }
    
    private String generateAuditTrailId() {
        return "AUDIT_" + System.currentTimeMillis() + "_" + userId;
    }
    
    // Getters
    public LocalDateTime getAuditTimestamp() { return auditTimestamp; }
    public String getAuditTrailId() { return auditTrailId; }
    public String getUserId() { return userId; }
    public String getAction() { return action; }
    public boolean isAuditRequired() { return auditRequired; }
    
    public void setAuditRequired(boolean auditRequired) { this.auditRequired = auditRequired; }
    
    @Override
    public String toString() {
        return String.format("AuditInfo{id='%s', user='%s', action='%s'}", 
                auditTrailId, userId, action);
    }
} 