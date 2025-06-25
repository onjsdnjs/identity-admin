package io.spring.identityadmin.ai.controller;

import io.spring.identityadmin.ai.AINativeIAMAdvisor;
import io.spring.identityadmin.ai.AINativeIAMSynapseArbiterFromOllama;
import io.spring.identityadmin.ai.dto.ConditionValidationRequest;
import io.spring.identityadmin.ai.dto.ConditionValidationResponse;
import io.spring.identityadmin.domain.dto.AiGeneratedPolicyDraftDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
// 기존 AiApiController를 참고하여 스트리밍 메서드를 추가하는 예시

@RestController
@RequestMapping("/api/ai/policies")
@RequiredArgsConstructor
@Slf4j
public class AiApiController {

    private final AINativeIAMSynapseArbiterFromOllama aiNativeIAMAdvisor;

    /**
     * AI로 정책 초안을 스트리밍 방식으로 생성합니다.
     * Server-Sent Events (SSE) 형식으로 응답을 스트리밍합니다.
     */
    @PostMapping(value = "/generate-from-text/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generatePolicyFromTextStream(@RequestBody Map<String, String> request) {

        String naturalLanguageQuery = request.get("naturalLanguageQuery");
        if (naturalLanguageQuery == null || naturalLanguageQuery.trim().isEmpty()) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .data("ERROR: naturalLanguageQuery is required")
                    .build());
        }

        log.info("🔥 AI 스트리밍 정책 생성 요청: {}", naturalLanguageQuery);

        try {
            return aiNativeIAMAdvisor.generatePolicyFromTextStream(naturalLanguageQuery)
                    .map(chunk -> {
                        // 청크를 SSE 형식으로 변환
                        return ServerSentEvent.<String>builder()
                                .data(chunk)
                                .build();
                    })
                    .concatWith(
                            // 스트림 종료 시그널
                            Mono.just(ServerSentEvent.<String>builder()
                                    .data("[DONE]")
                                    .build())
                    )
                    .onErrorResume(error -> {
                        log.error("🔥 스트리밍 중 오류 발생", error);
                        return Flux.just(ServerSentEvent.<String>builder()
                                .data("ERROR: " + error.getMessage())
                                .build());
                    });

        } catch (Exception e) {
            log.error("🔥 AI 스트리밍 정책 생성 실패", e);
            return Flux.just(ServerSentEvent.<String>builder()
                    .data("ERROR: " + e.getMessage())
                    .build());
        }
    }

    /**
     * AI로 정책 초안을 일반 방식으로 생성합니다 (fallback용).
     */
    @PostMapping("/generate-from-text")
    public ResponseEntity<AiGeneratedPolicyDraftDto> generatePolicyFromText(
            @RequestBody Map<String, String> request) {

        String naturalLanguageQuery = request.get("naturalLanguageQuery");
        if (naturalLanguageQuery == null || naturalLanguageQuery.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("AI 정책 생성 요청: {}", naturalLanguageQuery);

        try {
            AiGeneratedPolicyDraftDto result = aiNativeIAMAdvisor.generatePolicyFromTextByAi(naturalLanguageQuery);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("AI 정책 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    @PostMapping("/validate-condition")
    public ResponseEntity<ConditionValidationResponse> validateCondition(@RequestBody ConditionValidationRequest request) {
        ConditionValidationResponse response = aiNativeIAMAdvisor.validateCondition(
                request.resourceIdentifier(), request.conditionSpel()
        );
        return ResponseEntity.ok(response);
    }
}