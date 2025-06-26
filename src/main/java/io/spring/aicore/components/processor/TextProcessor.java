package io.spring.aicore.components.processor;

public interface TextProcessor {
    String clean(String text);
    String normalize(String text);
    Encoding detectEncoding(String text);
}
