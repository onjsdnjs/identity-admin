package io.spring.iam.aiam.operations;

import io.spring.aicore.operations.AICoreOperations;
import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.aicore.protocol.DomainContext;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.iam.aiam.strategy.DiagnosisStrategyRegistry;
import io.spring.redis.RedisDistributedLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🎭 AI Native IAM Operations - 세계 최첨단 분산 AI 전략 기관 마스터 브레인
 * 
 * 🏛️ 성스러운 전략 지휘부 - 오직 전략 지휘와 조율만 담당
 * 
 * 🌿 자연의 이치:
 * - 외부 세계가 아무리 변해도 절대 흔들리지 않음
 * - 오직 IAMRequest → IAMResponse 변환이라는 자연의 이치만 수행
 * - 구체적 구현은 알지도 모르고 알 필요도 없음
 * - DiagnosisStrategyRegistry를 통해 모든 전략을 위임
 * 
 * @param <T> IAM 컨텍스트 타입
 */
@Slf4j
@Service
public class AINativeIAMOperations<T extends IAMContext> implements AIAMOperations<T> {
    
    // ==================== 🎯 전략 지휘부 핵심 구성 ====================
    private final DistributedSessionManager<T> sessionManager;
    private final RedisDistributedLockService distributedLockService;
    private final IAMSecurityValidator securityValidator;
    
    // ==================== 🏭 전략 레지스트리 (유일한 의존성) ====================
    private final DiagnosisStrategyRegistry strategyRegistry; // ✅ 오직 이것만 의존
    
    // ==================== 📊 전략 실행 상태 추적 ====================
    private final AtomicLong totalStrategicOperations = new AtomicLong(0);
    private final AtomicLong successfulStrategicOperations = new AtomicLong(0);
    private final AtomicLong failedStrategicOperations = new AtomicLong(0);
    
    // ==================== 🔧 전략 지휘 설정 ====================
    private static final Duration STRATEGIC_LOCK_TIMEOUT = Duration.ofMinutes(30);
    private static final String STRATEGIC_LOCK_PREFIX = "ai:strategy:master:";
    
    @Autowired
    public AINativeIAMOperations(DistributedSessionManager<T> sessionManager,
                                RedisDistributedLockService distributedLockService,
                                IAMSecurityValidator securityValidator,
                                DiagnosisStrategyRegistry strategyRegistry) {
        this.sessionManager = sessionManager;
        this.distributedLockService = distributedLockService;
        this.securityValidator = securityValidator;
        this.strategyRegistry = strategyRegistry; // ✅ 오직 전략 레지스트리만 주입
        
        log.info("🎭 AI Native IAM Operations Master Brain initialized");
        log.info("🏭 DiagnosisStrategyRegistry integrated - Natural Order Maintained");
    }
    
    // ==================== 🏛️ 최고 전략 지휘 메서드 (불변의 자연 법칙) ====================
    
    /**
     * 🎯 유일한 진입점 - 모든 AI 진단 요청의 단일 통로
     * 
     * 🌿 자연의 이치:
     * - 어떤 AI 진단이 추가되어도 이 메서드는 절대 변하지 않음
     * - 오직 IAMRequest → IAMResponse 변환만 수행
     * - 구체적 진단 로직은 DiagnosisStrategyRegistry가 알아서 처리
     */
    public <R extends IAMResponse> R executeWithAudit(IAMRequest<T> request, Class<R> responseType) {
        String strategyId = generateStrategyId(request, responseType);
        String lockKey = STRATEGIC_LOCK_PREFIX + strategyId;
        
        log.info("🎯 Master Brain: Strategic operation initiated - {}", strategyId);
        totalStrategicOperations.incrementAndGet();
        
        if (!acquireStrategicLock(lockKey, strategyId)) {
            throw new IAMOperationException("Strategic operation conflict: " + strategyId);
        }
        
        try {
            String sessionId = sessionManager.createDistributedStrategySession(request, strategyId);
            
            R result = strategyRegistry.executeStrategy(request, responseType);
            
            sessionManager.completeDistributedExecution(sessionId, strategyId, request, result, true);
            successfulStrategicOperations.incrementAndGet();
            
            log.info("✅ Master Brain: Strategic operation completed - {}", strategyId);
            return result;
            
        } catch (Exception e) {
            handleStrategicFailure(strategyId, request, e);
            failedStrategicOperations.incrementAndGet();
            throw new IAMOperationException("Strategic operation failed: " + strategyId, e);
            
        } finally {
            releaseStrategicLock(lockKey, strategyId);
        }
    }
    
    public <R extends IAMResponse> R executeWithSecurity(IAMRequest<T> request,
                                                         SecurityContext securityContext,
                                                         Class<R> responseType) {
        log.info("🛡️ Master Brain: Secured strategic operation");
        securityValidator.validateRequest(request, securityContext);
        request.addSecurityContext(securityContext);
        return executeWithAudit(request, responseType);
    }
    
    // ==================== 🎯 진짜 진입점들 (AICoreOperations 표준) ====================
    
    /**
     * 🎯 주요 진입점 - 모든 AI 요청의 표준 진입점
     * 
     * 🌿 자연의 이치: 이 메서드는 절대 변하지 않음
     * - AICoreOperations 표준을 준수
     * - 내부적으로 executeWithAudit() 호출
     */
    @Override
    public <R extends AIResponse> Mono<R> execute(AIRequest<T> request, Class<R> responseType) {
        log.info("🎯 Main Entry Point: AI request received - {}", request.getOperation());
        return Mono.fromCallable(() -> {
            // IAMRequest로 변환하고 executeWithAudit 호출
            IAMRequest<T> iamRequest = convertToIAMRequest(request);
            return (R) executeWithAudit(iamRequest, (Class<IAMResponse>) responseType);
        });
    }
    
    /**
     * 🌊 스트리밍 진입점 - 스트리밍 AI 요청 처리
     */
    @Override
    public Flux<String> executeStream(AIRequest<T> request) {
        log.info("🌊 Stream Entry Point: {}", request.getOperation());
        return Flux.error(new UnsupportedOperationException("Streaming through AI Core not yet implemented"));
    }
    
    /**
     * 🌊 타입 스트리밍 진입점
     */
    @Override
    public <R extends AIResponse> Flux<R> executeStreamTyped(AIRequest<T> request, Class<R> responseType) {
        log.info("🌊 Typed Stream Entry Point: {}", request.getOperation());
        return Flux.error(new UnsupportedOperationException("Typed streaming through AI Core not yet implemented"));
    }
    
    /**
     * 📦 배치 진입점 - 여러 AI 요청 일괄 처리
     */
    @Override
    public <R extends AIResponse> Mono<List<R>> executeBatch(List<AIRequest<T>> requests, Class<R> responseType) {
        log.info("📦 Batch Entry Point: {} requests", requests.size());
        return Flux.fromIterable(requests)
                .flatMap(req -> execute(req, responseType))
                .collectList();
    }
    
    @Override
    public <T1 extends DomainContext, T2 extends DomainContext> 
           Mono<AIResponse> executeMixed(List<AIRequest<T1>> requests1, List<AIRequest<T2>> requests2) {
        log.info("🔀 Mixed Entry Point");
        return Mono.error(new UnsupportedOperationException("Mixed execution not yet implemented"));
    }
    
    @Override
    public Mono<AICoreOperations.HealthStatus> checkHealth() {
        return Mono.just(AICoreOperations.HealthStatus.HEALTHY);
    }
    
    @Override
    public Set<AICoreOperations.AICapability> getSupportedCapabilities() {
        return Set.of(
                AICoreOperations.AICapability.TEXT_GENERATION
        );
    }
    
    @Override
    public boolean supportsOperation(String operation) {
        return operation.startsWith("iam.") || operation.startsWith("policy.") || operation.startsWith("risk.");
    }
    
    @Override
    public Mono<AICoreOperations.SystemMetrics> getMetrics() {
        return Mono.error(new UnsupportedOperationException("System metrics not yet implemented"));
    }
    
    // ==================== 🎯 분산 모니터링 및 관리 API ====================
    
    public DistributedStrategyStatus getDistributedStrategyStatus() {
        try {
            return new DistributedStrategyStatus(
                getNodeId(),
                0, 0,
                totalStrategicOperations.get(),
                successfulStrategicOperations.get(),
                failedStrategicOperations.get(),
                calculateSuccessRate(),
                System.currentTimeMillis()
            );
        } catch (Exception e) {
            return DistributedStrategyStatus.error(e.getMessage());
        }
    }
    
    public DetailedStrategySessionInfo getStrategySessionDetails(String sessionId) {
        return sessionManager.getStrategySessionDetails(sessionId);
    }
    
    public CleanupResult cleanupInactiveSessions(Duration inactiveThreshold) {
        return sessionManager.cleanupInactiveSessions(inactiveThreshold);
    }
    
    public DistributedMetricsReport generateMetricsReport() {
        try {
            return new DistributedMetricsReport(
                getNodeId(),
                totalStrategicOperations.get(),
                successfulStrategicOperations.get(),
                failedStrategicOperations.get(),
                calculateSuccessRate(),
                calculateFailureRate(),
                0, 0, 150.0,
                Map.of(), Map.of(),
                System.currentTimeMillis()
            );
        } catch (Exception e) {
            return DistributedMetricsReport.error(e.getMessage());
        }
    }
    
    // ==================== 🔧 전략적 지원 메서드 ====================
    
    private boolean acquireStrategicLock(String lockKey, String strategyId) {
        try {
            return distributedLockService.tryLock(lockKey, getNodeId(), STRATEGIC_LOCK_TIMEOUT);
        } catch (Exception e) {
            log.error("❌ Failed to acquire strategic lock for {}", strategyId, e);
            return false;
        }
    }
    
    private void releaseStrategicLock(String lockKey, String strategyId) {
        try {
            distributedLockService.unlock(lockKey, getNodeId());
        } catch (Exception e) {
            log.warn("⚠️ Failed to release strategic lock for {}", strategyId, e);
        }
    }
    
    private void handleStrategicFailure(String strategyId, IAMRequest<T> request, Exception error) {
        log.error("❌ Master Brain: Strategic failure - {} | Error: {}", strategyId, error.getMessage());
    }
    
    private String generateStrategyId(IAMRequest<T> request, Class<?> responseType) {
        return String.format("strategy-%s-%s-%d", 
            request.getClass().getSimpleName(),
            responseType.getSimpleName(),
            System.currentTimeMillis()
        );
    }
    
    private String getNodeId() {
        return System.getProperty("node.id", "master-node-" + UUID.randomUUID().toString().substring(0, 8));
    }
    
    private double calculateSuccessRate() {
        long total = totalStrategicOperations.get();
        return total > 0 ? (double) successfulStrategicOperations.get() / total * 100.0 : 0.0;
    }
    
    private double calculateFailureRate() {
        long total = totalStrategicOperations.get();
        return total > 0 ? (double) failedStrategicOperations.get() / total * 100.0 : 0.0;
    }
    
    // ==================== 🔄 변환 유틸리티 ====================
    
    private IAMRequest<T> convertToIAMRequest(AIRequest<T> aiRequest) {
        // AIRequest를 IAMRequest로 변환하는 로직
        // 실제 구현 필요
        return (IAMRequest<T>) aiRequest; // 임시 캐스팅
    }
} 