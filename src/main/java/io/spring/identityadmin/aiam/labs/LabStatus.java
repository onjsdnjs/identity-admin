package io.spring.identityadmin.aiam.labs;

import java.time.LocalDateTime;

/**
 * 연구소 상태 클래스
 * ✅ SRP 준수: 상태 관리만 담당
 */
public class LabStatus {
    private final String labName;
    private final double successRate;
    private final double averageResponseTime;
    private final LocalDateTime timestamp;
    private final HealthStatus healthStatus;
    
    public LabStatus(String labName, double successRate, double averageResponseTime) {
        this.labName = labName;
        this.successRate = successRate;
        this.averageResponseTime = averageResponseTime;
        this.timestamp = LocalDateTime.now();
        this.healthStatus = calculateHealthStatus(successRate, averageResponseTime);
    }
    
    private HealthStatus calculateHealthStatus(double successRate, double avgResponseTime) {
        if (successRate >= 0.95 && avgResponseTime < 1000) return HealthStatus.EXCELLENT;
        if (successRate >= 0.90 && avgResponseTime < 2000) return HealthStatus.GOOD;
        if (successRate >= 0.80 && avgResponseTime < 5000) return HealthStatus.FAIR;
        return HealthStatus.POOR;
    }
    
    // Getters
    public String getLabName() { return labName; }
    public double getSuccessRate() { return successRate; }
    public double getAverageResponseTime() { return averageResponseTime; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public HealthStatus getHealthStatus() { return healthStatus; }
    
    public enum HealthStatus {
        EXCELLENT, GOOD, FAIR, POOR
    }
    
    @Override
    public String toString() {
        return String.format("LabStatus{lab='%s', health=%s, success=%.2f%%, avgTime=%.0fms}", 
                labName, healthStatus, successRate * 100, averageResponseTime);
    }
} 