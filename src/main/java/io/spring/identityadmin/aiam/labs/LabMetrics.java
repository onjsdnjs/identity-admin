package io.spring.identityadmin.aiam.labs;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 연구소 메트릭 클래스
 * ✅ SRP 준수: 메트릭 관리만 담당
 */
public class LabMetrics {
    private final String labName;
    private long totalRequests = 0;
    private long successfulRequests = 0;
    private long failedRequests = 0;
    private long totalResponseTime = 0;
    private long streamEvents = 0;
    private final Map<String, Integer> errorCounts = new ConcurrentHashMap<>();
    private LocalDateTime lastActivity;
    
    public LabMetrics(String labName) {
        this.labName = labName;
        this.lastActivity = LocalDateTime.now();
    }
    
    public synchronized void recordRequestStart() {
        totalRequests++;
        lastActivity = LocalDateTime.now();
    }
    
    public synchronized void recordSuccess() {
        successfulRequests++;
    }
    
    public synchronized void recordError(Throwable error) {
        failedRequests++;
        String errorType = error.getClass().getSimpleName();
        errorCounts.merge(errorType, 1, Integer::sum);
    }
    
    public synchronized void recordStreamEvent() {
        streamEvents++;
    }
    
    public synchronized void recordResponseTime(long millis) {
        totalResponseTime += millis;
    }
    
    public double getSuccessRate() {
        return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
    }
    
    public double getAverageResponseTime() {
        return successfulRequests > 0 ? (double) totalResponseTime / successfulRequests : 0.0;
    }
    
    // Getters
    public String getLabName() { return labName; }
    public long getTotalRequests() { return totalRequests; }
    public long getSuccessfulRequests() { return successfulRequests; }
    public long getFailedRequests() { return failedRequests; }
    public long getStreamEvents() { return streamEvents; }
    public Map<String, Integer> getErrorCounts() { return Map.copyOf(errorCounts); }
    public LocalDateTime getLastActivity() { return lastActivity; }
    
    @Override
    public String toString() {
        return String.format("LabMetrics{lab='%s', total=%d, success=%d, failed=%d, successRate=%.2f%%}", 
                labName, totalRequests, successfulRequests, failedRequests, getSuccessRate() * 100);
    }
} 