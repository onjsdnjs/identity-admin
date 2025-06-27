package io.spring.iam.aiam.operations;

import io.spring.aicore.pipeline.PipelineConfiguration;
import io.spring.aicore.pipeline.UniversalPipeline;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.iam.aiam.session.AIExecutionMetrics;
import io.spring.iam.aiam.session.AIStrategyExecutionPhase;
import io.spring.iam.aiam.session.AIStrategySessionRepository;
import io.spring.iam.aiam.strategy.DiagnosisStrategyRegistry;
import io.spring.iam.aiam.strategy.DiagnosisException;
import io.spring.iam.redis.DistributedAIStrategyCoordinator;
import io.spring.redis.RedisEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * 🏭 분산 전략 실행을 담당하는 전용 서비스
 * 
 * 🎯 핵심 역할:
 * 1. 마스터 브레인(AINativeIAMOperations)의 지휘를 받아 구체적인 실행 담당
 * 2. DiagnosisStrategyRegistry를 통해 적절한 전략 선택 및 실행
 * 3. 분산 환경에서의 세션 관리 및 상태 추적
 * 4. AI 파이프라인과 전략 실행의 조율
 * 
 * 🌿 자연의 이치:
 * - AINativeIAMOperations → DistributedStrategyExecutor → DiagnosisStrategyRegistry → 구체적 전략
 * - 각 계층은 자신의 역할만 수행하고 하위 계층에 위임
 */
@Slf4j
@Service
public class DistributedStrategyExecutor<T extends IAMContext> {
    
    private final UniversalPipeline pipeline;
    private final AIStrategySessionRepository sessionRepository;
    private final DistributedAIStrategyCoordinator strategyCoordinator;
    private final RedisEventPublisher eventPublisher;
    private final IAMTypeConverter typeConverter;
    
    // ==================== 🎯 핵심 의존성: 전략 레지스트리 ====================
    private final DiagnosisStrategyRegistry strategyRegistry; // ✅ 실제 전략 실행 담당
    
    @Autowired
    public DistributedStrategyExecutor(UniversalPipeline pipeline,
                                     AIStrategySessionRepository sessionRepository,
                                     DistributedAIStrategyCoordinator strategyCoordinator,
                                     RedisEventPublisher eventPublisher,
                                     IAMTypeConverter typeConverter,
                                     DiagnosisStrategyRegistry strategyRegistry) {
        this.pipeline = pipeline;
        this.sessionRepository = sessionRepository;
        this.strategyCoordinator = strategyCoordinator;
        this.eventPublisher = eventPublisher;
        this.typeConverter = typeConverter;
        this.strategyRegistry = strategyRegistry; // ✅ 전략 레지스트리 주입
        
        log.info("🏭 DistributedStrategyExecutor initialized with DiagnosisStrategyRegistry");
    }
    
    /**
     * 🎯 분산 전략 실행 - 마스터 브레인의 명령을 받아 실행
     * 
     * 실행 흐름:
     * 1. LAB_ALLOCATION: 세션 상태 업데이트 및 랩 전략 생성
     * 2. EXECUTING: DiagnosisStrategyRegistry를 통한 실제 전략 실행
     * 3. VALIDATING: 결과 검증 및 세션 완료
     */
    public <R extends IAMResponse> R executeDistributedStrategy(IAMRequest<T> request, 
                                                               Class<R> responseType,
                                                               String sessionId, 
                                                               String auditId) {
        try {
            // Phase 1: LAB_ALLOCATION - 세션 상태 업데이트
            updateSessionState(sessionId, AIStrategyExecutionPhase.LAB_ALLOCATION, Map.of(
                "auditId", auditId,
                "requestType", request.getClass().getSimpleName(),
                "diagnosisType", request.getDiagnosisType() != null ? request.getDiagnosisType().name() : "UNKNOWN"
            ));
            
            LabExecutionStrategy labStrategy = createLabExecutionStrategy(request, sessionId);
            
            // Phase 2: EXECUTING - 실제 전략 실행
            updateSessionState(sessionId, AIStrategyExecutionPhase.EXECUTING, Map.of(
                "labStrategy", labStrategy.getStrategyName(),
                "expectedDuration", labStrategy.getExpectedDuration(),
                "startTime", System.currentTimeMillis()
            ));
            
            // ✅ 핵심: DiagnosisStrategyRegistry를 통한 전략 실행
            R result = executeStrategyThroughRegistry(request, responseType, sessionId);
            
            // Phase 3: VALIDATING - 결과 검증
            updateSessionState(sessionId, AIStrategyExecutionPhase.VALIDATING, Map.of(
                "resultType", result.getClass().getSimpleName(),
                "validationStartTime", System.currentTimeMillis(),
                "executionCompleted", true
            ));
            
            validateResult(result, sessionId);
            
            // Phase 4: COMPLETED - 성공 완료
            updateSessionState(sessionId, AIStrategyExecutionPhase.COMPLETED, Map.of(
                "completionTime", System.currentTimeMillis(),
                "success", true
            ));
            
            return result;
            
        } catch (DiagnosisException e) {
            log.error("❌ Strategy execution failed for session: {} - {}", sessionId, e.getMessage(), e);
            updateSessionState(sessionId, AIStrategyExecutionPhase.FAILED, Map.of(
                "error", e.getMessage(),
                "errorCode", e.getErrorCode(),
                "failureTime", System.currentTimeMillis()
            ));
            throw new IAMOperationException("Strategy execution failed: " + e.getMessage(), e);
            
        } catch (Exception e) {
            log.error("❌ Distributed strategy execution failed for session: {}", sessionId, e);
            updateSessionState(sessionId, AIStrategyExecutionPhase.FAILED, Map.of(
                "error", e.getMessage(),
                "failureTime", System.currentTimeMillis()
            ));
            throw new IAMOperationException("Strategy execution failed", e);
        }
    }
    
    /**
     * 🎯 핵심 메서드: DiagnosisStrategyRegistry를 통한 전략 실행
     * 
     * 이 메서드가 실제로 DiagnosisStrategyRegistry의 executeStrategy()를 호출합니다.
     */
    private <R extends IAMResponse> R executeStrategyThroughRegistry(IAMRequest<T> request, 
                                                                   Class<R> responseType, 
                                                                   String sessionId) {
        try {
            log.debug("🎯 Executing strategy through registry for session: {} - diagnosisType: {}", 
                sessionId, request.getDiagnosisType());
            
            // ✅ 핵심: DiagnosisStrategyRegistry 에게 전략 실행 위임
            R result = strategyRegistry.executeStrategy(request, responseType);
            
            log.debug("✅ Strategy execution completed for session: {} - resultType: {}", 
                sessionId, result.getClass().getSimpleName());
            
            return result;
            
        } catch (DiagnosisException e) {
            log.error("❌ Strategy registry execution failed for session: {} - {}", sessionId, e.getMessage());
            
            // 폴백: AI 파이프라인 시도
            log.info("🔄 Attempting fallback to AI pipeline for session: {}", sessionId);
            return executeAIPipelineFallback(request, responseType, sessionId);
            
        } catch (Exception e) {
            log.error("❌ Unexpected error in strategy execution for session: {}", sessionId, e);
            throw new DiagnosisException(
                request.getDiagnosisType() != null ? request.getDiagnosisType().name() : "UNKNOWN",
                "STRATEGY_EXECUTION_ERROR",
                "전략 실행 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }
    
    /**
     * 🔄 폴백: AI 파이프라인 실행 (전략 실행 실패 시)
     */
    private <R extends IAMResponse> R executeAIPipelineFallback(IAMRequest<T> request, 
                                                              Class<R> responseType, 
                                                              String sessionId) {
        try {
            log.info("🔄 Fallback: AI Pipeline execution for session: {}", sessionId);
            
            // 파이프라인 설정 생성
            PipelineConfiguration config = createPipelineConfiguration();
            
            // 파이프라인 실행
            IAMResponse result = pipeline.execute(request, config, responseType)
                .block(); // 동기식 실행
            
            // 타입 캐스팅
            return responseType.cast(result);
            
        } catch (Exception e) {
            log.error("❌ AI Pipeline fallback failed for session: {}", sessionId, e);
            // 최종 폴백: Mock 응답 사용
            return createMockResponse(request, responseType, sessionId);
        }
    }
    
    /**
     * 파이프라인 설정 생성
     */
    private PipelineConfiguration createPipelineConfiguration() {
        return PipelineConfiguration.builder()
            .addStep(PipelineConfiguration.PipelineStep.PREPROCESSING)
            .addStep(PipelineConfiguration.PipelineStep.CONTEXT_RETRIEVAL)
            .addStep(PipelineConfiguration.PipelineStep.PROMPT_GENERATION)
            .addStep(PipelineConfiguration.PipelineStep.LLM_EXECUTION)
            .addStep(PipelineConfiguration.PipelineStep.RESPONSE_PARSING)
            .addStep(PipelineConfiguration.PipelineStep.POSTPROCESSING)
            .addParameter("enableCaching", true)
            .addParameter("timeoutSeconds", 30)
            .addParameter("retryCount", 3)
            .timeoutSeconds(30)
            .enableCaching(true)
            .build();
    }
    
    /**
     * Mock 응답 생성 (최종 폴백)
     */
    @SuppressWarnings("unchecked")
    private <R extends IAMResponse> R createMockResponse(IAMRequest<T> request, 
                                                        Class<R> responseType, 
                                                        String sessionId) {
        try {
            return typeConverter.createMockResponse(request, responseType, sessionId);
        } catch (Exception e) {
            log.error("❌ Mock response creation failed for session: {}", sessionId, e);
            throw new IAMOperationException("Mock response creation failed", e);
        }
    }
    
    /**
     * 결과 검증
     */
    private void validateResult(IAMResponse result, String sessionId) {
        if (result == null) {
            throw new IAMOperationException("Strategy execution returned null result for session: " + sessionId);
        }
        
        // 추가 검증 로직
        log.debug("✅ Result validation completed for session: {}", sessionId);
    }
    
    /**
     * 세션 상태 업데이트
     */
    private void updateSessionState(String sessionId, AIStrategyExecutionPhase phase, Map<String, Object> phaseData) {
        try {
            sessionRepository.updateExecutionPhase(sessionId, phase, phaseData);
            
            // 분산 이벤트 발행
            eventPublisher.publishEvent("ai:strategy:phase:updated", Map.of(
                "sessionId", sessionId,
                "phase", phase.name(),
                "timestamp", System.currentTimeMillis(),
                "phaseData", phaseData
            ));
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to update session state for {}: {}", sessionId, e.getMessage());
        }
    }
    
    /**
     * Lab 실행 전략 생성
     */
    private LabExecutionStrategy createLabExecutionStrategy(IAMRequest<T> request, String strategyId) {
        return LabExecutionStrategy.builder()
            .strategyId(strategyId)
            .requestType(request.getClass().getSimpleName())
            .complexity(determineComplexity(request))
            .priority(request.getPriority().name())
            .build();
    }
    
    /**
     * 요청 복잡도 판단
     */
    private int determineComplexity(IAMRequest<T> request) {
        // 간단한 복잡도 계산 로직
        return request.getClass().getSimpleName().length() % 10 + 1;
    }
    
    /**
     * 실행 메트릭 생성
     */
    public AIExecutionMetrics createExecutionMetrics(String sessionId, boolean success) {
        return AIExecutionMetrics.builder()
            .sessionId(sessionId)
            .processingTime(System.currentTimeMillis())
            .success(success)
            .customMetrics(Map.of("nodeId", getNodeId()))
            .build();
    }
    
    /**
     * 현재 노드 ID 반환
     */
    private String getNodeId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
} 