package io.spring.iam.aiam.operations;

import java.util.List;

/**
 * 세션 정리 결과를 나타내는 데이터 클래스
 */
public class CleanupResult {
    private final List<String> cleanedSessions;
    private final List<String> failedCleanups;
    private final long cleanupTime;
    private final String status;
    private final String errorMessage;

    public CleanupResult(List<String> cleanedSessions, List<String> failedCleanups, long cleanupTime) {
        this.cleanedSessions = cleanedSessions;
        this.failedCleanups = failedCleanups;
        this.cleanupTime = cleanupTime;
        this.status = "SUCCESS";
        this.errorMessage = null;
    }

    private CleanupResult(String errorMessage) {
        this.cleanedSessions = List.of();
        this.failedCleanups = List.of();
        this.cleanupTime = System.currentTimeMillis();
        this.status = "ERROR";
        this.errorMessage = errorMessage;
    }

    public static CleanupResult error(String errorMessage) {
        return new CleanupResult(errorMessage);
    }

    public List<String> getCleanedSessions() { return cleanedSessions; }
    public List<String> getFailedCleanups() { return failedCleanups; }
    public long getCleanupTime() { return cleanupTime; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }

    public int getTotalCleaned() { return cleanedSessions.size(); }
    public int getTotalFailed() { return failedCleanups.size(); }

    public boolean isSuccessful() {
        return "SUCCESS".equals(status);
    }
} 