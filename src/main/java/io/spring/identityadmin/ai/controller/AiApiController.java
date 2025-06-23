package io.spring.identityadmin.ai.controller;

import io.spring.identityadmin.ai.AINativeIAMAdvisor;
import io.spring.identityadmin.ai.dto.NaturalLanguageQueryDto;
import io.spring.identityadmin.domain.dto.AiGeneratedPolicyDraftDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * [신규] AI 기능과 관련된 모든 API 요청을 처리하는 컨트롤러.
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiApiController {

    private final AINativeIAMAdvisor aINativeIAMAdvisor;

    /**
     * [구현 완료] '지능형 정책 빌더'의 자연어 입력값을 받아,
     * AI를 통해 분석된 정책 초안 DTO를 반환합니다.
     */
    @PostMapping(value = "/policies/generate-from-text/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody generatePolicyFromTextStream(@Valid @RequestBody NaturalLanguageQueryDto request) {
        log.info("AI 정책 초안 스트리밍 생성 요청 수신: \"{}\"", request.naturalLanguageQuery());

        Flux<String> stream = aINativeIAMAdvisor.generatePolicyFromTextStream(request.naturalLanguageQuery());

        return outputStream -> {
            stream
                    .doOnNext(chunk -> {
                        try {
                            outputStream.write(chunk.getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
                        } catch (IOException e) {
                            log.error("스트리밍 중 오류 발생", e);
                            // 클라이언트와의 연결이 끊어졌을 가능성이 높으므로, 스트림을 중단하기 위해 RuntimeException을 던집니다.
                            throw new RuntimeException("Error while streaming AI response", e);
                        }
                    })
                    // [핵심 수정] doOnComplete와 doOnError에서 IOException을 try-catch로 처리합니다.
                    .doOnComplete(() -> {
                        try {
                            log.info("AI 응답 스트림 전송 완료.");
                            outputStream.close();
                        } catch (IOException e) {
                            log.error("스트림 완료 후 종료 중 오류 발생", e);
                        }
                    })
                    .doOnError(throwable -> {
                        try {
                            log.error("AI 응답 스트림 중 에러 발생", throwable);
                            outputStream.close();
                        } catch (IOException e) {
                            log.error("스트림 오류 후 종료 중 추가 오류 발생", e);
                        }
                    })
                    .subscribe(); // 리액티브 스트림 구독 시작
        };
    }
}
