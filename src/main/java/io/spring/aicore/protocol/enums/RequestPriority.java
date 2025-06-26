package io.spring.aicore.protocol.enums;

/**
 * 요청 우선순위
 * ✅ SRP 준수: 요청 우선순위 정의만 담당
 */
public enum RequestPriority {
    LOW("낮음", 1),
    NORMAL("보통", 2),
    HIGH("높음", 3),
    URGENT("긴급", 4),
    CRITICAL("매우긴급", 5);
    
    private final String displayName;
    private final int level;
    
    RequestPriority(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }
    
    public String getDisplayName() { return displayName; }
    public int getLevel() { return level; }
    
    public boolean isHigherThan(RequestPriority other) {
        return this.level > other.level;
    }
} 