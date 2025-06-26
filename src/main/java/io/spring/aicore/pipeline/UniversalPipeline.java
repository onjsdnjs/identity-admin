package io.spring.aicore.pipeline;

public interface UniversalPipeline<T extends DomainContext, R extends AIResponse> {
    Mono<R> execute(AIRequest<T> request, Class<R> responseType);
    Flux<R> executeStream(AIRequest<T> request, Class<R> responseType);

    // 파이프라인 체이닝 지원
    <U extends AIResponse> UniversalPipeline<T, U> then(PipelineStage<R, U> nextStage);
}
