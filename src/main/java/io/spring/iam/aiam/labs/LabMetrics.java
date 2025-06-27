package io.spring.iam.aiam.labs;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 🔬 IAM 연구소 성능 메트릭
 * 
 * 연구소의 실시간 성능 지표와 통계 정보를 제공
 */
public class LabMetrics {
    
    // ==================== 기본 식별 정보 ====================
    private final String labId;
    private final String labName;
    private final LabSpecialization specialization;
    
    // ==================== 요청 처리 메트릭 ====================
    private final long totalRequests;
    private final long successfulRequests;
    private final long failedRequests;
    private final double successRate; // 성공률 (%)
    
    // ==================== 성능 메트릭 ====================
    private final double averageResponseTime; // 평균 응답 시간 (ms)
    private final double throughput; // 처리량 (requests/minute)
    private final double currentLoad; // 현재 부하율 (%)
    
    // ==================== 상태 정보 ====================
    private final LabStatus currentStatus;
    private final long lastUpdated; // 마지막 업데이트 시간
    
    // ==================== 추가 성능 지표 ====================
    private final double cpuUsage; // CPU 사용률 (%)
    private final double memoryUsage; // 메모리 사용률 (%)
    private final long activeConnections; // 활성 연결 수
    private final double errorRate; // 오류율 (%)
    
    public LabMetrics(String labId, String labName, LabSpecialization specialization,
                     long totalRequests, long successfulRequests, long failedRequests,
                     double successRate, double averageResponseTime, double throughput,
                     double currentLoad, LabStatus currentStatus, long lastUpdated) {
        this(labId, labName, specialization, totalRequests, successfulRequests, failedRequests,
             successRate, averageResponseTime, throughput, currentLoad, currentStatus, lastUpdated,
             0.0, 0.0, 0, 0.0);
    }
    
    public LabMetrics(String labId, String labName, LabSpecialization specialization,
                     long totalRequests, long successfulRequests, long failedRequests,
                     double successRate, double averageResponseTime, double throughput,
                     double currentLoad, LabStatus currentStatus, long lastUpdated,
                     double cpuUsage, double memoryUsage, long activeConnections, double errorRate) {
        this.labId = labId;
        this.labName = labName;
        this.specialization = specialization;
        this.totalRequests = totalRequests;
        this.successfulRequests = successfulRequests;
        this.failedRequests = failedRequests;
        this.successRate = successRate;
        this.averageResponseTime = averageResponseTime;
        this.throughput = throughput;
        this.currentLoad = currentLoad;
        this.currentStatus = currentStatus;
        this.lastUpdated = lastUpdated;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.activeConnections = activeConnections;
        this.errorRate = errorRate;
    }
    
    /**
     * 성능 점수를 계산합니다 (0.0 ~ 100.0)
     */
    public double calculatePerformanceScore() {
        // 성공률 (30%) + 응답시간 (25%) + 처리량 (20%) + 안정성 (15%) + 리소스 효율성 (10%)
        double successScore = successRate;
        double responseTimeScore = Math.max(0, 100 - (averageResponseTime / 100.0)); // 10초 기준으로 정규화
        double throughputScore = Math.min(100, throughput * 2); // 50 req/min 기준으로 정규화
        double stabilityScore = Math.max(0, 100 - (errorRate * 10)); // 10% 오류율 기준
        double resourceScore = Math.max(0, 100 - Math.max(cpuUsage, memoryUsage)); // 리소스 사용률 기준
        
        return (successScore * 0.30) + 
               (responseTimeScore * 0.25) + 
               (throughputScore * 0.20) + 
               (stabilityScore * 0.15) + 
               (resourceScore * 0.10);
    }
    
    /**
     * 연구소가 고성능 상태인지 확인합니다
     */
    public boolean isHighPerformance() {
        return calculatePerformanceScore() >= 80.0 &&
               successRate >= 95.0 &&
               averageResponseTime <= 2000 &&
               currentLoad <= 70.0;
    }
    
    /**
     * 연구소가 과부하 상태인지 확인합니다
     */
    public boolean isOverloaded() {
        return currentLoad >= 90.0 ||
               averageResponseTime >= 10000 ||
               errorRate >= 5.0 ||
               cpuUsage >= 90.0 ||
               memoryUsage >= 90.0;
    }
    
    /**
     * 연구소가 안정적인 상태인지 확인합니다
     */
    public boolean isStable() {
        return successRate >= 90.0 &&
               errorRate <= 2.0 &&
               currentLoad <= 80.0 &&
               currentStatus == LabStatus.OPERATIONAL;
    }
    
    /**
     * 예상 처리 시간을 계산합니다
     */
    public long estimateProcessingTime(int requestCount) {
        if (throughput <= 0) return -1;
        
        double estimatedMinutes = requestCount / throughput;
        return Math.round(estimatedMinutes * 60 * 1000); // 밀리초로 변환
    }
    
    /**
     * 추가 요청을 처리할 수 있는 여유 용량을 계산합니다
     */
    public int calculateAvailableCapacity() {
        if (currentLoad >= 100.0) return 0;
        
        double availableLoadPercentage = 100.0 - currentLoad;
        return (int) Math.round(availableLoadPercentage / 2.0); // 요청당 2% 부하 가정
    }
    
    /**
     * 메트릭을 사람이 읽기 쉬운 형태로 포맷합니다
     */
    public String formatSummary() {
        return String.format(
            "🔬 %s Lab [%s]\n" +
            "📊 Performance Score: %.1f/100\n" +
            "✅ Success Rate: %.1f%% (%d/%d)\n" +
            "⚡ Avg Response: %.0fms | Throughput: %.1f req/min\n" +
            "📈 Load: %.1f%% | Status: %s\n" +
            "💻 CPU: %.1f%% | Memory: %.1f%% | Connections: %d\n" +
            "🕒 Last Updated: %s",
            labName, specialization.getDisplayName(),
            calculatePerformanceScore(),
            successRate, successfulRequests, totalRequests,
            averageResponseTime, throughput,
            currentLoad, currentStatus.getDisplayName(),
            cpuUsage, memoryUsage, activeConnections,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }
    
    /**
     * 메트릭을 JSON 형태로 변환합니다
     */
    public String toJson() {
        return String.format("""
            {
                "labId": "%s",
                "labName": "%s",
                "specialization": "%s",
                "performanceScore": %.2f,
                "totalRequests": %d,
                "successfulRequests": %d,
                "failedRequests": %d,
                "successRate": %.2f,
                "averageResponseTime": %.2f,
                "throughput": %.2f,
                "currentLoad": %.2f,
                "status": "%s",
                "cpuUsage": %.2f,
                "memoryUsage": %.2f,
                "activeConnections": %d,
                "errorRate": %.2f,
                "isHighPerformance": %s,
                "isOverloaded": %s,
                "isStable": %s,
                "availableCapacity": %d,
                "lastUpdated": %d
            }""",
            labId, labName, specialization.name(),
            calculatePerformanceScore(),
            totalRequests, successfulRequests, failedRequests,
            successRate, averageResponseTime, throughput, currentLoad,
            currentStatus.name(),
            cpuUsage, memoryUsage, activeConnections, errorRate,
            isHighPerformance(), isOverloaded(), isStable(),
            calculateAvailableCapacity(), lastUpdated
        );
    }
    
    /**
     * 다른 연구소와 성능을 비교합니다
     */
    public MetricsComparison compareWith(LabMetrics other) {
        return new MetricsComparison(this, other);
    }
    
    /**
     * 메트릭 비교 결과
     */
    public static class MetricsComparison {
        private final LabMetrics current;
        private final LabMetrics other;
        
        public MetricsComparison(LabMetrics current, LabMetrics other) {
            this.current = current;
            this.other = other;
        }
        
        public boolean isBetterThan() {
            return current.calculatePerformanceScore() > other.calculatePerformanceScore();
        }
        
        public double getPerformanceDifference() {
            return current.calculatePerformanceScore() - other.calculatePerformanceScore();
        }
        
        public String getSummary() {
            double diff = getPerformanceDifference();
            String comparison = diff > 0 ? "better" : "worse";
            return String.format("%s is %.1f points %s than %s", 
                               current.labName, Math.abs(diff), comparison, other.labName);
        }
    }
    
    // ==================== Getters ====================
    
    public String getLabId() { return labId; }
    public String getLabName() { return labName; }
    public LabSpecialization getSpecialization() { return specialization; }
    public long getTotalRequests() { return totalRequests; }
    public long getSuccessfulRequests() { return successfulRequests; }
    public long getFailedRequests() { return failedRequests; }
    public double getSuccessRate() { return successRate; }
    public double getAverageResponseTime() { return averageResponseTime; }
    public double getThroughput() { return throughput; }
    public double getCurrentLoad() { return currentLoad; }
    public LabStatus getCurrentStatus() { return currentStatus; }
    public long getLastUpdated() { return lastUpdated; }
    public double getCpuUsage() { return cpuUsage; }
    public double getMemoryUsage() { return memoryUsage; }
    public long getActiveConnections() { return activeConnections; }
    public double getErrorRate() { return errorRate; }
} 