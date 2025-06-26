package io.spring.aicore.infrastructure.llm;

public interface LLMClient {
    Mono<LLMResponse> send(LLMRequest request);
    Flux<String> sendStream(LLMRequest request);
    boolean isHealthy();
    LLMCapabilities getCapabilities();
}
