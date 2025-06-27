package io.spring.iam.aiam.session;

import io.spring.iam.aiam.operations.LabExecutionStrategy;
import io.spring.session.MfaSessionRepository;
import io.spring.session.MfaSessionRepository.SessionStats;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

/**
 * AI 전략 세션 관리를 위한 확장된 저장소 인터페이스
 * 
 * 🧠 AI 전략 실행에 특화된 세션 관리
 * - 전략 실행 상태 추적
 * - 연구소 할당 정보 관리  
 * - 분산 환경에서의 세션 동기화
 * - AI 실행 메트릭 수집
 */
public interface AIStrategySessionRepository extends MfaSessionRepository {
    
    // ==================== AI 전략 세션 전용 메서드 ====================
    
    /**
     * AI 전략 실행 세션을 생성합니다
     */
    String createStrategySession(LabExecutionStrategy strategy, 
                               Map<String, Object> context,
                               HttpServletRequest request, 
                               HttpServletResponse response);
    
    /**
     * 전략 실행 상태를 업데이트합니다
     */
    void updateStrategyState(String sessionId, 
                           AIStrategyExecutionPhase phase, 
                           Map<String, Object> phaseData);
    
    /**
     * 전략 실행 단계를 업데이트합니다 (별칭 메서드)
     */
    default void updateExecutionPhase(String sessionId, 
                                    AIStrategyExecutionPhase phase, 
                                    Map<String, Object> phaseData) {
        updateStrategyState(sessionId, phase, phaseData);
    }
    
    /**
     * 전략 실행 상태를 조회합니다
     */
    AIStrategySessionState getStrategyState(String sessionId);
    
    /**
     * 연구소 할당 정보를 저장합니다
     */
    void storeLabAllocation(String sessionId, 
                          String labType, 
                          String nodeId, 
                          Map<String, Object> allocation);
    
    /**
     * 연구소 할당 정보를 조회합니다
     */
    AILabAllocation getLabAllocation(String sessionId);
    
    /**
     * 전략 실행 메트릭을 기록합니다
     */
    void recordExecutionMetrics(String sessionId, 
                              AIExecutionMetrics metrics);
    
    /**
     * 실행 중인 전략 세션 목록을 조회합니다
     */
    List<String> getActiveStrategySessions();
    
    /**
     * 특정 노드의 실행 중인 세션을 조회합니다
     */
    List<String> getActiveSessionsByNode(String nodeId);
    
    /**
     * 전략 세션을 다른 노드로 이전합니다
     */
    boolean migrateStrategySession(String sessionId, 
                                 String fromNodeId, 
                                 String toNodeId);
    
    /**
     * 전략 실행 결과를 저장합니다
     */
    void storeExecutionResult(String sessionId, 
                            AIExecutionResult result);
    
    /**
     * 전략 실행 결과를 조회합니다
     */
    AIExecutionResult getExecutionResult(String sessionId);
    
    /**
     * 분산 환경에서의 세션 동기화
     */
    void syncSessionAcrossNodes(String sessionId);
    
    /**
     * AI 전략 세션 통계 조회
     */
    AIStrategySessionStats getAIStrategyStats();
    
    // ==================== 내부 클래스 정의 ====================
    
    /**
     * AI 전략 실행 단계
     */
    enum AIStrategyExecutionPhase {
        INITIALIZED("초기화됨"),
        PLANNING("계획 수립 중"),
        LAB_ALLOCATION("연구소 할당 중"),
        EXECUTING("실행 중"),
        VALIDATING("검증 중"),
        COMPLETED("완료됨"),
        FAILED("실패함"),
        CANCELLED("취소됨");
        
        private final String description;
        
        AIStrategyExecutionPhase(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * AI 전략 세션 상태
     */
    class AIStrategySessionState {
        private final String sessionId;
        private final String strategyId;
        private final AIStrategyExecutionPhase phase;
        private final String nodeId;
        private final long createTime;
        private final long lastUpdateTime;
        private final Map<String, Object> context;
        private final Map<String, Object> phaseData;
        
        public AIStrategySessionState(String sessionId, String strategyId, 
                                    AIStrategyExecutionPhase phase, String nodeId,
                                    long createTime, long lastUpdateTime,
                                    Map<String, Object> context, 
                                    Map<String, Object> phaseData) {
            this.sessionId = sessionId;
            this.strategyId = strategyId;
            this.phase = phase;
            this.nodeId = nodeId;
            this.createTime = createTime;
            this.lastUpdateTime = lastUpdateTime;
            this.context = context;
            this.phaseData = phaseData;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getStrategyId() { return strategyId; }
        public AIStrategyExecutionPhase getPhase() { return phase; }
        public String getNodeId() { return nodeId; }
        public long getCreateTime() { return createTime; }
        public long getLastUpdateTime() { return lastUpdateTime; }
        public Map<String, Object> getContext() { return context; }
        public Map<String, Object> getPhaseData() { return phaseData; }
        
        public boolean isActive() {
            return phase != AIStrategyExecutionPhase.COMPLETED && 
                   phase != AIStrategyExecutionPhase.FAILED && 
                   phase != AIStrategyExecutionPhase.CANCELLED;
        }
        
        public Duration getExecutionDuration() {
            return Duration.ofMillis(lastUpdateTime - createTime);
        }
    }
    
    /**
     * AI 연구소 할당 정보
     */
    class AILabAllocation {
        private final String sessionId;
        private final String labType;
        private final String nodeId;
        private final Map<String, Object> allocationData;
        private final long allocationTime;
        
        public AILabAllocation(String sessionId, String labType, String nodeId,
                             Map<String, Object> allocationData, long allocationTime) {
            this.sessionId = sessionId;
            this.labType = labType;
            this.nodeId = nodeId;
            this.allocationData = allocationData;
            this.allocationTime = allocationTime;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getLabType() { return labType; }
        public String getNodeId() { return nodeId; }
        public Map<String, Object> getAllocationData() { return allocationData; }
        public long getAllocationTime() { return allocationTime; }
    }
    
    /**
     * AI 실행 메트릭
     */
    class AIExecutionMetrics {
        private final String sessionId;
        private final long startTime;
        private final long endTime;
        private final long processingTime;
        private final int requestCount;
        private final int successCount;
        private final int errorCount;
        private final Map<String, Object> customMetrics;
        
        public AIExecutionMetrics(String sessionId, long startTime, long endTime,
                                long processingTime, int requestCount, int successCount,
                                int errorCount, Map<String, Object> customMetrics) {
            this.sessionId = sessionId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.processingTime = processingTime;
            this.requestCount = requestCount;
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.customMetrics = customMetrics;
        }
        
        /**
         * Builder 패턴을 위한 정적 메서드
         */
        public static AIExecutionMetricsBuilder builder() {
            return new AIExecutionMetricsBuilder();
        }
        
        /**
         * Builder 클래스
         */
        public static class AIExecutionMetricsBuilder {
            private String sessionId;
            private long startTime = System.currentTimeMillis();
            private long endTime = System.currentTimeMillis();
            private long processingTime = 0;
            private int requestCount = 1;
            private int successCount = 0;
            private int errorCount = 0;
            private Map<String, Object> customMetrics = new HashMap<>();
            
            public AIExecutionMetricsBuilder sessionId(String sessionId) {
                this.sessionId = sessionId;
                return this;
            }
            
            public AIExecutionMetricsBuilder startTime(long startTime) {
                this.startTime = startTime;
                return this;
            }
            
            public AIExecutionMetricsBuilder endTime(long endTime) {
                this.endTime = endTime;
                return this;
            }
            
            public AIExecutionMetricsBuilder processingTime(long processingTime) {
                this.processingTime = processingTime;
                return this;
            }
            
            public AIExecutionMetricsBuilder requestCount(int requestCount) {
                this.requestCount = requestCount;
                return this;
            }
            
            public AIExecutionMetricsBuilder successCount(int successCount) {
                this.successCount = successCount;
                return this;
            }
            
            public AIExecutionMetricsBuilder errorCount(int errorCount) {
                this.errorCount = errorCount;
                return this;
            }
            
            public AIExecutionMetricsBuilder customMetrics(Map<String, Object> customMetrics) {
                this.customMetrics = customMetrics != null ? customMetrics : new HashMap<>();
                return this;
            }
            
            public AIExecutionMetricsBuilder success(boolean success) {
                if (success) {
                    this.successCount = 1;
                    this.errorCount = 0;
                } else {
                    this.successCount = 0;
                    this.errorCount = 1;
                }
                return this;
            }
            
            public AIExecutionMetrics build() {
                if (sessionId == null) {
                    throw new IllegalStateException("sessionId is required");
                }
                return new AIExecutionMetrics(sessionId, startTime, endTime, processingTime,
                                            requestCount, successCount, errorCount, customMetrics);
            }
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public long getProcessingTime() { return processingTime; }
        public int getRequestCount() { return requestCount; }
        public int getSuccessCount() { return successCount; }
        public int getErrorCount() { return errorCount; }
        public Map<String, Object> getCustomMetrics() { return customMetrics; }
        
        public double getSuccessRate() {
            return requestCount > 0 ? (double) successCount / requestCount : 0.0;
        }
        
        public double getErrorRate() {
            return requestCount > 0 ? (double) errorCount / requestCount : 0.0;
        }
    }
    
    /**
     * AI 실행 결과
     */
    class AIExecutionResult {
        private final String sessionId;
        private final boolean success;
        private final Object result;
        private final String errorMessage;
        private final long completionTime;
        private final AIExecutionMetrics metrics;
        
        public AIExecutionResult(String sessionId, boolean success, Object result,
                               String errorMessage, long completionTime, 
                               AIExecutionMetrics metrics) {
            this.sessionId = sessionId;
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
            this.completionTime = completionTime;
            this.metrics = metrics;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public boolean isSuccess() { return success; }
        public Object getResult() { return result; }
        public String getErrorMessage() { return errorMessage; }
        public long getCompletionTime() { return completionTime; }
        public AIExecutionMetrics getMetrics() { return metrics; }
        
        public static AIExecutionResult success(String sessionId, Object result, 
                                              long completionTime, AIExecutionMetrics metrics) {
            return new AIExecutionResult(sessionId, true, result, null, completionTime, metrics);
        }
        
        public static AIExecutionResult failure(String sessionId, String errorMessage, 
                                              long completionTime, AIExecutionMetrics metrics) {
            return new AIExecutionResult(sessionId, false, null, errorMessage, completionTime, metrics);
        }
    }
    
    /**
     * AI 전략 세션 통계
     */
    class AIStrategySessionStats extends SessionStats {
        private final long activeStrategySessions;
        private final long completedStrategySessions;
        private final long failedStrategySessions;
        private final double averageExecutionTime;
        private final Map<String, Long> labTypeDistribution;
        private final Map<String, Long> nodeDistribution;
        
        public AIStrategySessionStats(long activeSessions, long totalSessionsCreated,
                                    long sessionCollisions, double averageSessionDuration,
                                    String repositoryType, long activeStrategySessions,
                                    long completedStrategySessions, long failedStrategySessions,
                                    double averageExecutionTime, Map<String, Long> labTypeDistribution,
                                    Map<String, Long> nodeDistribution) {
            super(activeSessions, totalSessionsCreated, sessionCollisions, 
                  averageSessionDuration, repositoryType);
            this.activeStrategySessions = activeStrategySessions;
            this.completedStrategySessions = completedStrategySessions;
            this.failedStrategySessions = failedStrategySessions;
            this.averageExecutionTime = averageExecutionTime;
            this.labTypeDistribution = labTypeDistribution;
            this.nodeDistribution = nodeDistribution;
        }
        
        // Getters
        public long getActiveStrategySessions() { return activeStrategySessions; }
        public long getCompletedStrategySessions() { return completedStrategySessions; }
        public long getFailedStrategySessions() { return failedStrategySessions; }
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public Map<String, Long> getLabTypeDistribution() { return labTypeDistribution; }
        public Map<String, Long> getNodeDistribution() { return nodeDistribution; }
        
        public double getStrategySuccessRate() {
            long total = completedStrategySessions + failedStrategySessions;
            return total > 0 ? (double) completedStrategySessions / total : 0.0;
        }
    }
}