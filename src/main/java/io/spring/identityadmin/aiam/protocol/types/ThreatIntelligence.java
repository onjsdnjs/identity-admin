package io.spring.identityadmin.aiam.protocol.types;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 위협 인텔리전스 정보 클래스
 * ✅ SRP 준수: 위협 정보 관리만 담당
 */
public class ThreatIntelligence {
    private final Map<String, Object> threatData;
    private String threatLevel;
    private boolean hasActiveThreat;
    private List<String> threatSources;
    private LocalDateTime lastUpdate;
    
    public ThreatIntelligence() {
        this.threatData = new ConcurrentHashMap<>();
        this.threatLevel = "LOW";
        this.hasActiveThreat = false;
        this.lastUpdate = LocalDateTime.now();
    }
    
    public void updateThreatLevel(String level, boolean hasActive) {
        this.threatLevel = level;
        this.hasActiveThreat = hasActive;
        this.lastUpdate = LocalDateTime.now();
    }
    
    public void addThreatData(String key, Object value) {
        this.threatData.put(key, value);
    }
    
    // Getters
    public Map<String, Object> getThreatData() { return Map.copyOf(threatData); }
    public String getThreatLevel() { return threatLevel; }
    public boolean hasActiveThreat() { return hasActiveThreat; }
    public List<String> getThreatSources() { return threatSources; }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    
    public void setThreatSources(List<String> threatSources) { this.threatSources = threatSources; }
    
    @Override
    public String toString() {
        return String.format("ThreatIntelligence{level='%s', active=%s, sources=%d}", 
                threatLevel, hasActiveThreat, threatSources != null ? threatSources.size() : 0);
    }
} 