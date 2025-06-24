package io.spring.identityadmin.ai.controller;

import io.spring.identityadmin.ai.AINativeIAMAdvisor;
import io.spring.identityadmin.ai.dto.ConditionValidationRequest;
import io.spring.identityadmin.ai.dto.ConditionValidationResponse;
import io.spring.identityadmin.domain.dto.AiGeneratedPolicyDraftDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;
// 기존 AiApiController를 참고하여 스트리밍 메서드를 추가하는 예시

@RestController
@RequestMapping("/api/ai/policies")
@RequiredArgsConstructor
@Slf4j
public class AiApiController {

    private final AINativeIAMAdvisor aiNativeIAMAdvisor;

    /**
     * AI로 정책 초안을 스트리밍 방식으로 생성합니다.
     * 기존 AiApiController의 패턴을 따라 구현합니다.
     */
    @PostMapping(value = "/generate-from-text/stream", produces = "text/event-stream; charset=UTF-8")
    public Flux<ServerSentEvent<String>> generatePolicyFromTextStream(@RequestBody Map<String, String> request) {

        String naturalLanguageQuery = request.get("naturalLanguageQuery");
        if (naturalLanguageQuery == null || naturalLanguageQuery.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("naturalLanguageQuery is required"));
        }

        log.info("🔥 AI 스트리밍 정책 생성 요청: {}", naturalLanguageQuery);

        try {
            return aiNativeIAMAdvisor.generatePolicyFromTextStream(naturalLanguageQuery)
                    .map(chunk -> ServerSentEvent.<String>builder()
                            .data(chunk)
                            .build())
                    .concatWith(Flux.just(ServerSentEvent.<String>builder()
                            .data("[DONE]")
                            .build()))
                    .onErrorResume(error -> {
                        log.error("🔥 스트리밍 중 오류 발생", error);
                        return Flux.just(ServerSentEvent.<String>builder()
                                .data("ERROR: " + error.getMessage())
                                .build());
                    });

        } catch (Exception e) {
            log.error("🔥 AI 스트리밍 정책 생성 실패", e);
            return Flux.error(e);
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

    @PostMapping("/policies/validate-condition")
    public ResponseEntity<ConditionValidationResponse> validateCondition(@RequestBody ConditionValidationRequest request) {
        ConditionValidationResponse response = aiNativeIAMAdvisor.validateCondition(
                request.resourceIdentifier(), request.conditionSpel()
        );
        return ResponseEntity.ok(response);
    }
}