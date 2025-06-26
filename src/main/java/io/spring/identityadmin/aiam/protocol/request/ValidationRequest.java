package io.spring.identityadmin.aiam.protocol.request;

import io.spring.identityadmin.aiam.protocol.IAMRequest;
import io.spring.identityadmin.aiam.protocol.types.PolicyContext;

import java.util.List;

/**
 * 검증 요청 클래스
 * 정책 유효성 검증을 위한 요청
 */
public class ValidationRequest<T extends PolicyContext> extends IAMRequest<T> {
    
    private String targetPolicyId;
    private String policyContent;
    private List<String> validationRules;
    private String validationType;
    private boolean strictMode;
    
    public ValidationRequest(T context) {
        super(context, "VALIDATION");
        this.validationType = "COMPREHENSIVE";
        this.strictMode = false;
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
    
    @Override
    public String toString() {
        return String.format("ValidationRequest{policy='%s', type='%s', strict=%s}", 
                targetPolicyId, validationType, strictMode);
    }
} 