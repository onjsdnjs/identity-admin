package io.spring.iam.aiam.operations;

import java.util.Map;

/**
 * 분산 메트릭 리포트를 나타내는 데이터 클래스
 */
public class DistributedMetricsReport {
    private final String nodeId;
    private final long totalExecutions;
    private final long successfulExecutions;
    private final long failedExecutions;
    private final double successRate;
    private final double failureRate;
    private final int totalActiveSessions;
    private final int currentNodeSessions;
    private final double averageExecutionTime;
    private final Map<String, Long> labTypeDistribution;
    private final Map<String, Long> nodeDistribution;
    private final long reportTime;
    private final String status;
    private final String errorMessage;

    public DistributedMetricsReport(String nodeId, long totalExecutions, long successfulExecutions,
                                  long failedExecutions, double successRate, double failureRate,
                                  int totalActiveSessions, int currentNodeSessions,
                                  double averageExecutionTime, Map<String, Long> labTypeDistribution,
                                  Map<String, Long> nodeDistribution, long reportTime) {
        this.nodeId = nodeId;
        this.totalExecutions = totalExecutions;
        this.successfulExecutions = successfulExecutions;
        this.failedExecutions = failedExecutions;
        this.successRate = successRate;
        this.failureRate = failureRate;
        this.totalActiveSessions = totalActiveSessions;
        this.currentNodeSessions = currentNodeSessions;
        this.averageExecutionTime = averageExecutionTime;
        this.labTypeDistribution = labTypeDistribution;
        this.nodeDistribution = nodeDistribution;
        this.reportTime = reportTime;
        this.status = "SUCCESS";
        this.errorMessage = null;
    }

    private DistributedMetricsReport(String errorMessage) {
        this.nodeId = null;
        this.totalExecutions = 0;
        this.successfulExecutions = 0;
        this.failedExecutions = 0;
        this.successRate = 0.0;
        this.failureRate = 0.0;
        this.totalActiveSessions = 0;
        this.currentNodeSessions = 0;
        this.averageExecutionTime = 0.0;
        this.labTypeDistribution = Map.of();
        this.nodeDistribution = Map.of();
        this.reportTime = System.currentTimeMillis();
        this.status = "ERROR";
        this.errorMessage = errorMessage;
    }

    public static DistributedMetricsReport error(String errorMessage) {
        return new DistributedMetricsReport(errorMessage);
    }

    public String getNodeId() { return nodeId; }
    public long getTotalExecutions() { return totalExecutions; }
    public long getSuccessfulExecutions() { return successfulExecutions; }
    public long getFailedExecutions() { return failedExecutions; }
    public double getSuccessRate() { return successRate; }
    public double getFailureRate() { return failureRate; }
    public int getTotalActiveSessions() { return totalActiveSessions; }
    public int getCurrentNodeSessions() { return currentNodeSessions; }
    public double getAverageExecutionTime() { return averageExecutionTime; }
    public Map<String, Long> getLabTypeDistribution() { return labTypeDistribution; }
    public Map<String, Long> getNodeDistribution() { return nodeDistribution; }
    public long getReportTime() { return reportTime; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }

    public boolean isHealthy() {
        return "SUCCESS".equals(status) && successRate >= 0.90;
    }
} 