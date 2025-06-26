package io.spring.aicore.components.prompt;

public interface PromptEngine {
    String generatePrompt(PromptTemplate template, DomainContext context);
    String optimizePrompt(String prompt, Feedback feedback);
    ValidationResult validatePrompt(String prompt);
    Set<TemplateFormat> getSupportedFormats();
}
