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
     * 자연어 정책 생성 요청을 스트리밍 방식으로 처리합니다.
     * 메서드의 반환 타입을 Flux<String>으로 직접 지정하여, Spring WebFlux가
     * 스트림의 생명주기를 안전하게 관리하도록 위임합니다.
     */
    @PostMapping(value = "/policies/generate-from-text/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generatePolicyFromTextStream(@Valid @RequestBody NaturalLanguageQueryDto request) {
        log.info("AI 정책 초안 스트리밍 생성 요청 수신: \"{}\"", request.naturalLanguageQuery());

        try {
            // 서비스 계층에서 반환된 Flux<String> 스트림을 그대로 반환합니다.
            return aINativeIAMAdvisor.generatePolicyFromTextStream(request.naturalLanguageQuery())
                    .doOnError(error -> log.error("AI 응답 스트림 중 에러 발생", error))
                    .doOnComplete(() -> log.info("AI 응답 스트림 전송 완료."));
        } catch (Exception e) {
            log.error("AI 스트리밍 서비스 호출 중 즉시 오류 발생", e);
            // 즉각적인 오류 발생 시, 에러 메시지를 담은 단일 스트림을 반환합니다.
            return Flux.just("AI 서비스 호출에 실패했습니다: " + e.getMessage());
        }
    }
}
