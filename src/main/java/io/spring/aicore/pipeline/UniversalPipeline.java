package io.spring.aicore.pipeline;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.aicore.protocol.DomainContext;
import reactor.core.publisher.Mono;

/**
 * 범용 AI 처리 파이프라인
 * 
 * 🎯 모든 AI 작업의 표준 처리 흐름을 정의
 * - 요청 전처리 → 컨텍스트 검색 → 프롬프트 생성 → LLM 호출 → 응답 파싱 → 후처리
 * - 각 단계별 컴포넌트 조합을 통한 유연한 파이프라인 구성
 * 
 * @param <T> 도메인 컨텍스트 타입
 */
public interface UniversalPipeline<T extends DomainContext> {
    
    /**
     * 파이프라인을 실행합니다
     * @param request AI 요청
     * @param config 파이프라인 설정
     * @param executor 실행 담당 컴포넌트
     * @return AI 응답
     */
    <R extends AIResponse> Mono<R> execute(AIRequest<T> request, 
                                          PipelineConfiguration<T> config,
                                          PipelineExecutor<T, R> executor);
    
    /**
     * 파이프라인 상태를 확인합니다
     * @return 파이프라인 상태
     */
    PipelineStatus getStatus();
    
    /**
     * 파이프라인을 중단합니다
     */
    void abort();
    
    /**
     * 파이프라인 메트릭을 조회합니다
     * @return 파이프라인 메트릭
     */
    Mono<PipelineMetrics> getMetrics();
    
    /**
     * 파이프라인 상태 열거형
     */
    enum PipelineStatus {
        READY,          // 준비 상태
        RUNNING,        // 실행 중
        COMPLETED,      // 완료
        FAILED,         // 실패
        ABORTED         // 중단됨
    }
    
    /**
     * 파이프라인 메트릭 정보
     */
    record PipelineMetrics(
        long totalExecutions,
        long successfulExecutions, 
        long failedExecutions,
        double averageExecutionTime,
        long activeExecutions
    ) {
        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 0.0;
        }
    }
} 