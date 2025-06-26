package io.spring.identityadmin.aiam.protocol.enums;

/**
 * 정책 생성 모드
 * ✅ SRP 준수: 정책 생성 모드 정의만 담당
 */
public enum PolicyGenerationMode {
    SIMPLE("간단생성", "기본적인 정책 생성"),
    ADVANCED("고급생성", "복잡한 조건을 포함한 정책 생성"),
    AI_ASSISTED("AI보조", "AI를 활용한 지능형 정책 생성"),
    TEMPLATE_BASED("템플릿기반", "사전 정의된 템플릿을 사용한 정책 생성");
    
    private final String displayName;
    private final String description;
    
    PolicyGenerationMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
} 