package io.spring.iam.aiam.protocol.enums;

/**
 * 사용자 분석 유형을 정의하는 열거형
 * AI가 사용자 분석을 수행할 때 적용할 분석 유형을 결정합니다
 */
public enum UserAnalysisType {
    /**
     * 행동 분석 - 사용자 행동 패턴 분석
     */
    BEHAVIOR_ANALYSIS("행동 분석", "사용자의 접근 패턴과 행동을 분석하여 이상 징후 탐지"),
    
    /**
     * 권한 분석 - 사용자 권한 적절성 분석
     */
    PERMISSION_ANALYSIS("권한 분석", "사용자가 보유한 권한의 적절성과 과도함 여부 분석"),
    
    /**
     * 위험 분석 - 사용자 관련 보안 위험 분석
     */
    RISK_ANALYSIS("위험 분석", "사용자와 관련된 보안 위험 요소를 종합적으로 분석"),
    
    /**
     * 종합 분석 - 모든 측면을 포함한 종합 분석
     */
    COMPREHENSIVE_ANALYSIS("종합 분석", "행동, 권한, 위험을 모두 포함한 종합적인 사용자 분석");
    
    private final String displayName;
    private final String description;
    
    UserAnalysisType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * 사용자 분석 유형의 표시명을 반환합니다
     * @return 한국어 표시명
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 사용자 분석 유형의 상세 설명을 반환합니다
     * @return 상세 설명
     */
    public String getDescription() {
        return description;
    }
} 