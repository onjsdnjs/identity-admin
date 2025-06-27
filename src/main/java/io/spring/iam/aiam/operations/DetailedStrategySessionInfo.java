package io.spring.iam.aiam.operations;

import io.spring.iam.aiam.session.AIStrategySessionState;
import io.spring.iam.aiam.session.AILabAllocation;
import io.spring.iam.aiam.session.AIExecutionResult;

/**
 * 상세 전략 세션 정보를 나타내는 데이터 클래스
 */
public class DetailedStrategySessionInfo {
    private final String sessionId;
    private final AIStrategySessionState sessionState;
    private final AILabAllocation labAllocation;
    private final AIExecutionResult executionResult;
    private final long retrievalTime;
    private final String status;
    private final String errorMessage;

    public DetailedStrategySessionInfo(String sessionId, AIStrategySessionState sessionState,
                                     AILabAllocation labAllocation,
                                     AIExecutionResult executionResult, long retrievalTime) {
        this.sessionId = sessionId;
        this.sessionState = sessionState;
        this.labAllocation = labAllocation;
        this.executionResult = executionResult;
        this.retrievalTime = retrievalTime;
        this.status = "SUCCESS";
        this.errorMessage = null;
    }

    private DetailedStrategySessionInfo(String sessionId, String status, String errorMessage) {
        this.sessionId = sessionId;
        this.sessionState = null;
        this.labAllocation = null;
        this.executionResult = null;
        this.retrievalTime = System.currentTimeMillis();
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public static DetailedStrategySessionInfo notFound(String sessionId) {
        return new DetailedStrategySessionInfo(sessionId, "NOT_FOUND", "Session not found: " + sessionId);
    }

    public static DetailedStrategySessionInfo error(String sessionId, String errorMessage) {
        return new DetailedStrategySessionInfo(sessionId, "ERROR", errorMessage);
    }

    public String getSessionId() { return sessionId; }
    public AIStrategySessionState getSessionState() { return sessionState; }
    public AILabAllocation getLabAllocation() { return labAllocation; }
    public AIExecutionResult getExecutionResult() { return executionResult; }
    public long getRetrievalTime() { return retrievalTime; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }

    public boolean isActive() {
        return sessionState != null && sessionState.isActive();
    }
} 