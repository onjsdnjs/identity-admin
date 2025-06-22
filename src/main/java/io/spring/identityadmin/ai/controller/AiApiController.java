package io.spring.identityadmin.ai.controller;

import io.spring.identityadmin.ai.AINativeIAMAdvisor;
import io.spring.identityadmin.ai.dto.NaturalLanguageQueryDto;
import io.spring.identityadmin.domain.dto.AiGeneratedPolicyDraftDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/policies/generate-from-text")
    public ResponseEntity<AiGeneratedPolicyDraftDto> generatePolicyFromText(
            @Valid @RequestBody NaturalLanguageQueryDto request) {

        log.info("AI 정책 초안 생성 요청 수신: \"{}\"", request.naturalLanguageQuery());

        try {
            AiGeneratedPolicyDraftDto policyDraft = aINativeIAMAdvisor.generatePolicyFromText(request.naturalLanguageQuery());
            return ResponseEntity.ok(policyDraft);
        } catch (Exception e) {
            log.error("AI 정책 초안 생성 중 오류 발생", e);
            // 더 구체적인 오류 응답을 반환하도록 GlobalExceptionHandler 사용을 권장
            throw new IllegalStateException("AI 정책 초안 생성에 실패했습니다: " + e.getMessage());
        }
    }
}
