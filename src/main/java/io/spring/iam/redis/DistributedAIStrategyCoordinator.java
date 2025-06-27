package io.spring.iam.redis;

import io.spring.iam.aiam.operations.LabExecutionStrategy;
import io.spring.redis.RedisDistributedLockService;
import io.spring.redis.RedisEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 기반 분산 AI 전략 코디네이터
 * 
 * 🌐 다중 노드 환경에서 AI 전략 실행을 조율
 * - 전략 실행 상태 분산 관리
 * - 연구소 자원 할당 조정
 * - 노드 간 부하 분산
 * - 장애 복구 및 페일오버
 */
@Slf4j
@Service
public class DistributedAIStrategyCoordinator {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisDistributedLockService lockService;
    private final RedisEventPublisher eventPublisher;
    private final String nodeId;
    
    // Redis 키 패턴
    private static final String STRATEGY_STATE_KEY = "ai:strategy:state:%s";
    private static final String LAB_ALLOCATION_KEY = "ai:lab:allocation:%s";
    private static final String NODE_HEALTH_KEY = "ai:node:health:%s";
    private static final String EXECUTION_QUEUE_KEY = "ai:execution:queue";
    private static final String METRICS_KEY = "ai:metrics:%s";
    
    // 분산 상태 캐시
    private final Map<String, StrategyExecutionState> localStateCache = new ConcurrentHashMap<>();
    
    public DistributedAIStrategyCoordinator(RedisTemplate<String, Object> redisTemplate,
                                          RedisDistributedLockService lockService,
                                          RedisEventPublisher eventPublisher) {
        this.redisTemplate = redisTemplate;
        this.lockService = lockService;
        this.eventPublisher = eventPublisher;
        this.nodeId = generateNodeId();
        
        // 노드 등록 및 헬스체크 시작
        registerNode();
        startHealthCheck();
    }
    
    /**
     * 분산 환경에서 전략 실행을 시작합니다
     */
    public DistributedExecutionResult executeStrategy(LabExecutionStrategy strategy, 
                                                     Map<String, Object> context) {
        String executionId = UUID.randomUUID().toString();
        String lockKey = "strategy:lock:" + strategy.getStrategyId();
        
        try {
            // 1. 분산 락 획득 (전략 실행 중복 방지)
            boolean lockAcquired = lockService.tryLock(lockKey, nodeId, Duration.ofMinutes(10));
            if (!lockAcquired) {
                return DistributedExecutionResult.alreadyExecuting(strategy.getStrategyId());
            }
            
            // 2. 실행 상태 초기화
            StrategyExecutionState state = initializeExecutionState(executionId, strategy, context);
            saveExecutionState(state);
            
            // 3. 최적 노드 선택 및 연구소 할당
            NodeAllocationResult allocation = allocateOptimalResources(strategy);
            if (!allocation.isSuccessful()) {
                return DistributedExecutionResult.resourceUnavailable(allocation.getReason());
            }
            
            // 4. 실행 큐에 추가
            queueExecution(executionId, allocation);
            
            // 5. 다른 노드들에게 실행 시작 알림
            broadcastExecutionStart(executionId, strategy, allocation);
            
            return DistributedExecutionResult.success(executionId, allocation);
            
        } catch (Exception e) {
            // 실패 시 락 해제
            lockService.unlock(lockKey, nodeId);
            return DistributedExecutionResult.error(e.getMessage());
        }
    }
    
    /**
     * 실행 상태를 업데이트합니다
     */
    public void updateExecutionState(String executionId, 
                                   ExecutionPhase phase, 
                                   Map<String, Object> phaseData) {
        String stateKey = String.format(STRATEGY_STATE_KEY, executionId);
        
        try {
            StrategyExecutionState state = getExecutionState(executionId);
            if (state != null) {
                state.updatePhase(phase, phaseData);
                state.setLastUpdateTime(System.currentTimeMillis());
                state.setUpdatedByNode(nodeId);
                
                // Redis에 상태 저장
                redisTemplate.opsForValue().set(stateKey, state, Duration.ofHours(24));
                
                // 로컬 캐시 업데이트
                localStateCache.put(executionId, state);
                
                // 상태 변경 이벤트 발행 (기존 보안 이벤트 메서드 활용)
                eventPublisher.publishSecurityEvent("STRATEGY_STATE_UPDATED", "system", nodeId, Map.of(
                    "executionId", executionId,
                    "phase", phase.toString(),
                    "nodeId", nodeId,
                    "timestamp", System.currentTimeMillis()
                ));
            }
        } catch (Exception e) {
            // 상태 업데이트 실패 로깅
            log.error("Failed to update execution state: {}", e.getMessage());
        }
    }
    
    /**
     * 연구소 자원 할당을 조정합니다
     */
    public NodeAllocationResult allocateOptimalResources(LabExecutionStrategy strategy) {
        String allocationKey = String.format(LAB_ALLOCATION_KEY, strategy.getOperationType());
        
        try {
            // 현재 연구소 사용률 조회
            Map<String, Double> labUtilization = getLabUtilization();
            
            // 노드별 성능 메트릭 조회
            Map<String, NodeMetrics> nodeMetrics = getNodeMetrics();
            
            // 최적 할당 계산
            OptimalAllocation allocation = calculateOptimalAllocation(
                strategy, labUtilization, nodeMetrics);
            
            if (allocation.isValid()) {
                // 할당 정보를 Redis에 저장
                redisTemplate.opsForValue().set(
                    allocationKey, allocation, Duration.ofMinutes(30));
                
                return NodeAllocationResult.success(allocation);
            } else {
                return NodeAllocationResult.failure("No optimal allocation found");
            }
            
        } catch (Exception e) {
            return NodeAllocationResult.failure("Allocation calculation failed: " + e.getMessage());
        }
    }
    
    /**
     * 분산 메트릭을 집계합니다
     */
    public DistributedMetrics aggregateMetrics() {
        try {
            // 모든 노드의 메트릭 수집
            Map<String, NodeMetrics> allNodeMetrics = getAllNodeMetrics();
            
            // 전체 시스템 메트릭 계산
            long totalRequests = allNodeMetrics.values().stream()
                    .mapToLong(NodeMetrics::getTotalRequests)
                    .sum();
            
            long totalSuccesses = allNodeMetrics.values().stream()
                    .mapToLong(NodeMetrics::getSuccessfulRequests)
                    .sum();
            
            double avgResponseTime = allNodeMetrics.values().stream()
                    .mapToDouble(NodeMetrics::getAverageResponseTime)
                    .average()
                    .orElse(0.0);
            
            return new DistributedMetrics(
                totalRequests,
                totalSuccesses,
                avgResponseTime,
                allNodeMetrics.size(),
                System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            return DistributedMetrics.error("Metrics aggregation failed: " + e.getMessage());
        }
    }
    
    /**
     * 노드 장애 감지 및 페일오버
     */
    public void handleNodeFailure(String failedNodeId) {
        try {
            // 1. 실패한 노드의 실행 중인 작업 조회
            List<String> runningExecutions = getRunningExecutionsByNode(failedNodeId);
            
            // 2. 각 실행을 다른 노드로 재할당
            for (String executionId : runningExecutions) {
                redistributeExecution(executionId, failedNodeId);
            }
            
            // 3. 실패한 노드를 클러스터에서 제거
            removeNodeFromCluster(failedNodeId);
            
            // 4. 페일오버 이벤트 발행
            eventPublisher.publishEvent("node.failover", Map.of(
                "failedNodeId", failedNodeId,
                "redistributedExecutions", runningExecutions.size(),
                "handledByNode", nodeId,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            System.err.println("Node failover failed: " + e.getMessage());
        }
    }
    
    // ==================== Private Helper Methods ====================
    
    private String generateNodeId() {
        return "ai-node-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private void registerNode() {
        String healthKey = String.format(NODE_HEALTH_KEY, nodeId);
        NodeHealth health = new NodeHealth(nodeId, System.currentTimeMillis(), "HEALTHY");
        redisTemplate.opsForValue().set(healthKey, health, Duration.ofMinutes(5));
    }
    
    private void startHealthCheck() {
        // 주기적 헬스체크 스케줄링 (실제 구현에서는 @Scheduled 사용)
        // TODO: 실제 스케줄링 구현
    }
    
    private StrategyExecutionState initializeExecutionState(String executionId,
                                                          LabExecutionStrategy strategy,
                                                          Map<String, Object> context) {
        return new StrategyExecutionState(
            executionId,
            strategy.getStrategyId(),
            ExecutionPhase.INITIALIZED,
            nodeId,
            System.currentTimeMillis(),
            context
        );
    }
    
    private void saveExecutionState(StrategyExecutionState state) {
        String stateKey = String.format(STRATEGY_STATE_KEY, state.getExecutionId());
        redisTemplate.opsForValue().set(stateKey, state, Duration.ofHours(24));
        localStateCache.put(state.getExecutionId(), state);
    }
    
    private StrategyExecutionState getExecutionState(String executionId) {
        // 로컬 캐시 먼저 확인
        StrategyExecutionState state = localStateCache.get(executionId);
        if (state != null) {
            return state;
        }
        
        // Redis에서 조회
        String stateKey = String.format(STRATEGY_STATE_KEY, executionId);
        return (StrategyExecutionState) redisTemplate.opsForValue().get(stateKey);
    }
    
    private void queueExecution(String executionId, NodeAllocationResult allocation) {
        ExecutionQueueItem item = new ExecutionQueueItem(executionId, allocation, nodeId);
        redisTemplate.opsForList().leftPush(EXECUTION_QUEUE_KEY, item);
    }
    
    private void broadcastExecutionStart(String executionId, 
                                       LabExecutionStrategy strategy,
                                       NodeAllocationResult allocation) {
        eventPublisher.publishEvent("strategy.execution.started", Map.of(
            "executionId", executionId,
            "strategyId", strategy.getStrategyId(),
            "allocation", allocation,
            "startedByNode", nodeId,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    // TODO: 나머지 헬퍼 메서드들 구현
    private Map<String, Double> getLabUtilization() { return Map.of(); }
    private Map<String, NodeMetrics> getNodeMetrics() { return Map.of(); }
    private OptimalAllocation calculateOptimalAllocation(LabExecutionStrategy strategy, 
                                                        Map<String, Double> labUtilization, 
                                                        Map<String, NodeMetrics> nodeMetrics) { 
        return new OptimalAllocation(); 
    }
    private Map<String, NodeMetrics> getAllNodeMetrics() { return Map.of(); }
    private List<String> getRunningExecutionsByNode(String nodeId) { return List.of(); }
    private void redistributeExecution(String executionId, String failedNodeId) {}
    private void removeNodeFromCluster(String failedNodeId) {}
    
    // ==================== Inner Classes ====================
    
    public enum ExecutionPhase {
        INITIALIZED, PLANNING, EXECUTING, VALIDATING, COMPLETED, FAILED
    }
    
    // TODO: 나머지 내부 클래스들 정의
    public static class StrategyExecutionState {
        private final String executionId;
        private final String strategyId;
        private ExecutionPhase phase;
        private final String createdByNode;
        private String updatedByNode;
        private final long createTime;
        private long lastUpdateTime;
        private final Map<String, Object> context;
        
        public StrategyExecutionState(String executionId, String strategyId, ExecutionPhase phase,
                                    String createdByNode, long createTime, Map<String, Object> context) {
            this.executionId = executionId;
            this.strategyId = strategyId;
            this.phase = phase;
            this.createdByNode = createdByNode;
            this.updatedByNode = createdByNode;
            this.createTime = createTime;
            this.lastUpdateTime = createTime;
            this.context = context;
        }
        
        public void updatePhase(ExecutionPhase newPhase, Map<String, Object> phaseData) {
            this.phase = newPhase;
            if (phaseData != null) {
                this.context.putAll(phaseData);
            }
        }
        
        // Getters and setters
        public String getExecutionId() { return executionId; }
        public String getStrategyId() { return strategyId; }
        public ExecutionPhase getPhase() { return phase; }
        public String getCreatedByNode() { return createdByNode; }
        public String getUpdatedByNode() { return updatedByNode; }
        public void setUpdatedByNode(String updatedByNode) { this.updatedByNode = updatedByNode; }
        public long getCreateTime() { return createTime; }
        public long getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
        public Map<String, Object> getContext() { return context; }
    }
    
    // TODO: 나머지 클래스들 구현
    public static class DistributedExecutionResult {
        private final boolean success;
        private final String executionId;
        private final String strategyId;
        private final String reason;
        private final NodeAllocationResult allocation;
        
        private DistributedExecutionResult(boolean success, String executionId, String strategyId, String reason, NodeAllocationResult allocation) {
            this.success = success;
            this.executionId = executionId;
            this.strategyId = strategyId;
            this.reason = reason;
            this.allocation = allocation;
        }
        
        public static DistributedExecutionResult alreadyExecuting(String strategyId) { 
            return new DistributedExecutionResult(false, null, strategyId, "Already executing", null); 
        }
        
        public static DistributedExecutionResult resourceUnavailable(String reason) { 
            return new DistributedExecutionResult(false, null, null, reason, null); 
        }
        
        public static DistributedExecutionResult success(String executionId, NodeAllocationResult allocation) { 
            return new DistributedExecutionResult(true, executionId, null, null, allocation); 
        }
        
        public static DistributedExecutionResult error(String message) { 
            return new DistributedExecutionResult(false, null, null, message, null); 
        }
        
        // Jackson 직렬화를 위한 getter 메서드들
        public boolean isSuccess() { return success; }
        public String getExecutionId() { return executionId; }
        public String getStrategyId() { return strategyId; }
        public String getReason() { return reason; }
        public NodeAllocationResult getAllocation() { return allocation; }
    }
    
    public static class NodeAllocationResult {
        private final boolean successful;
        private final String reason;
        private final OptimalAllocation allocation;
        
        private NodeAllocationResult(boolean successful, String reason, OptimalAllocation allocation) {
            this.successful = successful;
            this.reason = reason;
            this.allocation = allocation;
        }
        
        public boolean isSuccessful() { return successful; }
        public String getReason() { return reason; }
        
        public static NodeAllocationResult success(OptimalAllocation allocation) { 
            return new NodeAllocationResult(true, null, allocation); 
        }
        
        public static NodeAllocationResult failure(String reason) { 
            return new NodeAllocationResult(false, reason, null); 
        }
        
        // Jackson 직렬화를 위한 getter 메서드들
        public boolean getSuccessful() { return successful; }
        public OptimalAllocation getAllocation() { return allocation; }
    }
    
    public static class OptimalAllocation {
        private final boolean valid;
        private final String allocationId;
        
        public OptimalAllocation() {
            this.valid = false;
            this.allocationId = null;
        }
        
        public OptimalAllocation(boolean valid, String allocationId) {
            this.valid = valid;
            this.allocationId = allocationId;
        }
        
        public boolean isValid() { return valid; }
        
        // Jackson 직렬화를 위한 getter 메서드들
        public boolean getValid() { return valid; }
        public String getAllocationId() { return allocationId; }
    }
    
    public static class NodeMetrics {
        private final long totalRequests;
        private final long successfulRequests;
        private final double averageResponseTime;
        
        public NodeMetrics(long totalRequests, long successfulRequests, double averageResponseTime) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.averageResponseTime = averageResponseTime;
        }
        
        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public double getAverageResponseTime() { return averageResponseTime; }
    }
    
    public static class DistributedMetrics {
        private final long totalRequests;
        private final long totalSuccesses;
        private final double avgResponseTime;
        private final int nodeCount;
        private final long timestamp;
        private final String errorMessage;
        
        public DistributedMetrics(long totalRequests, long totalSuccesses, double avgResponseTime, int nodeCount, long timestamp) {
            this.totalRequests = totalRequests;
            this.totalSuccesses = totalSuccesses;
            this.avgResponseTime = avgResponseTime;
            this.nodeCount = nodeCount;
            this.timestamp = timestamp;
            this.errorMessage = null;
        }
        
        private DistributedMetrics(String errorMessage) {
            this.totalRequests = 0;
            this.totalSuccesses = 0;
            this.avgResponseTime = 0.0;
            this.nodeCount = 0;
            this.timestamp = System.currentTimeMillis();
            this.errorMessage = errorMessage;
        }
        
        public static DistributedMetrics error(String message) { 
            return new DistributedMetrics(message); 
        }
        
        // Jackson 직렬화를 위한 getter 메서드들
        public long getTotalRequests() { return totalRequests; }
        public long getTotalSuccesses() { return totalSuccesses; }
        public double getAvgResponseTime() { return avgResponseTime; }
        public int getNodeCount() { return nodeCount; }
        public long getTimestamp() { return timestamp; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class NodeHealth {
        private final String nodeId;
        private final long timestamp;
        private final String status;
        
        public NodeHealth(String nodeId, long timestamp, String status) {
            this.nodeId = nodeId;
            this.timestamp = timestamp;
            this.status = status;
        }
        
        // Jackson 직렬화를 위한 getter 메서드들
        public String getNodeId() {
            return nodeId;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public String getStatus() {
            return status;
        }
    }
    
    public static class ExecutionQueueItem {
        private final String executionId;
        private final NodeAllocationResult allocation;
        private final String nodeId;
        
        public ExecutionQueueItem(String executionId, NodeAllocationResult allocation, String nodeId) {
            this.executionId = executionId;
            this.allocation = allocation;
            this.nodeId = nodeId;
        }
        
        // Jackson 직렬화를 위한 getter 메서드들
        public String getExecutionId() { return executionId; }
        public NodeAllocationResult getAllocation() { return allocation; }
        public String getNodeId() { return nodeId; }
    }
}