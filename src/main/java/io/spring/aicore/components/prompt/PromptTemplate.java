package io.spring.aicore.components.prompt;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.DomainContext;

/**
 * 프롬프트 템플릿 인터페이스
 */
public interface PromptTemplate {
    String generateSystemPrompt(AIRequest<? extends DomainContext> request, String systemMetadata);
    String generateUserPrompt(AIRequest<? extends DomainContext> request, String contextInfo);
}
