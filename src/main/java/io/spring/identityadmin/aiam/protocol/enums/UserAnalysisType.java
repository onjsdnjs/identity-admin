package io.spring.identityadmin.aiam.protocol.enums;

/**
 * 사용자 분석 타입
 * ✅ SRP 준수: 사용자 분석 타입 정의만 담당
 */
public enum UserAnalysisType {
    BEHAVIOR_ANALYSIS("행동분석", "사용자의 행동 패턴 분석"),
    ACCESS_PATTERN("접근패턴", "리소스 접근 패턴 분석"),
    RISK_ASSESSMENT("위험평가", "사용자별 보안 위험 평가"),
    ROLE_OPTIMIZATION("역할최적화", "사용자 역할 할당 최적화 분석"),
    COMPLIANCE_CHECK("컴플라이언스", "규정 준수 상태 분석");
    
    private final String displayName;
    private final String description;
    
    UserAnalysisType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
} 