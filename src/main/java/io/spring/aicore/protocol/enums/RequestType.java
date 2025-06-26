package io.spring.aicore.protocol.enums;

/**
 * 요청 타입
 * ✅ SRP 준수: 요청 타입 정의만 담당
 */
public enum RequestType {
    QUERY("조회", "데이터 조회 요청"),
    COMMAND("명령", "시스템 상태 변경 요청"),
    ANALYSIS("분석", "데이터 분석 요청"),
    GENERATION("생성", "콘텐츠 생성 요청"),
    VALIDATION("검증", "데이터 검증 요청"),
    OPTIMIZATION("최적화", "성능 최적화 요청"),
    MONITORING("모니터링", "시스템 모니터링 요청");
    
    private final String displayName;
    private final String description;
    
    RequestType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
} 