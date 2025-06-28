package io.spring.iam.aiam.protocol.enums;

/**
 * AIAM 진단 요청 우선순위
 */
public enum RequestPriority {
    LOW("낮음"),
    NORMAL("보통"),
    HIGH("높음"),
    URGENT("긴급");
    
    private final String description;
    
    RequestPriority(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 