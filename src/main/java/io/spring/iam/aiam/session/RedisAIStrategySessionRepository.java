package io.spring.iam.aiam.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.iam.aiam.operations.LabExecutionStrategy;
import io.spring.redis.RedisDistributedLockService;
import io.spring.redis.RedisEventPublisher;
import io.spring.session.SessionIdGenerationException;
import io.spring.session.generator.SessionIdGenerator;
import io.spring.session.impl.RedisMfaRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis 기반 AI 전략 세션 리포지토리 구현체
 * 
 * 🔴 기존 RedisMfaRepository 인프라를 완전히 활용
 * - 분산 락을 통한 안전한 세션 관리
 * - Redis 이벤트를 통한 실시간 상태 동기화
 * - 기존 세션 ID 생성 로직 재사용
 * - Redis 스크립트를 통한 원자성 보장
 */
@Slf4j
public class RedisAIStrategySessionRepository extends RedisMfaRepository
        implements AIStrategySessionRepository {
    
    private final RedisDistributedLockService lockService;
    private final RedisEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    
    // Redis 키 패턴 (기존 MFA 세션과 구분)
    private static final String AI_STRATEGY_PREFIX = "ai:strategy:session:";
    private static final String AI_STATE_PREFIX = "ai:strategy:state:";
    private static final String AI_LAB_ALLOCATION_PREFIX = "ai:lab:allocation:";
    private static final String AI_METRICS_PREFIX = "ai:metrics:";
    private static final String AI_RESULT_PREFIX = "ai:result:";
    private static final String AI_ACTIVE_SESSIONS_KEY = "ai:active:sessions";
    private static final String AI_NODE_SESSIONS_PREFIX = "ai:node:sessions:";
    private static final String AI_STATS_KEY = "ai:strategy:stats";
    
    // 로컬 캐시 (성능 최적화)
    private final Map<String, AIStrategySessionState> localStateCache = new ConcurrentHashMap<>();
    private final AtomicLong totalStrategySessionsCreated = new AtomicLong(0);
    private final AtomicLong completedStrategySessions = new AtomicLong(0);
    private final AtomicLong failedStrategySessions = new AtomicLong(0);
    
    // Redis 스크립트
    private static final String CREATE_STRATEGY_SESSION_SCRIPT =
            "local sessionKey = KEYS[1] " +
            "local stateKey = KEYS[2] " +
            "local activeKey = KEYS[3] " +
            "local nodeKey = KEYS[4] " +
            "local sessionData = ARGV[1] " +
            "local stateData = ARGV[2] " +
            "local ttl = ARGV[3] " +
            "local sessionId = ARGV[4] " +
            "local nodeId = ARGV[5] " +
            "if redis.call('EXISTS', sessionKey) == 0 then " +
            "  redis.call('PSETEX', sessionKey, ttl, sessionData) " +
            "  redis.call('PSETEX', stateKey, ttl, stateData) " +
            "  redis.call('SADD', activeKey, sessionId) " +
            "  redis.call('SADD', nodeKey, sessionId) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";
    
    private static final String UPDATE_STRATEGY_STATE_SCRIPT =
            "local stateKey = KEYS[1] " +
            "local stateData = ARGV[1] " +
            "local ttl = ARGV[2] " +
            "if redis.call('EXISTS', stateKey) == 1 then " +
            "  redis.call('PSETEX', stateKey, ttl, stateData) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";
    
    public RedisAIStrategySessionRepository(StringRedisTemplate redisTemplate,
                                          SessionIdGenerator sessionIdGenerator,
                                          RedisDistributedLockService lockService,
                                          RedisEventPublisher eventPublisher,
                                          ObjectMapper objectMapper) {
        super(redisTemplate, sessionIdGenerator);
        this.lockService = lockService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String createStrategySession(LabExecutionStrategy strategy, 
                                      Map<String, Object> context,
                                      HttpServletRequest request, 
                                      HttpServletResponse response) {
        // 1. 고유한 세션 ID 생성 (기존 MFA 로직 활용)
        String sessionId = generateUniqueSessionId("ai-strategy", request);
        String lockKey = "create-strategy-session:" + sessionId;
        
        try {
            // 2. 분산 락 획득 (중복 생성 방지)
            boolean lockAcquired = lockService.tryLock(lockKey, getNodeId(), Duration.ofMinutes(1));
            if (!lockAcquired) {
                throw new SessionIdGenerationException("Failed to acquire lock for strategy session creation");
            }
            
            try {
                // 3. 세션 데이터 준비
                String nodeId = getNodeId();
                long currentTime = System.currentTimeMillis();
                
                Map<String, Object> sessionData = createStrategySessionData(strategy, context, nodeId, currentTime);
                AIStrategySessionState initialState = new AIStrategySessionState(
                    sessionId, strategy.getStrategyId(), AIStrategyExecutionPhase.INITIALIZED,
                    nodeId, currentTime, currentTime, context, new HashMap<>()
                );
                
                // 4. Redis에 원자적으로 저장
                String sessionKey = AI_STRATEGY_PREFIX + sessionId;
                String stateKey = AI_STATE_PREFIX + sessionId;
                String activeKey = AI_ACTIVE_SESSIONS_KEY;
                String nodeKey = AI_NODE_SESSIONS_PREFIX + nodeId;
                
                Long result = redisTemplate().execute(
                    new DefaultRedisScript<>(CREATE_STRATEGY_SESSION_SCRIPT, Long.class),
                    Arrays.asList(sessionKey, stateKey, activeKey, nodeKey),
                    objectMapper.writeValueAsString(sessionData),
                    objectMapper.writeValueAsString(initialState),
                    String.valueOf(sessionTimeout().toMillis()),
                    sessionId,
                    nodeId
                );
                
                if (result == 1) {
                    // 5. 로컬 캐시 업데이트
                    localStateCache.put(sessionId, initialState);
                    totalStrategySessionsCreated.incrementAndGet();
                    
                    // 6. 기존 MFA 세션도 생성 (통합 관리)
                    storeSession(sessionId, request, response);
                    
                    // 7. 이벤트 발행
                    publishStrategySessionEvent("STRATEGY_SESSION_CREATED", sessionId, strategy, context);
                    
                    log.info("AI Strategy session created successfully: {} for strategy: {}", 
                            sessionId, strategy.getStrategyId());
                    
                    return sessionId;
                } else {
                    throw new SessionIdGenerationException("Failed to create strategy session in Redis");
                }
                
            } finally {
                lockService.unlock(lockKey, getNodeId());
            }
            
        } catch (Exception e) {
            log.error("Failed to create AI strategy session: {}", e.getMessage(), e);
            throw new SessionIdGenerationException("Strategy session creation failed", e);
        }
    }
    
    @Override
    public void updateStrategyState(String sessionId, 
                                  AIStrategyExecutionPhase phase, 
                                  Map<String, Object> phaseData) {
        String lockKey = "update-strategy-state:" + sessionId;
        
        try {
            boolean lockAcquired = lockService.tryLock(lockKey, getNodeId(), Duration.ofSeconds(30));
            if (!lockAcquired) {
                log.warn("Failed to acquire lock for strategy state update: {}", sessionId);
                return;
            }
            
            try {
                // 현재 상태 조회
                AIStrategySessionState currentState = getStrategyState(sessionId);
                if (currentState == null) {
                    log.warn("Strategy session not found for state update: {}", sessionId);
                    return;
                }
                
                // 새로운 상태 생성
                Map<String, Object> newPhaseData = new HashMap<>(currentState.getPhaseData());
                if (phaseData != null) {
                    newPhaseData.putAll(phaseData);
                }
                
                AIStrategySessionState newState = new AIStrategySessionState(
                    sessionId, currentState.getStrategyId(), phase, getNodeId(),
                    currentState.getCreateTime(), System.currentTimeMillis(),
                    currentState.getContext(), newPhaseData
                );
                
                // Redis 업데이트
                String stateKey = AI_STATE_PREFIX + sessionId;
                Long result = redisTemplate().execute(
                    new DefaultRedisScript<>(UPDATE_STRATEGY_STATE_SCRIPT, Long.class),
                    Collections.singletonList(stateKey),
                    objectMapper.writeValueAsString(newState),
                    String.valueOf(sessionTimeout().toMillis())
                );
                
                if (result == 1) {
                    // 로컬 캐시 업데이트
                    localStateCache.put(sessionId, newState);
                    
                    // 세션 갱신 (TTL 연장)
                    refreshSession(sessionId);
                    
                    // 완료/실패 상태 통계 업데이트
                    if (phase == AIStrategyExecutionPhase.COMPLETED) {
                        completedStrategySessions.incrementAndGet();
                        removeFromActiveSessions(sessionId);
                    } else if (phase == AIStrategyExecutionPhase.FAILED || phase == AIStrategyExecutionPhase.CANCELLED) {
                        failedStrategySessions.incrementAndGet();
                        removeFromActiveSessions(sessionId);
                    }
                    
                    // 이벤트 발행
                    publishStrategyStateEvent("STRATEGY_STATE_UPDATED", sessionId, phase, phaseData);
                    
                    log.debug("Strategy state updated: {} -> {}", sessionId, phase);
                } else {
                    log.warn("Failed to update strategy state in Redis: {}", sessionId);
                }
                
            } finally {
                lockService.unlock(lockKey, getNodeId());
            }
            
        } catch (Exception e) {
            log.error("Error updating strategy state for session {}: {}", sessionId, e.getMessage(), e);
        }
    }
    
    @Override
    public AIStrategySessionState getStrategyState(String sessionId) {
        // 로컬 캐시 먼저 확인
        AIStrategySessionState cachedState = localStateCache.get(sessionId);
        if (cachedState != null) {
            return cachedState;
        }
        
        // Redis에서 조회
        try {
            String stateKey = AI_STATE_PREFIX + sessionId;
            String stateJson = redisTemplate().opsForValue().get(stateKey);
            
            if (stateJson != null) {
                AIStrategySessionState state = objectMapper.readValue(stateJson, AIStrategySessionState.class);
                localStateCache.put(sessionId, state); // 캐시 업데이트
                return state;
            }
        } catch (Exception e) {
            log.error("Error retrieving strategy state for session {}: {}", sessionId, e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public void storeLabAllocation(String sessionId, String labType, String nodeId, Map<String, Object> allocation) {
        try {
            AILabAllocation labAllocation = new AILabAllocation(
                sessionId, labType, nodeId, allocation, System.currentTimeMillis()
            );
            
            String allocationKey = AI_LAB_ALLOCATION_PREFIX + sessionId;
            redisTemplate().opsForValue().set(
                allocationKey, 
                objectMapper.writeValueAsString(labAllocation),
                sessionTimeout()
            );
            
            // 이벤트 발행
            publishLabAllocationEvent("LAB_ALLOCATED", sessionId, labType, nodeId, allocation);
            
            log.debug("Lab allocation stored: {} -> {} on node {}", sessionId, labType, nodeId);
            
        } catch (Exception e) {
            log.error("Error storing lab allocation for session {}: {}", sessionId, e.getMessage(), e);
        }
    }
    
    @Override
    public AILabAllocation getLabAllocation(String sessionId) {
        try {
            String allocationKey = AI_LAB_ALLOCATION_PREFIX + sessionId;
            String allocationJson = redisTemplate().opsForValue().get(allocationKey);
            
            if (allocationJson != null) {
                return objectMapper.readValue(allocationJson, AILabAllocation.class);
            }
        } catch (Exception e) {
            log.error("Error retrieving lab allocation for session {}: {}", sessionId, e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public void recordExecutionMetrics(String sessionId, AIExecutionMetrics metrics) {
        try {
            String metricsKey = AI_METRICS_PREFIX + sessionId;
            redisTemplate().opsForValue().set(
                metricsKey,
                objectMapper.writeValueAsString(metrics),
                Duration.ofDays(7) // 메트릭은 더 오래 보관
            );
            
            log.debug("Execution metrics recorded for session: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Error recording execution metrics for session {}: {}", sessionId, e.getMessage(), e);
        }
    }
    
    @Override
    public List<String> getActiveStrategySessions() {
        try {
            Set<String> activeSessions = redisTemplate().opsForSet().members(AI_ACTIVE_SESSIONS_KEY);
            return activeSessions != null ? new ArrayList<>(activeSessions) : new ArrayList<>();
        } catch (Exception e) {
            log.error("Error retrieving active strategy sessions: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<String> getActiveSessionsByNode(String nodeId) {
        try {
            String nodeKey = AI_NODE_SESSIONS_PREFIX + nodeId;
            Set<String> nodeSessions = redisTemplate().opsForSet().members(nodeKey);
            return nodeSessions != null ? new ArrayList<>(nodeSessions) : new ArrayList<>();
        } catch (Exception e) {
            log.error("Error retrieving active sessions for node {}: {}", nodeId, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean migrateStrategySession(String sessionId, String fromNodeId, String toNodeId) {
        String lockKey = "migrate-session:" + sessionId;
        
        try {
            boolean lockAcquired = lockService.tryLock(lockKey, getNodeId(), Duration.ofMinutes(5));
            if (!lockAcquired) {
                log.warn("Failed to acquire lock for session migration: {}", sessionId);
                return false;
            }
            
            try {
                // 노드 세션 목록에서 이동
                String fromNodeKey = AI_NODE_SESSIONS_PREFIX + fromNodeId;
                String toNodeKey = AI_NODE_SESSIONS_PREFIX + toNodeId;
                
                redisTemplate().opsForSet().move(fromNodeKey, sessionId, toNodeKey);
                
                // 상태 업데이트 (새 노드 정보 반영)
                AIStrategySessionState currentState = getStrategyState(sessionId);
                if (currentState != null) {
                    AIStrategySessionState migratedState = new AIStrategySessionState(
                        sessionId, currentState.getStrategyId(), currentState.getPhase(),
                        toNodeId, currentState.getCreateTime(), System.currentTimeMillis(),
                        currentState.getContext(), currentState.getPhaseData()
                    );
                    
                    String stateKey = AI_STATE_PREFIX + sessionId;
                    redisTemplate().opsForValue().set(
                        stateKey,
                        objectMapper.writeValueAsString(migratedState),
                        sessionTimeout()
                    );
                    
                    localStateCache.put(sessionId, migratedState);
                }
                
                // 이벤트 발행
                publishSessionMigrationEvent("SESSION_MIGRATED", sessionId, fromNodeId, toNodeId);
                
                log.info("Strategy session migrated: {} from {} to {}", sessionId, fromNodeId, toNodeId);
                return true;
                
            } finally {
                lockService.unlock(lockKey, getNodeId());
            }
            
        } catch (Exception e) {
            log.error("Error migrating strategy session {}: {}", sessionId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public void storeExecutionResult(String sessionId, AIExecutionResult result) {
        try {
            String resultKey = AI_RESULT_PREFIX + sessionId;
            redisTemplate().opsForValue().set(
                resultKey,
                objectMapper.writeValueAsString(result),
                Duration.ofDays(30) // 결과는 더 오래 보관
            );
            
            log.debug("Execution result stored for session: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Error storing execution result for session {}: {}", sessionId, e.getMessage(), e);
        }
    }
    
    @Override
    public AIExecutionResult getExecutionResult(String sessionId) {
        try {
            String resultKey = AI_RESULT_PREFIX + sessionId;
            String resultJson = redisTemplate().opsForValue().get(resultKey);
            
            if (resultJson != null) {
                return objectMapper.readValue(resultJson, AIExecutionResult.class);
            }
        } catch (Exception e) {
            log.error("Error retrieving execution result for session {}: {}", sessionId, e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public void syncSessionAcrossNodes(String sessionId) {
        // Redis는 자동으로 분산 동기화되므로 특별한 작업 불필요
        // 필요시 로컬 캐시 무효화
        localStateCache.remove(sessionId);
        
        // 동기화 이벤트 발행
        publishSyncEvent("SESSION_SYNC_REQUESTED", sessionId);
    }
    
    @Override
    public AIStrategySessionStats getAIStrategyStats() {
        try {
            // 기본 세션 통계 (부모 클래스)
            SessionStats baseStats = getSessionStats();
            
            // AI 전략 특화 통계
            long activeStrategySessions = getActiveStrategySessions().size();
            long completed = completedStrategySessions.get();
            long failed = failedStrategySessions.get();
            
            // 연구소 타입별 분포 (실제 구현에서는 Redis에서 집계)
            Map<String, Long> labTypeDistribution = calculateLabTypeDistribution();
            
            // 노드별 분포
            Map<String, Long> nodeDistribution = calculateNodeDistribution();
            
            return new AIStrategySessionStats(
                baseStats.getActiveSessions(),
                totalStrategySessionsCreated.get(),
                baseStats.getSessionCollisions(),
                baseStats.getAverageSessionDuration(),
                "Redis-AI-Strategy",
                activeStrategySessions,
                completed,
                failed,
                calculateAverageExecutionTime(),
                labTypeDistribution,
                nodeDistribution
            );
            
        } catch (Exception e) {
            log.error("Error retrieving AI strategy stats: {}", e.getMessage());
            return null;
        }
    }
    
    // ==================== Private Helper Methods ====================
    
    private String getNodeId() {
        // 실제 구현에서는 서버 인스턴스 ID 사용
        return System.getenv("HOSTNAME") != null ? 
               System.getenv("HOSTNAME") : "ai-node-" + System.currentTimeMillis();
    }
    
    private Map<String, Object> createStrategySessionData(LabExecutionStrategy strategy, 
                                                        Map<String, Object> context,
                                                        String nodeId, long createTime) {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("strategyId", strategy.getStrategyId());
        sessionData.put("operationType", strategy.getOperationType());
        sessionData.put("context", context);
        sessionData.put("nodeId", nodeId);
        sessionData.put("createTime", createTime);
        sessionData.put("type", "AI_STRATEGY_SESSION");
        return sessionData;
    }
    
    private void removeFromActiveSessions(String sessionId) {
        try {
            redisTemplate().opsForSet().remove(AI_ACTIVE_SESSIONS_KEY, sessionId);
            
            // 노드별 세션 목록에서도 제거
            String nodeKey = AI_NODE_SESSIONS_PREFIX + getNodeId();
            redisTemplate().opsForSet().remove(nodeKey, sessionId);
            
        } catch (Exception e) {
            log.error("Error removing session from active list: {}", e.getMessage());
        }
    }
    
    private void publishStrategySessionEvent(String eventType, String sessionId, 
                                           LabExecutionStrategy strategy, Map<String, Object> context) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("sessionId", sessionId);
        eventData.put("strategyId", strategy.getStrategyId());
        eventData.put("operationType", strategy.getOperationType());
        eventData.put("context", context);
        eventData.put("nodeId", getNodeId());
        
        eventPublisher.publishEvent("ai-strategy-events", Map.of(
            "eventType", eventType,
            "category", "AI_STRATEGY",
            "data", eventData,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    private void publishStrategyStateEvent(String eventType, String sessionId, 
                                         AIStrategyExecutionPhase phase, Map<String, Object> phaseData) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("sessionId", sessionId);
        eventData.put("phase", phase.name());
        eventData.put("phaseDescription", phase.getDescription());
        eventData.put("phaseData", phaseData);
        eventData.put("nodeId", getNodeId());
        
        eventPublisher.publishEvent("ai-strategy-state-events", Map.of(
            "eventType", eventType,
            "category", "AI_STRATEGY_STATE",
            "data", eventData,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    private void publishLabAllocationEvent(String eventType, String sessionId, String labType, 
                                         String nodeId, Map<String, Object> allocation) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("sessionId", sessionId);
        eventData.put("labType", labType);
        eventData.put("allocatedNodeId", nodeId);
        eventData.put("allocation", allocation);
        eventData.put("publisherNodeId", getNodeId());
        
        eventPublisher.publishEvent("ai-lab-allocation-events", Map.of(
            "eventType", eventType,
            "category", "AI_LAB_ALLOCATION",
            "data", eventData,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    private void publishSessionMigrationEvent(String eventType, String sessionId, 
                                            String fromNodeId, String toNodeId) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("sessionId", sessionId);
        eventData.put("fromNodeId", fromNodeId);
        eventData.put("toNodeId", toNodeId);
        eventData.put("migratorNodeId", getNodeId());
        
        eventPublisher.publishEvent("ai-session-migration-events", Map.of(
            "eventType", eventType,
            "category", "AI_SESSION_MIGRATION",
            "data", eventData,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    private void publishSyncEvent(String eventType, String sessionId) {
        eventPublisher.publishEvent("ai-session-sync-events", Map.of(
            "eventType", eventType,
            "category", "AI_SESSION_SYNC",
            "sessionId", sessionId,
            "nodeId", getNodeId(),
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    // TODO: 실제 구현에서는 Redis 집계 쿼리 사용
    private Map<String, Long> calculateLabTypeDistribution() {
        return Map.of("PolicyGeneration", 10L, "RiskAssessment", 5L, "Recommendation", 3L);
    }
    
    private Map<String, Long> calculateNodeDistribution() {
        return Map.of("node-1", 8L, "node-2", 6L, "node-3", 4L);
    }
    
    private double calculateAverageExecutionTime() {
        return 2500.0; // milliseconds
    }
}