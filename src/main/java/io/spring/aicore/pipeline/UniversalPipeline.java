package io.spring.aicore.pipeline;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.aicore.protocol.DomainContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 범용 AI 처리 파이프라인
 * 
 * 🎯 모든 AI 작업의 표준 처리 흐름을 정의
 * - 요청 전처리 → 컨텍스트 검색 → 프롬프트 생성 → LLM 호출 → 응답 파싱 → 후처리
 * - 각 단계별 컴포넌트 조합을 통한 유연한 파이프라인 구성
 */
public interface UniversalPipeline {
    
    /**
     * 파이프라인을 실행합니다
     */
    <T extends DomainContext, R extends AIResponse> Mono<R> execute(
            AIRequest<T> request, 
            PipelineConfiguration configuration, 
            Class<R> responseType);
    
    /**
     * 스트리밍 파이프라인을 실행합니다
     */
    <T extends DomainContext> Flux<String> executeStream(
            AIRequest<T> request, 
            PipelineConfiguration configuration);
    
    /**
     * 설정을 지원하는지 확인합니다
     */
    boolean supportsConfiguration(PipelineConfiguration configuration);
    
    /**
     * 파이프라인 메트릭을 조회합니다
     */
    PipelineMetrics getMetrics();
    
    /**
     * 파이프라인 메트릭 정보
     */
    record PipelineMetrics(
        String pipelineName,
        String version,
        long timestamp,
        Map<String, Object> metrics
    ) {
        public Object getMetric(String key) {
            return metrics.get(key);
        }
    }
} 