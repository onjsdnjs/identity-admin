package io.spring.identityadmin.aiam.protocol.types;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 보안 이벤트 정보 클래스
 * ✅ SRP 준수: 보안 이벤트 데이터만 담당
 */
public class SecurityEvent {
    private final String eventId;
    private final String eventType;
    private final String severity;
    private final LocalDateTime timestamp;
    private final String description;
    private final Map<String, Object> details;
    
    public SecurityEvent(String eventType, String severity, String description) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.eventType = eventType;
        this.severity = severity;
        this.description = description;
        this.timestamp = LocalDateTime.now();
        this.details = new ConcurrentHashMap<>();
    }
    
    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }
    
    // Getters
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getSeverity() { return severity; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public Map<String, Object> getDetails() { return Map.copyOf(details); }
    
    @Override
    public String toString() {
        return String.format("SecurityEvent{id='%s', type='%s', severity='%s'}", 
                eventId, eventType, severity);
    }
} 