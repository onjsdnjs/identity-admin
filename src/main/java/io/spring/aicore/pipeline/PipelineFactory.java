package io.spring.aicore.pipeline;

public interface PipelineFactory<T extends DomainContext> {
    <R extends AIResponse> UniversalPipeline<T, R> createPipeline(PipelineConfig<T, R> config);
    <R extends AIResponse> StreamingUniversalPipeline<T, R> createStreamingPipeline(PipelineConfig<T, R> config);
}
