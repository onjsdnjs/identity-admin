package io.spring.iam.aiam.protocol.types;

import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.enums.AuditRequirement;
import io.spring.iam.aiam.protocol.enums.PolicyGenerationMode;
import io.spring.iam.aiam.protocol.enums.SecurityLevel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 정책 생성 및 관리를 위한 특화된 IAM 컨텍스트
 * 정책 생성 AI 작업에 필요한 모든 컨텍스트 정보를 포함
 */
public class PolicyContext extends IAMContext {
    
    private List<String> availableRoles;
    private List<String> availablePermissions;
    private List<String> availableConditionTypes;
    private List<String> availableResources;
    private Map<String, Object> currentPolicySet;
    private Set<String> businessRules;
    private String naturalLanguageQuery;
    private PolicyGenerationMode generationMode;
    private boolean allowExperimentalFeatures;
    
    public PolicyContext(SecurityLevel securityLevel, AuditRequirement auditRequirement) {
        super(securityLevel, auditRequirement);
        this.generationMode = PolicyGenerationMode.QUICK;
        this.allowExperimentalFeatures = false;
    }
    
    public PolicyContext(String userId, String sessionId, SecurityLevel securityLevel, AuditRequirement auditRequirement) {
        super(userId, sessionId, securityLevel, auditRequirement);
        this.generationMode = PolicyGenerationMode.QUICK;
        this.allowExperimentalFeatures = false;
    }
    
    @Override
    public String getIAMContextType() {
        return "POLICY";
    }
    
    /**
     * 정책 생성에 필요한 모든 컨텍스트가 완전한지 확인합니다
     * @return 완전성 여부
     */
    public boolean isComplete() {
        return availableRoles != null && !availableRoles.isEmpty() &&
               availablePermissions != null && !availablePermissions.isEmpty() &&
               availableConditionTypes != null && !availableConditionTypes.isEmpty() &&
               naturalLanguageQuery != null && !naturalLanguageQuery.trim().isEmpty();
    }
    
    /**
     * 정책 생성 복잡도를 계산합니다
     * @return 복잡도 점수 (1-10)
     */
    public int calculateComplexity() {
        int complexity = 1;
        
        if (availableRoles != null) complexity += Math.min(availableRoles.size() / 5, 2);
        if (availablePermissions != null) complexity += Math.min(availablePermissions.size() / 10, 2);
        if (availableConditionTypes != null) complexity += Math.min(availableConditionTypes.size() / 3, 2);
        if (businessRules != null) complexity += Math.min(businessRules.size() / 5, 2);
        if (allowExperimentalFeatures) complexity += 1;
        
        return Math.min(complexity, 10);
    }
    
    /**
     * 스트리밍 모드가 권장되는지 확인합니다
     * @return 스트리밍 권장 여부
     */
    public boolean isStreamingRecommended() {
        return calculateComplexity() >= 6 || 
               (naturalLanguageQuery != null && naturalLanguageQuery.length() > 200) ||
               generationMode == PolicyGenerationMode.AI_ASSISTED;
    }
    
    // Builder 패턴 지원
    public static class Builder {
        private final PolicyContext context;
        
        public Builder(SecurityLevel securityLevel, AuditRequirement auditRequirement) {
            this.context = new PolicyContext(securityLevel, auditRequirement);
        }
        
        public Builder(String userId, String sessionId, SecurityLevel securityLevel, AuditRequirement auditRequirement) {
            this.context = new PolicyContext(userId, sessionId, securityLevel, auditRequirement);
        }
        
        public Builder withAvailableRoles(List<String> roles) {
            context.availableRoles = roles;
            return this;
        }
        
        public Builder withAvailablePermissions(List<String> permissions) {
            context.availablePermissions = permissions;
            return this;
        }
        
        public Builder withAvailableConditionTypes(List<String> conditionTypes) {
            context.availableConditionTypes = conditionTypes;
            return this;
        }
        
        public Builder withAvailableResources(List<String> resources) {
            context.availableResources = resources;
            return this;
        }
        
        public Builder withCurrentPolicySet(Map<String, Object> policySet) {
            context.currentPolicySet = policySet;
            return this;
        }
        
        public Builder withBusinessRules(Set<String> businessRules) {
            context.businessRules = businessRules;
            return this;
        }
        
        public Builder withNaturalLanguageQuery(String query) {
            context.naturalLanguageQuery = query;
            return this;
        }
        
        public Builder withGenerationMode(PolicyGenerationMode mode) {
            context.generationMode = mode;
            return this;
        }
        
        public Builder withExperimentalFeatures(boolean allow) {
            context.allowExperimentalFeatures = allow;
            return this;
        }
        
        public Builder withOrganizationId(String organizationId) {
            context.setOrganizationId(organizationId);
            return this;
        }
        
        public Builder withTenantId(String tenantId) {
            context.setTenantId(tenantId);
            return this;
        }
        
        public PolicyContext build() {
            return context;
        }
    }
    
    // Getters and Setters
    public List<String> getAvailableRoles() { return availableRoles; }
    public void setAvailableRoles(List<String> availableRoles) { this.availableRoles = availableRoles; }
    
    public List<String> getAvailablePermissions() { return availablePermissions; }
    public void setAvailablePermissions(List<String> availablePermissions) { this.availablePermissions = availablePermissions; }
    
    public List<String> getAvailableConditionTypes() { return availableConditionTypes; }
    public void setAvailableConditionTypes(List<String> availableConditionTypes) { this.availableConditionTypes = availableConditionTypes; }
    
    public List<String> getAvailableResources() { return availableResources; }
    public void setAvailableResources(List<String> availableResources) { this.availableResources = availableResources; }
    
    public Map<String, Object> getCurrentPolicySet() { return currentPolicySet; }
    public void setCurrentPolicySet(Map<String, Object> currentPolicySet) { this.currentPolicySet = currentPolicySet; }
    
    public Set<String> getBusinessRules() { return businessRules; }
    public void setBusinessRules(Set<String> businessRules) { this.businessRules = businessRules; }
    
    public String getNaturalLanguageQuery() { return naturalLanguageQuery; }
    public void setNaturalLanguageQuery(String naturalLanguageQuery) { this.naturalLanguageQuery = naturalLanguageQuery; }
    
    public PolicyGenerationMode getGenerationMode() { return generationMode; }
    public void setGenerationMode(PolicyGenerationMode generationMode) { this.generationMode = generationMode; }
    
    public boolean isAllowExperimentalFeatures() { return allowExperimentalFeatures; }
    public void setAllowExperimentalFeatures(boolean allowExperimentalFeatures) { this.allowExperimentalFeatures = allowExperimentalFeatures; }
    
    // ✅ Enum 분리 완료 - enums 패키지로 이동됨
    
    @Override
    public String toString() {
        return String.format("PolicyContext{id='%s', mode=%s, roles=%d, permissions=%d, complexity=%d}", 
                getContextId(), generationMode, 
                availableRoles != null ? availableRoles.size() : 0,
                availablePermissions != null ? availablePermissions.size() : 0,
                calculateComplexity());
    }
} 