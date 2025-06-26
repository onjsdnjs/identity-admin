package io.spring.aicore.components.retriever;

public interface ContextRetriever {
    Mono<ContextData> retrieve(DomainContext context);
    boolean isApplicable(DomainContext context);
    String getCacheKey(DomainContext context);
    int getPriority();
}