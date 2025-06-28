package io.spring.aicore.components.parser;

public interface ResponseParser {

    <T> T parse(String jsonResponse, T target);
    <T> T parseToType(String jsonStr, Class<T> targetType);
    String extractAndCleanJson(String aiResponse);
}
