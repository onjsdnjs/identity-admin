package io.spring.identityadmin.aiam.protocol.request;

import io.spring.identityadmin.aiam.protocol.IAMContext;
import io.spring.identityadmin.aiam.protocol.IAMRequest;

import java.util.List;

/**
 * 정책 충돌 감지 요청 클래스
 * 정책 간 충돌을 감지하기 위한 요청
 */
public class ConflictDetectionRequest<T extends IAMContext> extends IAMRequest<T> {
    
    private String targetPolicyId;
    private List<String> existingPolicyIds;
    private String conflictScope;
    private boolean includeWarnings;
    
    public ConflictDetectionRequest(T context) {
        super(context, "CONFLICT_DETECTION");
        this.includeWarnings = true;
        this.conflictScope = "ALL";
    }
    
    // Getters and Setters
    public String getTargetPolicyId() { return targetPolicyId; }
    public void setTargetPolicyId(String targetPolicyId) { this.targetPolicyId = targetPolicyId; }
    
    public List<String> getExistingPolicyIds() { return existingPolicyIds; }
    public void setExistingPolicyIds(List<String> existingPolicyIds) { this.existingPolicyIds = existingPolicyIds; }
    
    public String getConflictScope() { return conflictScope; }
    public void setConflictScope(String conflictScope) { this.conflictScope = conflictScope; }
    
    public boolean isIncludeWarnings() { return includeWarnings; }
    public void setIncludeWarnings(boolean includeWarnings) { this.includeWarnings = includeWarnings; }
    
    @Override
    public String toString() {
        return String.format("ConflictDetectionRequest{targetPolicy='%s', scope='%s', warnings=%s}", 
                targetPolicyId, conflictScope, includeWarnings);
    }
} 