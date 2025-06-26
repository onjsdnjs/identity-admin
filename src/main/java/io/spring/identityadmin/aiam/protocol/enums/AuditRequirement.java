package io.spring.identityadmin.aiam.protocol.enums;

/**
 * 감사 요구사항
 * ✅ SRP 준수: 감사 요구사항 정의만 담당
 */
public enum AuditRequirement {
    NONE("감사불필요", "감사 로깅이 필요하지 않음"),
    BASIC("기본감사", "기본적인 감사 로깅"),
    DETAILED("상세감사", "상세한 감사 로깅"),
    FULL("전체감사", "모든 활동에 대한 완전한 감사 로깅");
    
    private final String displayName;
    private final String description;
    
    AuditRequirement(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
} 