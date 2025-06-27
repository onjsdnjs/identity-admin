package io.spring.aicore.pipeline;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.aicore.protocol.DomainContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * UniversalPipeline의 기본 구현체
 * 
 * 🎯 실제 AI 파이프라인 실행을 담당
 * - 단계별 파이프라인 실행
 * - 에러 핸들링 및 복구
 * - 메트릭 수집 및 모니터링
 * - Spring Bean으로 등록되어 의존성 주입 지원
 */
@Slf4j
@Component
public class DefaultUniversalPipeline<T extends DomainContext> implements UniversalPipeline<T> {
    
    private final AtomicReference<PipelineStatus> currentStatus = new AtomicReference<>(PipelineStatus.READY);
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong successfulExecutions = new AtomicLong(0);
    private final AtomicLong failedExecutions = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final AtomicLong activeExecutions = new AtomicLong(0);
    
    @Override
    public <R extends AIResponse> Mono<R> execute(AIRequest<T> request, 
                                                 PipelineConfiguration<T> config,
                                                 PipelineExecutor<T, R> executor) {
        log.info("🚀 Universal Pipeline: Starting execution for request {}", request.getRequestId());
        
        long startTime = System.currentTimeMillis();
        totalExecutions.incrementAndGet();
        activeExecutions.incrementAndGet();
        currentStatus.set(PipelineStatus.RUNNING);
        
        return Mono.fromCallable(() -> {
                try {
                    // 1. 파이프라인 실행 컨텍스트 생성
                    PipelineExecutionContext context = createExecutionContext(request, config);
                    
                    // 2. 각 단계별 실행
                    for (PipelineConfiguration.PipelineStep step : config.getSteps()) {
                        log.debug("🔄 Executing pipeline step: {}", step);
                        Object stepResult = executor.executeStep(request, step, context).block();
                        context.addStepResult(step, stepResult);
                    }
                    
                    // 3. 최종 응답 생성
                    R finalResponse = executor.buildFinalResponse(request, context).block();
                    
                    // 4. 성공 처리
                    long executionTime = System.currentTimeMillis() - startTime;
                    recordSuccess(executionTime);
                    
                    log.info("✅ Universal Pipeline: Execution completed for request {} in {}ms", 
                            request.getRequestId(), executionTime);
                    
                    return finalResponse;
                    
                } catch (Exception e) {
                    long executionTime = System.currentTimeMillis() - startTime;
                    recordFailure(executionTime, e);
                    
                    log.error("❌ Universal Pipeline: Execution failed for request {} after {}ms", 
                             request.getRequestId(), executionTime, e);
                    
                    throw new PipelineExecutionException("Pipeline execution failed", e);
                }
            })
            .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
            .doFinally(signal -> {
                activeExecutions.decrementAndGet();
                if (activeExecutions.get() == 0) {
                    currentStatus.set(PipelineStatus.READY);
                }
            })
            .onErrorMap(throwable -> {
                currentStatus.set(PipelineStatus.FAILED);
                return new PipelineExecutionException("Pipeline execution error", throwable);
            });
    }
    
    @Override
    public PipelineStatus getStatus() {
        return currentStatus.get();
    }
    
    @Override
    public void abort() {
        log.warn("⚠️ Universal Pipeline: Aborting pipeline execution");
        currentStatus.set(PipelineStatus.ABORTED);
    }
    
    @Override
    public Mono<PipelineMetrics> getMetrics() {
        return Mono.fromCallable(() -> {
            long total = totalExecutions.get();
            long successful = successfulExecutions.get();
            long failed = failedExecutions.get();
            double avgTime = total > 0 ? (double) totalExecutionTime.get() / total : 0.0;
            long active = activeExecutions.get();
            
            return new PipelineMetrics(total, successful, failed, avgTime, active);
        });
    }
    
    /**
     * 파이프라인 실행 컨텍스트를 생성합니다
     */
    private PipelineExecutionContext createExecutionContext(AIRequest<T> request, 
                                                           PipelineConfiguration<T> config) {
        return new PipelineExecutionContext(request.getRequestId(), config.getParameters());
    }
    
    /**
     * 성공 실행을 기록합니다
     */
    private void recordSuccess(long executionTime) {
        successfulExecutions.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime);
        currentStatus.set(PipelineStatus.COMPLETED);
    }
    
    /**
     * 실패 실행을 기록합니다
     */
    private void recordFailure(long executionTime, Exception error) {
        failedExecutions.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime);
        currentStatus.set(PipelineStatus.FAILED);
        
        log.error("📊 Pipeline failure recorded - Error: {}, ExecutionTime: {}ms", 
                 error.getClass().getSimpleName(), executionTime);
    }
    
    /**
     * 파이프라인 실행 예외
     */
    public static class PipelineExecutionException extends RuntimeException {
        public PipelineExecutionException(String message) {
            super(message);
        }
        
        public PipelineExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 