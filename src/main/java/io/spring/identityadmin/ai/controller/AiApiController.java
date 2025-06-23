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

        // 서비스 계층에서 Reactor의 Flux (반응형 스트림)를 반환받습니다.
        Flux<String> stream = aINativeIAMAdvisor.generatePolicyFromTextStream(request.naturalLanguageQuery());

        return outputStream -> {
            stream
                    // 스트림에서 각 청크(chunk)가 도착할 때마다 outputStream에 씁니다.
                    .doOnNext(chunk -> {
                        try {
                            outputStream.write(chunk.getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
                        } catch (IOException e) {
                            log.error("스트리밍 중 오류 발생", e);
                            throw new RuntimeException(e);
                        }
                    })
                    // 스트림이 완료되거나 오류 발생 시 outputStream을 닫습니다.
                    .doOnComplete(outputStream::close)
                    .doOnError(throwable -> {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            log.error("스트림 오류 후 종료 중 오류", e);
                        }
                    })
                    .subscribe(); // 스트림 구독 시작
        };
    }
}
