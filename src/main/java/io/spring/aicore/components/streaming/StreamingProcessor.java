package io.spring.aicore.components.streaming;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 스트리밍 처리기
 * 
 * 📡 현재 하드코딩된 복잡한 스트리밍 로직을 체계화
 * - JSON 블록 감지 및 버퍼링
 * - 실시간 텍스트 스트리밍
 * - 에러 처리 및 복구
 */
@Slf4j
@Component
public class StreamingProcessor {
    
    /**
     * AI 모델로부터 스트리밍 응답을 처리합니다 (현재 하드코딩된 로직과 동일)
     * 
     * @param chatModel AI 채팅 모델
     * @param prompt 생성된 프롬프트
     * @return 처리된 스트리밍 응답
     */
    public Flux<String> processStream(ChatModel chatModel, Prompt prompt) {
        
        // 텍스트 버퍼와 JSON 감지 상태 관리 (현재 하드코딩된 로직과 동일)
        AtomicReference<StringBuilder> textBuffer = new AtomicReference<>(new StringBuilder());
        AtomicBoolean jsonStarted = new AtomicBoolean(false);
        AtomicBoolean jsonEnded = new AtomicBoolean(false);
        AtomicReference<StringBuilder> jsonBuffer = new AtomicReference<>(new StringBuilder());

        return chatModel.stream(prompt)
                .filter(Objects::nonNull)
                .filter(chatResponse -> chatResponse.getResult() != null)
                .filter(chatResponse -> chatResponse.getResult().getOutput() != null)
                .map(chatResponse -> {
                    try {
                        String content = chatResponse.getResult().getOutput().getText();
                        return content != null ? content : "";
                    } catch (Exception e) {
                        log.warn("🔥 컨텐츠 추출 실패: {}", e.getMessage());
                        return "";
                    }
                })
                .filter(content -> !content.isEmpty())
                .map(this::cleanTextChunk)
                .filter(chunk -> !chunk.trim().isEmpty())
                .flatMap(chunk -> processChunk(chunk, textBuffer, jsonStarted, jsonEnded, jsonBuffer))
                .filter(text -> !text.isEmpty())
                .doOnNext(chunk -> {
                    if (chunk.contains("===JSON시작===")) {
                        log.debug("🔥 JSON 블록 시작 감지");
                    }
                    if (chunk.contains("===JSON끝===")) {
                        log.debug("🔥 JSON 블록 완료");
                    }
                })
                .doOnError(error -> log.error("🔥 AI 스트리밍 오류", error))
                .onErrorResume(error -> {
                    log.error("🔥 AI 스트리밍 실패, 에러 메시지 반환", error);
                    return Flux.just("ERROR: AI 서비스 연결 실패: " + error.getMessage());
                });
    }
    
    /**
     * 개별 청크를 처리합니다 (현재 하드코딩된 복잡한 로직과 동일)
     */
    private Flux<String> processChunk(String chunk, 
                                     AtomicReference<StringBuilder> textBuffer,
                                     AtomicBoolean jsonStarted, 
                                     AtomicBoolean jsonEnded,
                                     AtomicReference<StringBuilder> jsonBuffer) {
        
        // 버퍼에 청크 추가
        textBuffer.get().append(chunk);

        // JSON 시작 감지
        if (!jsonStarted.get() && textBuffer.get().toString().contains("===JSON시작===")) {
            jsonStarted.set(true);
            int startIndex = textBuffer.get().toString().indexOf("===JSON시작===");

            // JSON 시작 전의 텍스트 반환
            String beforeJson = textBuffer.get().substring(0, startIndex);

            // JSON 부분만 버퍼에 남기기
            String afterJsonMarker = textBuffer.get().substring(startIndex + "===JSON시작===".length());
            textBuffer.set(new StringBuilder(afterJsonMarker));
            jsonBuffer.set(new StringBuilder());

            return Flux.just(beforeJson);
        }

        // JSON 수집 중
        if (jsonStarted.get() && !jsonEnded.get()) {
            String currentText = textBuffer.get().toString();

            // JSON 종료 감지
            if (currentText.contains("===JSON끝===")) {
                jsonEnded.set(true);
                int endIndex = currentText.indexOf("===JSON끝===");

                // JSON 컨텐츠 추출
                String jsonContent = currentText.substring(0, endIndex);
                jsonBuffer.get().append(jsonContent);

                // 완전한 JSON 반환
                String completeJson = "===JSON시작===" + jsonBuffer.get().toString() + "===JSON끝===";

                // 남은 텍스트 처리
                String afterJson = currentText.substring(endIndex + "===JSON끝===".length());

                if (!afterJson.trim().isEmpty()) {
                    return Flux.just(completeJson, afterJson);
                } else {
                    return Flux.just(completeJson);
                }
            } else {
                // JSON 버퍼에 추가하고 빈 응답 반환 (JSON이 완성될 때까지 대기)
                jsonBuffer.get().append(currentText);
                textBuffer.set(new StringBuilder());
                return Flux.empty();
            }
        }

        // 일반 텍스트 스트리밍
        if (!jsonStarted.get()) {
            String content = textBuffer.get().toString();
            textBuffer.set(new StringBuilder());
            return Flux.just(content);
        }

        // JSON 종료 후 텍스트
        if (jsonEnded.get()) {
            String content = textBuffer.get().toString();
            textBuffer.set(new StringBuilder());
            return Flux.just(content);
        }

        return Flux.empty();
    }
    
    /**
     * 텍스트 청크 정제 (현재 하드코딩된 로직과 동일)
     */
    private String cleanTextChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return "";
        }

        try {
            // UTF-8 인코딩 안정성 검증
            byte[] bytes = chunk.getBytes(StandardCharsets.UTF_8);
            String decoded = new String(bytes, StandardCharsets.UTF_8);

            // 불필요한 제어 문자만 제거 (한글은 보존)
            String cleaned = decoded.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

            return cleaned;
        } catch (Exception e) {
            log.warn("🔥 텍스트 청크 정제 실패: {}", e.getMessage());
            return chunk;
        }
    }
} 