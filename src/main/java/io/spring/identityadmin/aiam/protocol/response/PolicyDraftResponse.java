package io.spring.identityadmin.aiam.protocol.response;

import io.spring.identityadmin.aiam.protocol.IAMResponse;

/**
 * 정책 초안 응답 클래스
 * 스트리밍 정책 생성에서 사용되는 개별 초안 응답
 */
public class PolicyDraftResponse extends IAMResponse {
    
    private String draftContent;
    private Double completionPercentage;
    private String draftStatus;
    private boolean isFinalDraft;
    
    public PolicyDraftResponse(String requestId, ExecutionStatus status) {
        super(requestId, status);
        this.isFinalDraft = false;
    }
    
    public PolicyDraftResponse(String requestId, ExecutionStatus status, String draftContent) {
        super(requestId, status);
        this.draftContent = draftContent;
        this.isFinalDraft = false;
    }
    
    @Override
    public Object getData() { 
        return draftContent; 
    }
    
    @Override
    public String getResponseType() { 
        return "POLICY_DRAFT"; 
    }
    
    // Getters and Setters
    public String getDraftContent() { return draftContent; }
    public void setDraftContent(String draftContent) { this.draftContent = draftContent; }
    
    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }
    
    public String getDraftStatus() { return draftStatus; }
    public void setDraftStatus(String draftStatus) { this.draftStatus = draftStatus; }
    
    public boolean isFinalDraft() { return isFinalDraft; }
    public void setFinalDraft(boolean finalDraft) { isFinalDraft = finalDraft; }
    
    @Override
    public String toString() {
        return String.format("PolicyDraftResponse{status=%s, completion=%.1f%%, final=%s}", 
                getStatus(), completionPercentage != null ? completionPercentage : 0.0, isFinalDraft);
    }
} 