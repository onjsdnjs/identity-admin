package io.spring.iam.aiam.protocol.request;

import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.types.PolicyContext;

import java.util.List;

/**
 * 정책 검증 요청 클래스
 * 정책의 유효성, 일관성, 보안성을 검증하기 위한 요청
 */
public class ValidationRequest<T extends PolicyContext> extends IAMRequest<T> {
    
    private String targetPolicyId;
    private String policyContent;
    private List<String> validationRules;
    private String validationType;
    private boolean strictMode;
    private boolean includeWarnings;
    
    public ValidationRequest(T context) {
        super(context, "POLICY_VALIDATION");
        this.validationType = "COMPREHENSIVE";
        this.strictMode = false;
        this.includeWarnings = true;
    }
    
    // Getters and Setters
    public String getTargetPolicyId() { return targetPolicyId; }
    public void setTargetPolicyId(String targetPolicyId) { this.targetPolicyId = targetPolicyId; }
    
    public String getPolicyContent() { return policyContent; }
    public void setPolicyContent(String policyContent) { this.policyContent = policyContent; }
    
    public List<String> getValidationRules() { return validationRules; }
    public void setValidationRules(List<String> validationRules) { this.validationRules = validationRules; }
    
    public String getValidationType() { return validationType; }
    public void setValidationType(String validationType) { this.validationType = validationType; }
    
    public boolean isStrictMode() { return strictMode; }
    public void setStrictMode(boolean strictMode) { this.strictMode = strictMode; }
    
    public boolean isIncludeWarnings() { return includeWarnings; }
    public void setIncludeWarnings(boolean includeWarnings) { this.includeWarnings = includeWarnings; }
    
    @Override
    public String toString() {
        return String.format("ValidationRequest{policy='%s', type='%s', strict=%s, warnings=%s}", 
                targetPolicyId, validationType, strictMode, includeWarnings);
    }
} 