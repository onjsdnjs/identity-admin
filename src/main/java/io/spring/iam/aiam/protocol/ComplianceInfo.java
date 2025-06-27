package io.spring.iam.aiam.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 컴플라이언스 정보 클래스
 * ✅ SRP 준수: 컴플라이언스 정보 관리만 담당
 */
public class ComplianceInfo {
    private final Map<String, Boolean> complianceChecks;
    private String overallStatus;
    private String complianceFramework;
    
    public ComplianceInfo() {
        this.complianceChecks = new ConcurrentHashMap<>();
        this.overallStatus = "PENDING";
    }
    
    public void addComplianceCheck(String checkName, boolean passed) {
        complianceChecks.put(checkName, passed);
        updateOverallStatus();
    }
    
    private void updateOverallStatus() {
        if (complianceChecks.isEmpty()) {
            this.overallStatus = "PENDING";
        } else if (complianceChecks.values().stream().allMatch(Boolean::booleanValue)) {
            this.overallStatus = "COMPLIANT";
        } else {
            this.overallStatus = "NON_COMPLIANT";
        }
    }
    
    // Getters
    public Map<String, Boolean> getComplianceChecks() { return Map.copyOf(complianceChecks); }
    public String getOverallStatus() { return overallStatus; }
    public String getComplianceFramework() { return complianceFramework; }
    
    public void setComplianceFramework(String complianceFramework) { 
        this.complianceFramework = complianceFramework; 
    }
    
    @Override
    public String toString() {
        return String.format("ComplianceInfo{status='%s', framework='%s', checks=%d}", 
                overallStatus, complianceFramework, complianceChecks.size());
    }
} 