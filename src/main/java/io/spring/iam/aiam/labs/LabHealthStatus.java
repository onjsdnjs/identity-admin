package io.spring.iam.aiam.labs;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 🔬 IAM 연구소 헬스 상태
 * 
 * 연구소의 건강 상태와 진단 정보를 제공
 */
public class LabHealthStatus {
    
    private final String labId;
    private final String labName;
    private final HealthLevel healthLevel;
    private final String message;
    private final LocalDateTime checkTime;
    private final long responseTimeMs;
    private final HealthDetails details;
    
    private LabHealthStatus(String labId, String labName, HealthLevel healthLevel, 
                          String message, HealthDetails details) {
        this.labId = labId;
        this.labName = labName;
        this.healthLevel = healthLevel;
        this.message = message;
        this.checkTime = LocalDateTime.now();
        this.responseTimeMs = System.currentTimeMillis();
        this.details = details;
    }
    
    /**
     * 건강한 상태 생성
     */
    public static LabHealthStatus healthy(String labId, String labName) {
        return new LabHealthStatus(labId, labName, HealthLevel.HEALTHY, 
                                 "Lab is operating normally", HealthDetails.createHealthy());
    }
    
    /**
     * 성능 저하 상태 생성
     */
    public static LabHealthStatus degraded(String labId, String labName, String reason) {
        return new LabHealthStatus(labId, labName, HealthLevel.DEGRADED, 
                                 reason, HealthDetails.createDegraded(reason));
    }
    
    /**
     * 실패 상태 생성
     */
    public static LabHealthStatus failed(String labId, String labName, String error) {
        return new LabHealthStatus(labId, labName, HealthLevel.FAILED, 
                                 error, HealthDetails.createFailed(error));
    }
    
    /**
     * 경고 상태 생성
     */
    public static LabHealthStatus warning(String labId, String labName, String warning) {
        return new LabHealthStatus(labId, labName, HealthLevel.WARNING, 
                                 warning, HealthDetails.createWarning(warning));
    }
    
    /**
     * 헬스 레벨
     */
    public enum HealthLevel {
        HEALTHY(100, "Healthy", "🟢", "연구소가 정상적으로 작동"),
        WARNING(75, "Warning", "🟡", "주의가 필요한 상태"),
        DEGRADED(50, "Degraded", "🟠", "성능 저하 상태"),
        FAILED(0, "Failed", "🔴", "심각한 문제 발생");
        
        private final int score;
        private final String displayName;
        private final String emoji;
        private final String description;
        
        HealthLevel(int score, String displayName, String emoji, String description) {
            this.score = score;
            this.displayName = displayName;
            this.emoji = emoji;
            this.description = description;
        }
        
        public int getScore() { return score; }
        public String getDisplayName() { return displayName; }
        public String getEmoji() { return emoji; }
        public String getDescription() { return description; }
        
        public boolean isHealthy() { return this == HEALTHY; }
        public boolean isOperational() { return this == HEALTHY || this == WARNING; }
        public boolean isCritical() { return this == FAILED; }
    }
    
    /**
     * 헬스 상세 정보
     */
    public static class HealthDetails {
        private final boolean canAcceptRequests;
        private final double cpuUsage;
        private final double memoryUsage;
        private final int activeConnections;
        private final long lastSuccessfulRequest;
        private final String[] diagnosticMessages;
        
        public HealthDetails(boolean canAcceptRequests, double cpuUsage, double memoryUsage,
                           int activeConnections, long lastSuccessfulRequest, String[] diagnosticMessages) {
            this.canAcceptRequests = canAcceptRequests;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.activeConnections = activeConnections;
            this.lastSuccessfulRequest = lastSuccessfulRequest;
            this.diagnosticMessages = diagnosticMessages;
        }
        
        public static HealthDetails createHealthy() {
            return new HealthDetails(true, 45.0, 60.0, 5, System.currentTimeMillis(),
                                   new String[]{"All systems operational", "Performance within normal range"});
        }
        
        public static HealthDetails createDegraded(String reason) {
            return new HealthDetails(true, 75.0, 80.0, 15, System.currentTimeMillis() - 30000,
                                   new String[]{"Performance degraded", reason});
        }
        
        public static HealthDetails createFailed(String error) {
            return new HealthDetails(false, 95.0, 95.0, 0, System.currentTimeMillis() - 300000,
                                   new String[]{"Critical failure detected", error});
        }
        
        public static HealthDetails createWarning(String warning) {
            return new HealthDetails(true, 65.0, 70.0, 10, System.currentTimeMillis() - 10000,
                                   new String[]{"Warning condition detected", warning});
        }
        
        // Getters
        public boolean canAcceptRequests() { return canAcceptRequests; }
        public double getCpuUsage() { return cpuUsage; }
        public double getMemoryUsage() { return memoryUsage; }
        public int getActiveConnections() { return activeConnections; }
        public long getLastSuccessfulRequest() { return lastSuccessfulRequest; }
        public String[] getDiagnosticMessages() { return diagnosticMessages; }
    }
    
    /**
     * 상태 요약을 포맷합니다
     */
    public String formatSummary() {
        return String.format(
            "%s %s Lab [%s]\n" +
            "Status: %s\n" +
            "Message: %s\n" +
            "Check Time: %s\n" +
            "Can Accept Requests: %s\n" +
            "Resource Usage: CPU %.1f%% | Memory %.1f%%\n" +
            "Active Connections: %d",
            healthLevel.getEmoji(), labName, labId,
            healthLevel.getDisplayName(),
            message,
            checkTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            details.canAcceptRequests() ? "Yes" : "No",
            details.getCpuUsage(), details.getMemoryUsage(),
            details.getActiveConnections()
        );
    }
    
    /**
     * JSON 형태로 변환합니다
     */
    public String toJson() {
        return String.format("""
            {
                "labId": "%s",
                "labName": "%s",
                "healthLevel": "%s",
                "healthScore": %d,
                "message": "%s",
                "checkTime": "%s",
                "responseTimeMs": %d,
                "details": {
                    "canAcceptRequests": %s,
                    "cpuUsage": %.2f,
                    "memoryUsage": %.2f,
                    "activeConnections": %d,
                    "lastSuccessfulRequest": %d,
                    "diagnosticMessages": [%s]
                }
            }""",
            labId, labName,
            healthLevel.name(), healthLevel.getScore(),
            message,
            checkTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            responseTimeMs,
            details.canAcceptRequests(),
            details.getCpuUsage(), details.getMemoryUsage(),
            details.getActiveConnections(),
            details.getLastSuccessfulRequest(),
            String.join("\", \"", details.getDiagnosticMessages())
        );
    }
    
    /**
     * 다른 헬스 상태와 비교합니다
     */
    public boolean isBetterThan(LabHealthStatus other) {
        return this.healthLevel.getScore() > other.healthLevel.getScore();
    }
    
    /**
     * 헬스 상태가 악화되었는지 확인합니다
     */
    public boolean isWorseThan(LabHealthStatus previous) {
        return this.healthLevel.getScore() < previous.healthLevel.getScore();
    }
    
    // ==================== Getters ====================
    
    public String getLabId() { return labId; }
    public String getLabName() { return labName; }
    public HealthLevel getHealthLevel() { return healthLevel; }
    public String getMessage() { return message; }
    public LocalDateTime getCheckTime() { return checkTime; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public HealthDetails getDetails() { return details; }
    
    public boolean isHealthy() { return healthLevel.isHealthy(); }
    public boolean isOperational() { return healthLevel.isOperational(); }
    public boolean isCritical() { return healthLevel.isCritical(); }
} 