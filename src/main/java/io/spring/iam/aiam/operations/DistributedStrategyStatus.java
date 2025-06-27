package io.spring.iam.aiam.operations;

/**
 * 분산 전략 실행 상태를 나타내는 데이터 클래스
 */
public class DistributedStrategyStatus {
    private final String nodeId;
    private final int totalActiveSessions;
    private final int currentNodeSessions;
    private final long totalExecutions;
    private final long successfulExecutions;
    private final long failedExecutions;
    private final double successRate;
    private final long timestamp;
    private final String status;
    private final String errorMessage;

    public DistributedStrategyStatus(String nodeId, int totalActiveSessions, int currentNodeSessions,
                                   long totalExecutions, long successfulExecutions, long failedExecutions,
                                   double successRate, long timestamp) {
        this.nodeId = nodeId;
        this.totalActiveSessions = totalActiveSessions;
        this.currentNodeSessions = currentNodeSessions;
        this.totalExecutions = totalExecutions;
        this.successfulExecutions = successfulExecutions;
        this.failedExecutions = failedExecutions;
        this.successRate = successRate;
        this.timestamp = timestamp;
        this.status = "SUCCESS";
        this.errorMessage = null;
    }

    private DistributedStrategyStatus(String errorMessage) {
        this.nodeId = null;
        this.totalActiveSessions = 0;
        this.currentNodeSessions = 0;
        this.totalExecutions = 0;
        this.successfulExecutions = 0;
        this.failedExecutions = 0;
        this.successRate = 0.0;
        this.timestamp = System.currentTimeMillis();
        this.status = "ERROR";
        this.errorMessage = errorMessage;
    }

    public static DistributedStrategyStatus error(String errorMessage) {
        return new DistributedStrategyStatus(errorMessage);
    }

    public String getNodeId() { return nodeId; }
    public int getTotalActiveSessions() { return totalActiveSessions; }
    public int getCurrentNodeSessions() { return currentNodeSessions; }
    public long getTotalExecutions() { return totalExecutions; }
    public long getSuccessfulExecutions() { return successfulExecutions; }
    public long getFailedExecutions() { return failedExecutions; }
    public double getSuccessRate() { return successRate; }
    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }

    public boolean isHealthy() {
        return "SUCCESS".equals(status) && successRate >= 0.95;
    }
} 