package io.spring.identityadmin.aiam.protocol.enums;

/**
 * 위험 분석 범위
 * ✅ SRP 준수: 위험 분석 범위 정의만 담당
 */
public enum RiskAnalysisScope {
    USER_LEVEL("사용자수준", "개별 사용자의 위험 분석"),
    GROUP_LEVEL("그룹수준", "그룹 단위의 위험 분석"),
    SYSTEM_LEVEL("시스템수준", "전체 시스템의 위험 분석"),
    POLICY_LEVEL("정책수준", "특정 정책의 위험 분석"),
    COMPREHENSIVE("종합분석", "모든 수준을 포괄하는 종합적 위험 분석");
    
    private final String displayName;
    private final String description;
    
    RiskAnalysisScope(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
} 