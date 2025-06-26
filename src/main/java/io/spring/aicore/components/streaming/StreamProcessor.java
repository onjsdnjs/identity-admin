package io.spring.aicore.components.streaming;

public interface StreamProcessor {
    Flux<String> processStream(Flux<String> aiStream, StreamConfig config);
    Flux<String> extractJsonBlocks(Flux<String> stream, String startMarker, String endMarker);
}
