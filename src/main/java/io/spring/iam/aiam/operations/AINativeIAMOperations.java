package io.spring.iam.aiam.operations;

import io.spring.aicore.operations.AICoreOperations;
import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.aicore.protocol.DomainContext;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
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
 * - 오직 IAMRequest → IAMResponse 변환이라는 자연의 이치만 수행
 * - 구체적 구현은 알지도 모르고 알 필요도 없음
 * - DistributedStrategyExecutor 에게 모든 실행을 완전히 위임
 * 
 * AINativeIAMOperations (마스터 브레인)
 *     ↓ 완전 위임
 * DistributedStrategyExecutor (분산 실행 조율자)
 *     ↓ 전략 선택 및 실행
 * DiagnosisStrategyRegistry (전략 저장소)
 *     ↓ 구체적 전략 실행
 * DiagnosisStrategy 구현체들 (실제 AI 로직)
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
    
    // ==================== 🏭 분산 전략 실행기 (유일한 실행 의존성) ====================
    private final DistributedStrategyExecutor<T> distributedStrategyExecutor; // ✅ 모든 실행을 이것에게 위임
    
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
                                DistributedStrategyExecutor<T> distributedStrategyExecutor) {
        this.sessionManager = sessionManager;
        this.distributedLockService = distributedLockService;
        this.securityValidator = securityValidator;
        this.distributedStrategyExecutor = distributedStrategyExecutor; // ✅ 유일한 실행 의존성
        
        log.info("🎭 AI Native IAM Operations Master Brain initialized");
        log.info("🏭 DistributedStrategyExecutor integrated - Complete delegation established");
        log.info("🌿 Natural Order: AINativeIAMOperations → DistributedStrategyExecutor → DiagnosisStrategyRegistry");
    }
    
    // ==================== 🏛️ 최고 전략 지휘 메서드 (불변의 자연 법칙) ====================
    
    /**
     * 🎯 유일한 진입점 - 모든 AI 진단 요청의 단일 통로
     * 
     * 🌿 자연의 이치:
     * - 어떤 AI 진단이 추가되어도 이 메서드는 절대 변하지 않음
     * - 오직 IAMRequest → IAMResponse 변환만 수행
     * - 모든 구체적 실행은 DistributedStrategyExecutor에게 완전히 위임
     * 
     * 🎯 실행 흐름:
     * 1. 전략적 락 획득 (분산 환경 안전성)
     * 2. 세션 생성 및 감사 ID 생성
     * 3. DistributedStrategyExecutor에게 완전 위임
     * 4. 결과 수집 및 세션 완료
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
            String auditId = generateAuditId(request, strategyId);
            
            log.debug("🏛️ Master Brain: Delegating to DistributedStrategyExecutor - session: {}", sessionId);
            
            // DistributedStrategyExecutor가 다음을 모두 처리:
            // - DiagnosisStrategyRegistry를 통한 전략 선택 및 실행
            // - UniversalPipeline을 통한 6단계 AI 처리 (폴백)
            // - AIStrategySessionRepository를 통한 세션 상태 관리
            // - DistributedAIStrategyCoordinator를 통한 분산 조율
            // - IAMTypeConverter를 통한 타입 변환
            // - RedisEventPublisher를 통한 이벤트 발행
            R result = distributedStrategyExecutor.executeDistributedStrategy(
                request, responseType, sessionId, auditId
            );
            
            sessionManager.completeDistributedExecution(sessionId, auditId, request, result, true);
            successfulStrategicOperations.incrementAndGet();
            
            log.info("✅ Master Brain: Strategic operation completed successfully - {}", strategyId);
            return result;
            
        } catch (Exception e) {
            handleStrategicFailure(strategyId, request, e);
            failedStrategicOperations.incrementAndGet();
            throw new IAMOperationException("Strategic operation failed: " + strategyId, e);
            
        } finally {
            releaseStrategicLock(lockKey, strategyId);
        }
    }
    
    /**
     * 🛡️ 보안 컨텍스트를 포함한 전략 실행
     */
    public <R extends IAMResponse> R executeWithSecurity(IAMRequest<T> request,
                                                         SecurityContext securityContext,
                                                         Class<R> responseType) {
        log.info("🛡️ Master Brain: Secured strategic operation");
        securityValidator.validateRequest(request, securityContext);
        request.addSecurityContext(securityContext);
        return executeWithAudit(request, responseType);
    }
    
    // ==================== 🎯 AICoreOperations 표준 진입점들 ====================
    
    /**
     * 🎯 주요 진입점 - 모든 AI 요청의 표준 진입점
     * 
     * 🌿 자연의 이치: 이 메서드는 절대 변하지 않음
     * - AICoreOperations 표준을 준수
     * - 내부적으로 executeWithAudit() 호출
     */
    @Override
    public <R extends AIResponse> Mono<R> execute(AIRequest<T> request, Class<R> responseType) {
        return Mono.fromCallable(() -> {
            IAMRequest<T> iamRequest = convertToIAMRequest(request);
            
            // 기본 StringResponse 사용
            IAMResponse iamResponse = executeWithAudit(iamRequest, StringResponse.class);
            
            return (R) iamResponse;
        });
    }
    

    /**
     * 🌊 스트리밍 진입점 - 스트리밍 AI 요청 처리
     */
    @Override
    public Flux<String> executeStream(AIRequest<T> request) {
        return Flux.defer(() -> {
            try {
                // AIRequest를 IAMRequest로 변환
                IAMRequest<T> iamRequest = convertToIAMRequest(request);
                // 스트리밍 응답을 위한 특별한 응답 타입 사용
                StringResponse result = executeWithAudit(iamRequest, StringResponse.class);
                return Flux.just(result.getData().toString());
            } catch (Exception e) {
                return Flux.error(e);
            }
        });
    }
    
    /**
     * 🌊 타입 스트리밍 진입점
     */
    @Override
    public <R extends IAMResponse> Flux<R> executeStreamTyped(AIRequest<T> request, Class<R> responseType) {
        return Flux.defer(() -> {
            try {
                IAMRequest<T> iamRequest = convertToIAMRequest(request);
                R iamResponse = executeWithAudit(iamRequest, responseType);
                return Flux.just(iamResponse);
            } catch (Exception e) {
                return Flux.error(e);
            }
        });
    }
    
    /**
     * 📦 배치 진입점 - 여러 AI 요청 일괄 처리
     */
    @Override
    public <R extends IAMResponse> Mono<List<R>> executeBatch(List<AIRequest<T>> requests, Class<R> responseType) {
        return Mono.fromCallable(() -> 
            requests.stream()
                .map(this::convertToIAMRequest)
                .map(request -> executeWithAudit(request, responseType))
                .toList()
        );
    }
    
    /**
     * 🔀 혼합 요청 처리 (미지원)
     */
    @Override
    public <T1 extends DomainContext, T2 extends DomainContext> 
    Mono<AIResponse> executeMixed(List<AIRequest<T1>> requests1, List<AIRequest<T2>> requests2) {
        return Mono.error(new UnsupportedOperationException("Mixed requests not supported in IAM domain"));
    }
    
    /**
     * 🏥 헬스 체크
     */
    @Override
    public Mono<HealthStatus> checkHealth() {
        return Mono.fromCallable(() -> {
            double successRate = calculateSuccessRate();
            if (successRate >= 0.95) return HealthStatus.HEALTHY;
            if (successRate >= 0.80) return HealthStatus.DEGRADED;
            return HealthStatus.UNHEALTHY;
        });
    }
    
    /**
     * 🎯 지원 기능 목록
     */
    @Override
    public Set<AICoreOperations.AICapability> getSupportedCapabilities() {
        return Set.of(
            AICoreOperations.AICapability.TEXT_GENERATION,
            AICoreOperations.AICapability.TEXT_ANALYSIS,
            AICoreOperations.AICapability.STREAMING,
            AICoreOperations.AICapability.BATCH_PROCESSING
        );
    }
    
    /**
     * 🔍 작업 지원 여부 확인
     */
    @Override
    public boolean supportsOperation(String operation) {
        // DistributedStrategyExecutor가 DiagnosisStrategyRegistry를 통해 확인할 것이므로
        // 일단 모든 작업을 지원한다고 응답
        return true;
    }
    
    /**
     * 📊 시스템 메트릭
     */
    @Override
    public Mono<SystemMetrics> getMetrics() {
        return Mono.fromCallable(() -> new SystemMetrics(
            totalStrategicOperations.get(),
            successfulStrategicOperations.get(),
            failedStrategicOperations.get(),
            150.0, // 평균 응답 시간
            10.0,  // 처리량
            0      // 활성 연결
        ));
    }
    
    // ==================== 🎯 IAM 전용 메트릭 및 상태 조회 ====================
    
    /**
     * 🌐 분산 전략 상태 조회
     */
    public DistributedStrategyStatus getDistributedStrategyStatus() {
        return new DistributedStrategyStatus(
            getNodeId(),
            0, 0,
            totalStrategicOperations.get(),
            successfulStrategicOperations.get(),
            failedStrategicOperations.get(),
            calculateSuccessRate(),
            System.currentTimeMillis()
        );
    }
    
    /**
     * 📋 전략 세션 상세 정보 조회
     */
    public DetailedStrategySessionInfo getStrategySessionDetails(String sessionId) {
        return sessionManager.getStrategySessionDetails(sessionId);
    }
    
    /**
     * 🧹 비활성 세션 정리
     */
    public CleanupResult cleanupInactiveSessions(Duration inactiveThreshold) {
        return sessionManager.cleanupInactiveSessions(inactiveThreshold);
    }
    
    /**
     * 📊 메트릭 리포트 생성
     */
    public DistributedMetricsReport generateMetricsReport() {
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
    }
    
    // ==================== 🔧 내부 유틸리티 메서드들 ====================
    
    /**
     * 전략적 락 획득
     */
    private boolean acquireStrategicLock(String lockKey, String strategyId) {
        try {
            return distributedLockService.tryLock(lockKey, getNodeId(), STRATEGIC_LOCK_TIMEOUT);
        } catch (Exception e) {
            log.error("❌ Failed to acquire strategic lock: {} - {}", strategyId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 전략적 락 해제
     */
    private void releaseStrategicLock(String lockKey, String strategyId) {
        try {
            distributedLockService.unlock(lockKey, getNodeId());
        } catch (Exception e) {
            log.warn("⚠️ Failed to release strategic lock: {} - {}", strategyId, e.getMessage());
        }
    }
    
    /**
     * 전략적 실패 처리
     */
    private void handleStrategicFailure(String strategyId, IAMRequest<T> request, Exception error) {
        log.error("❌ Strategic operation failed: {} - {}", strategyId, error.getMessage(), error);
        // 추가 실패 처리 로직 (알림, 메트릭 등)
    }
    
    /**
     * 전략 ID 생성
     */
    private String generateStrategyId(IAMRequest<T> request, Class<?> responseType) {
        return String.format("strategy-%s-%s-%s", 
            request.getClass().getSimpleName(),
            responseType.getSimpleName(),
            UUID.randomUUID().toString().substring(0, 8));
    }
    
    /**
     * 감사 ID 생성
     */
    private String generateAuditId(IAMRequest<T> request, String strategyId) {
        return String.format("audit-%s-%s", strategyId, System.currentTimeMillis());
    }
    
    /**
     * 세션 ID 생성
     */
    private String generateSessionId(String strategyId) {
        return String.format("session-%s", strategyId);
    }
    
    /**
     * 노드 ID 조회
     */
    private String getNodeId() {
        return System.getProperty("node.id", "master-" + UUID.randomUUID().toString().substring(0, 8));
    }
    
    /**
     * 성공률 계산
     */
    private double calculateSuccessRate() {
        long total = totalStrategicOperations.get();
        return total > 0 ? (double) successfulStrategicOperations.get() / total : 0.0;
    }
    
    /**
     * 실패율 계산
     */
    private double calculateFailureRate() {
        long total = totalStrategicOperations.get();
        return total > 0 ? (double) failedStrategicOperations.get() / total : 0.0;
    }
    
    // ==================== 🔄 타입 변환 유틸리티 ====================
    
    /**
     * 🔥 AIRequest를 IAMRequest로 완전 변환 (모든 속성 복사)
     */
    private IAMRequest<T> convertToIAMRequest(AIRequest<T> aiRequest) {
        // 🔥 중요: AIRequest의 모든 속성을 IAMRequest로 완전 복사
        IAMRequest<T> iamRequest = new IAMRequest<>(aiRequest.getContext(), aiRequest.getOperation());
        
        // 🔥 핵심: DiagnosisType이 있으면 복사 (MISSING_DIAGNOSIS_TYPE 오류 해결)
        if (aiRequest instanceof IAMRequest) {
            IAMRequest<T> sourceIAMRequest = (IAMRequest<T>) aiRequest;
            if (sourceIAMRequest.getDiagnosisType() != null) {
                iamRequest.withDiagnosisType(sourceIAMRequest.getDiagnosisType());
                log.debug("🔥 DiagnosisType 복사됨: {}", sourceIAMRequest.getDiagnosisType());
            }
            
            // 🔥 모든 파라미터 복사
            if (sourceIAMRequest.getParameters() != null) {
                sourceIAMRequest.getParameters().forEach(iamRequest::withParameter);
            }
        }
        
        return iamRequest;
    }
    
    // ==================== 🔧 내부 응답 클래스들 ====================
    
    /**
     * 스트리밍용 문자열 응답
     */
    private static class StringResponse extends IAMResponse {
        private final String content;
        
        public StringResponse(String requestId, String content) {
            super(requestId, ExecutionStatus.SUCCESS);
            this.content = content;
        }
        
        @Override
        public Object getData() {
            return content;
        }
        
        @Override
        public String getResponseType() {
            return "STRING_STREAM";
        }
    }
    
    // ==================== 🚨 예외 클래스 ====================
    
    /**
     * IAM 작업 예외
     */
    public static class IAMOperationException extends RuntimeException {
        public IAMOperationException(String message) {
            super(message);
        }
        
        public IAMOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 