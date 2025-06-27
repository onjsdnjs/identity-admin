package io.spring.iam.aiam.protocol.enums;

/**
 * 위험 분석 범위를 정의하는 열거형
 * AI가 위험 분석을 수행할 때 적용할 범위를 결정합니다
 */
public enum RiskAnalysisScope {
    /**
     * 사용자 수준 - 개별 사용자 중심 분석
     */
    USER_LEVEL("사용자 수준", "개별 사용자의 권한과 행동 패턴을 중심으로 분석"),
    
    /**
     * 역할 수준 - 역할 기반 분석
     */
    ROLE_LEVEL("역할 수준", "역할별 권한 집합과 위험도를 중심으로 분석"),
    
    /**
     * 리소스 수준 - 리소스 중심 분석
     */
    RESOURCE_LEVEL("리소스 수준", "특정 리소스에 대한 접근 패턴과 위험도 분석"),
    
    /**
     * 시스템 수준 - 전체 시스템 분석
     */
    SYSTEM_LEVEL("시스템 수준", "전체 시스템의 보안 상태와 위험 요소를 종합 분석");
    
    private final String displayName;
    private final String description;
    
    RiskAnalysisScope(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * 위험 분석 범위의 표시명을 반환합니다
     * @return 한국어 표시명
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 위험 분석 범위의 상세 설명을 반환합니다
     * @return 상세 설명
     */
    public String getDescription() {
        return description;
    }
} 