package io.spring.identityadmin.aiam.labs.policy;

import reactor.core.publisher.Flux;

public interface PolicyGenerationStrategy {
    boolean supports(IAMContext context);
    Flux<String> generateStream(String query, IAMContext context);
}
