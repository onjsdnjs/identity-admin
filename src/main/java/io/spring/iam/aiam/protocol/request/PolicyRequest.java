package io.spring.iam.aiam.protocol.request;

import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.types.PolicyContext;

/**
 * 정책 관련 요청 클래스
 * 정책 생성, 최적화, 검증 등의 작업에 사용
 */
public class PolicyRequest<T extends PolicyContext> extends IAMRequest<T> {
    
    private String policyType;
    private String targetResource;
    private boolean includeOptimization;
    
    public PolicyRequest(T context, String operation) {
        super(context, operation);
    }
    
    public PolicyRequest(T context, String operation, String policyType) {
        super(context, operation);
        this.policyType = policyType;
    }
    
    // Getters and Setters
    public String getPolicyType() { return policyType; }
    public void setPolicyType(String policyType) { this.policyType = policyType; }
    
    public String getTargetResource() { return targetResource; }
    public void setTargetResource(String targetResource) { this.targetResource = targetResource; }
    
    public boolean isIncludeOptimization() { return includeOptimization; }
    public void setIncludeOptimization(boolean includeOptimization) { this.includeOptimization = includeOptimization; }
    
    @Override
    public String toString() {
        return String.format("PolicyRequest{operation='%s', policyType='%s', resource='%s'}", 
                getOperation(), policyType, targetResource);
    }
} 