package io.spring.iam.aiam.protocol.types;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 접근 이력 정보 클래스
 * ✅ SRP 준수: 접근 이력 관리만 담당
 */
public class AccessHistory {
    private final List<AccessRecord> accessRecords;
    private int totalAccesses;
    private LocalDateTime lastAccess;
    private Map<String, Integer> resourceAccessCounts;
    
    public AccessHistory() {
        this.accessRecords = new ArrayList<>();
        this.resourceAccessCounts = new ConcurrentHashMap<>();
        this.totalAccesses = 0;
    }
    
    public void addAccessRecord(AccessRecord record) {
        this.accessRecords.add(record);
        this.totalAccesses++;
        this.lastAccess = record.getTimestamp();
        
        String resource = record.getResourceId();
        resourceAccessCounts.merge(resource, 1, Integer::sum);
    }
    
    public int getComplexityScore() {
        // 접근 기록의 복잡도를 1-3 범위로 계산
        if (accessRecords.size() > 1000) return 3;
        if (accessRecords.size() > 100) return 2;
        return 1;
    }
    
    // Getters
    public List<AccessRecord> getAccessRecords() { return List.copyOf(accessRecords); }
    public int getTotalAccesses() { return totalAccesses; }
    public LocalDateTime getLastAccess() { return lastAccess; }
    public Map<String, Integer> getResourceAccessCounts() { return Map.copyOf(resourceAccessCounts); }
    
    @Override
    public String toString() {
        return String.format("AccessHistory{totalAccesses=%d, lastAccess=%s, uniqueResources=%d}", 
                totalAccesses, lastAccess, resourceAccessCounts.size());
    }
} 