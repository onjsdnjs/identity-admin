package io.spring.iam.aiam.protocol.types;

import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.enums.AuditRequirement;
import io.spring.iam.aiam.protocol.enums.SecurityLevel;
import io.spring.iam.aiam.protocol.enums.UserAnalysisType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 사용자 분석을 위한 특화된 IAM 컨텍스트
 * 사용자 관련 AI 작업에 필요한 모든 컨텍스트 정보를 포함
 */
public class UserContext extends IAMContext {
    
    private UserProfile userProfile;
    private List<String> currentRoles;
    private List<String> currentPermissions;
    private AccessHistory accessHistory;
    private Set<String> accessibleResources;
    private UserAnalysisType analysisType;
    private Map<String, Object> behaviorPatterns;
    private boolean includeRecommendations;
    
    public UserContext(SecurityLevel securityLevel, AuditRequirement auditRequirement) {
        super(securityLevel, auditRequirement);
        this.analysisType = UserAnalysisType.BEHAVIOR_ANALYSIS;
        this.includeRecommendations = true;
    }
    
    public UserContext(String userId, String sessionId, SecurityLevel securityLevel, AuditRequirement auditRequirement) {
        super(userId, sessionId, securityLevel, auditRequirement);
        this.analysisType = UserAnalysisType.BEHAVIOR_ANALYSIS;
        this.includeRecommendations = true;
    }
    
    @Override
    public String getIAMContextType() {
        return "USER";
    }
    
    /**
     * 사용자 분석에 필요한 모든 컨텍스트가 완전한지 확인합니다
     * @return 완전성 여부
     */
    public boolean isComplete() {
        return userProfile != null && getUserId() != null;
    }
    
    /**
     * 사용자 분석 복잡도를 계산합니다
     * @return 복잡도 점수 (1-10)
     */
    public int calculateAnalysisComplexity() {
        int complexity = 1;
        
        if (currentRoles != null) complexity += Math.min(currentRoles.size() / 3, 2);
        if (currentPermissions != null) complexity += Math.min(currentPermissions.size() / 10, 2);
        if (accessibleResources != null) complexity += Math.min(accessibleResources.size() / 20, 2);
        if (accessHistory != null) complexity += accessHistory.getComplexityScore();
        if (behaviorPatterns != null) complexity += Math.min(behaviorPatterns.size() / 5, 2);
        
        return Math.min(complexity, 10);
    }
    
    /**
     * 실시간 분석이 권장되는지 확인합니다
     * @return 실시간 분석 권장 여부
     */
    public boolean isRealTimeAnalysisRecommended() {
        return getSecurityLevel() == SecurityLevel.ENHANCED || getSecurityLevel() == SecurityLevel.MAXIMUM ||
               analysisType == UserAnalysisType.RISK_ANALYSIS ||
               (userProfile != null && userProfile.isHighRiskUser());
    }
    
    // Builder 패턴 지원
    public static class Builder {
        private final UserContext context;
        
        public Builder(SecurityLevel securityLevel, AuditRequirement auditRequirement) {
            this.context = new UserContext(securityLevel, auditRequirement);
        }
        
        public Builder(String userId, String sessionId, SecurityLevel securityLevel, AuditRequirement auditRequirement) {
            this.context = new UserContext(userId, sessionId, securityLevel, auditRequirement);
        }
        
        public Builder withUserProfile(UserProfile profile) {
            context.userProfile = profile;
            return this;
        }
        
        public Builder withCurrentRoles(List<String> roles) {
            context.currentRoles = roles;
            return this;
        }
        
        public Builder withCurrentPermissions(List<String> permissions) {
            context.currentPermissions = permissions;
            return this;
        }
        
        public Builder withAccessHistory(AccessHistory history) {
            context.accessHistory = history;
            return this;
        }
        
        public Builder withAccessibleResources(Set<String> resources) {
            context.accessibleResources = resources;
            return this;
        }
        
        public Builder withAnalysisType(UserAnalysisType type) {
            context.analysisType = type;
            return this;
        }
        
        public Builder withBehaviorPatterns(Map<String, Object> patterns) {
            context.behaviorPatterns = patterns;
            return this;
        }
        
        public Builder withRecommendations(boolean include) {
            context.includeRecommendations = include;
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
        
        public UserContext build() {
            return context;
        }
    }
    
    // Getters and Setters
    public UserProfile getUserProfile() { return userProfile; }
    public void setUserProfile(UserProfile userProfile) { this.userProfile = userProfile; }
    
    public List<String> getCurrentRoles() { return currentRoles; }
    public void setCurrentRoles(List<String> currentRoles) { this.currentRoles = currentRoles; }
    
    public List<String> getCurrentPermissions() { return currentPermissions; }
    public void setCurrentPermissions(List<String> currentPermissions) { this.currentPermissions = currentPermissions; }
    
    public AccessHistory getAccessHistory() { return accessHistory; }
    public void setAccessHistory(AccessHistory accessHistory) { this.accessHistory = accessHistory; }
    
    public Set<String> getAccessibleResources() { return accessibleResources; }
    public void setAccessibleResources(Set<String> accessibleResources) { this.accessibleResources = accessibleResources; }
    
    public UserAnalysisType getAnalysisType() { return analysisType; }
    public void setAnalysisType(UserAnalysisType analysisType) { this.analysisType = analysisType; }
    
    public Map<String, Object> getBehaviorPatterns() { return behaviorPatterns; }
    public void setBehaviorPatterns(Map<String, Object> behaviorPatterns) { this.behaviorPatterns = behaviorPatterns; }
    
    public boolean isIncludeRecommendations() { return includeRecommendations; }
    public void setIncludeRecommendations(boolean includeRecommendations) { this.includeRecommendations = includeRecommendations; }
    
    // ✅ 내부 클래스 분리 완료 - 별도 파일로 이동됨
    
    @Override
    public String toString() {
        return String.format("UserContext{id='%s', analysisType=%s, roles=%d, complexity=%d}", 
                getContextId(), analysisType, 
                currentRoles != null ? currentRoles.size() : 0,
                calculateAnalysisComplexity());
    }
} 