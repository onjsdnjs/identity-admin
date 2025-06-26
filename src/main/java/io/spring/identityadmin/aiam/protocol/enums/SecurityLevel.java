package io.spring.identityadmin.aiam.protocol.enums;

/**
 * 보안 레벨
 * ✅ SRP 준수: 보안 레벨 정의만 담당
 */
public enum SecurityLevel {
    LOW("낮음", "기본적인 보안 검증", 1),
    MEDIUM("보통", "표준 보안 검증", 2),
    HIGH("높음", "강화된 보안 검증", 3),
    CRITICAL("매우높음", "최고 수준 보안 검증", 4);
    
    private final String displayName;
    private final String description;
    private final int level;
    
    SecurityLevel(String displayName, String description, int level) {
        this.displayName = displayName;
        this.description = description;
        this.level = level;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getLevel() { return level; }
} 