package io.spring.iam.aiam.operations;

import io.spring.aicore.pipeline.UniversalPipeline;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.iam.aiam.session.AIStrategySessionRepository;
import io.spring.iam.aiam.session.AIStrategySessionRepository.AIStrategyExecutionPhase;
import io.spring.iam.aiam.session.AIStrategySessionRepository.AIExecutionResult;
import io.spring.iam.aiam.session.AIStrategySessionRepository.AIExecutionMetrics;
import io.spring.iam.redis.DistributedAIStrategyCoordinator;
import io.spring.redis.RedisEventPublisher;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

/**
 * 분산 전략 실행을 담당하는 전용 서비스
 * 마스터 브레인의 지휘 하에 구체적인 실행을 담당
 */
@Slf4j
@Service
public class DistributedStrategyExecutor<T extends IAMContext> {
    
    private final UniversalPipeline<T> pipeline;
    private final AIStrategySessionRepository sessionRepository;
    private final DistributedAIStrategyCoordinator strategyCoordinator;
    private final RedisEventPublisher eventPublisher;
    private final IAMTypeConverter typeConverter;
    
    @Autowired
    public DistributedStrategyExecutor(UniversalPipeline<T> pipeline,
                                     AIStrategySessionRepository sessionRepository,
                                     DistributedAIStrategyCoordinator strategyCoordinator,
                                     RedisEventPublisher eventPublisher,
                                     IAMTypeConverter typeConverter) {
        this.pipeline = pipeline;
        this.sessionRepository = sessionRepository;
        this.strategyCoordinator = strategyCoordinator;
        this.eventPublisher = eventPublisher;
        this.typeConverter = typeConverter;
    }
    
    /**
     * 분산 전략 실행
     */
    public <R extends IAMResponse> R executeDistributedStrategy(IAMRequest<T> request, 
                                                               Class<R> responseType,
                                                               String sessionId, 
                                                               String auditId) {
        try {
            // Phase 1: LAB_ALLOCATION
            updateSessionState(sessionId, AIStrategyExecutionPhase.LAB_ALLOCATION, Map.of(
                "auditId", auditId,
                "requestType", request.getClass().getSimpleName()
            ));
            
            LabExecutionStrategy labStrategy = createLabExecutionStrategy(request, sessionId);
            
            // Phase 2: EXECUTING
            updateSessionState(sessionId, AIStrategyExecutionPhase.EXECUTING, Map.of(
                "labStrategy", labStrategy.getStrategyName(),
                "expectedDuration", labStrategy.getExpectedDuration()
            ));
            
            // Phase 3: 실제 AI 파이프라인 실행
            R result = executeAIPipeline(request, responseType, sessionId);
            
            // Phase 4: VALIDATING
            updateSessionState(sessionId, AIStrategyExecutionPhase.VALIDATING, Map.of(
                "resultType", result.getClass().getSimpleName(),
                "validationStartTime", System.currentTimeMillis()
            ));
            
            validateResult(result, sessionId);
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Distributed strategy execution failed for session: {}", sessionId, e);
            throw new IAMOperationException("Strategy execution failed", e);
        }
    }
    
    /**
     * AI 파이프라인 실행
     */
    private <R extends IAMResponse> R executeAIPipeline(IAMRequest<T> request, 
                                                       Class<R> responseType, 
                                                       String sessionId) {
        try {
            // 실제 AI 파이프라인 실행 (현재는 Mock 응답)
            return createMockResponse(request, responseType, sessionId);
            
        } catch (Exception e) {
            log.error("❌ AI Pipeline execution failed for session: {}", sessionId, e);
            throw new IAMOperationException("AI Pipeline execution failed", e);
        }
    }
    
    /**
     * Mock 응답 생성 (실제 AI 통합 전까지 사용)
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
        return System.getProperty("node.id", "node-" + UUID.randomUUID().toString().substring(0, 8));
    }
} 