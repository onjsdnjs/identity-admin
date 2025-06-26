package io.spring.identityadmin.aiam.protocol.types;

import io.spring.identityadmin.aiam.protocol.IAMContext;
import io.spring.identityadmin.aiam.protocol.enums.AuditRequirement;
import io.spring.identityadmin.aiam.protocol.enums.RiskAnalysisScope;
import io.spring.identityadmin.aiam.protocol.enums.SecurityLevel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 위험 분석을 위한 특화된 IAM 컨텍스트
 * 위험 분석 AI 작업에 필요한 모든 컨텍스트 정보를 포함
 */
public class RiskContext extends IAMContext {
    
    private Map<String, Object> currentPolicies;
    private List<SecurityEvent> recentSecurityEvents;
    private ThreatIntelligence threatIntelligence;
    private List<String> targetResources;
    private Set<String> relevantUsers;
    private RiskAnalysisScope analysisScope;
    private Map<String, Double> riskFactors;
    private boolean includeHistoricalData;
    
    public RiskContext(SecurityLevel securityLevel, AuditRequirement auditRequirement) {
        super(securityLevel, auditRequirement);
        this.analysisScope = RiskAnalysisScope.USER_LEVEL;
        this.includeHistoricalData = true;
    }
    
    public RiskContext(String userId, String sessionId, SecurityLevel securityLevel, AuditRequirement auditRequirement) {
        super(userId, sessionId, securityLevel, auditRequirement);
        this.analysisScope = RiskAnalysisScope.USER_LEVEL;
        this.includeHistoricalData = true;
    }
    
    @Override
    public String getIAMContextType() {
        return "RISK";
    }
    
    /**
     * 위험 분석에 필요한 모든 컨텍스트가 완전한지 확인합니다
     * @return 완전성 여부
     */
    public boolean isComplete() {
        return currentPolicies != null && !currentPolicies.isEmpty() &&
               targetResources != null && !targetResources.isEmpty();
    }
    
    /**
     * 위험 분석 복잡도를 계산합니다
     * @return 복잡도 점수 (1-10)
     */
    public int calculateAnalysisComplexity() {
        int complexity = 1;
        
        if (currentPolicies != null) complexity += Math.min(currentPolicies.size() / 10, 2);
        if (targetResources != null) complexity += Math.min(targetResources.size() / 5, 2);
        if (relevantUsers != null) complexity += Math.min(relevantUsers.size() / 20, 2);
        if (recentSecurityEvents != null) complexity += Math.min(recentSecurityEvents.size() / 50, 2);
        if (includeHistoricalData) complexity += 1;
        
        return Math.min(complexity, 10);
    }
    
    /**
     * 실시간 분석이 권장되는지 확인합니다
     * @return 실시간 분석 권장 여부
     */
    public boolean isRealTimeAnalysisRecommended() {
        return getSecurityLevel() == SecurityLevel.HIGH || getSecurityLevel() == SecurityLevel.CRITICAL ||
               analysisScope == RiskAnalysisScope.SYSTEM_LEVEL ||
               (threatIntelligence != null && threatIntelligence.hasActiveThreat());
    }
    
    // Builder 패턴 지원
    public static class Builder {
        private final RiskContext context;
        
        public Builder(SecurityLevel securityLevel, AuditRequirement auditRequirement) {
            this.context = new RiskContext(securityLevel, auditRequirement);
        }
        
        public Builder(String userId, String sessionId, SecurityLevel securityLevel, AuditRequirement auditRequirement) {
            this.context = new RiskContext(userId, sessionId, securityLevel, auditRequirement);
        }
        
        public Builder withCurrentPolicies(Map<String, Object> policies) {
            context.currentPolicies = policies;
            return this;
        }
        
        public Builder withRecentSecurityEvents(List<SecurityEvent> events) {
            context.recentSecurityEvents = events;
            return this;
        }
        
        public Builder withThreatIntelligence(ThreatIntelligence threatIntel) {
            context.threatIntelligence = threatIntel;
            return this;
        }
        
        public Builder withTargetResources(List<String> resources) {
            context.targetResources = resources;
            return this;
        }
        
        public Builder withRelevantUsers(Set<String> users) {
            context.relevantUsers = users;
            return this;
        }
        
        public Builder withAnalysisScope(RiskAnalysisScope scope) {
            context.analysisScope = scope;
            return this;
        }
        
        public Builder withRiskFactors(Map<String, Double> factors) {
            context.riskFactors = factors;
            return this;
        }
        
        public Builder withHistoricalData(boolean include) {
            context.includeHistoricalData = include;
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
        
        public RiskContext build() {
            return context;
        }
    }
    
    // Getters and Setters
    public Map<String, Object> getCurrentPolicies() { return currentPolicies; }
    public void setCurrentPolicies(Map<String, Object> currentPolicies) { this.currentPolicies = currentPolicies; }
    
    public List<SecurityEvent> getRecentSecurityEvents() { return recentSecurityEvents; }
    public void setRecentSecurityEvents(List<SecurityEvent> recentSecurityEvents) { this.recentSecurityEvents = recentSecurityEvents; }
    
    public ThreatIntelligence getThreatIntelligence() { return threatIntelligence; }
    public void setThreatIntelligence(ThreatIntelligence threatIntelligence) { this.threatIntelligence = threatIntelligence; }
    
    public List<String> getTargetResources() { return targetResources; }
    public void setTargetResources(List<String> targetResources) { this.targetResources = targetResources; }
    
    public Set<String> getRelevantUsers() { return relevantUsers; }
    public void setRelevantUsers(Set<String> relevantUsers) { this.relevantUsers = relevantUsers; }
    
    public RiskAnalysisScope getAnalysisScope() { return analysisScope; }
    public void setAnalysisScope(RiskAnalysisScope analysisScope) { this.analysisScope = analysisScope; }
    
    public Map<String, Double> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(Map<String, Double> riskFactors) { this.riskFactors = riskFactors; }
    
    public boolean isIncludeHistoricalData() { return includeHistoricalData; }
    public void setIncludeHistoricalData(boolean includeHistoricalData) { this.includeHistoricalData = includeHistoricalData; }
    
    // ✅ 내부 클래스 분리 완료 - 별도 파일로 이동됨
    
    @Override
    public String toString() {
        return String.format("RiskContext{id='%s', scope=%s, resources=%d, complexity=%d}", 
                getContextId(), analysisScope, 
                targetResources != null ? targetResources.size() : 0,
                calculateAnalysisComplexity());
    }
} 