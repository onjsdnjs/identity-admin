package io.spring.identityadmin.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * '자연어 정책 생성' API 요청을 위한 DTO.
 */
public record NaturalLanguageQueryDto(
        @NotBlank(message = "분석할 텍스트를 입력해야 합니다.")
        String naturalLanguageQuery
) {}
