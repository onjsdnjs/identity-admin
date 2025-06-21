package io.spring.identityadmin.domain.dto;

import java.util.Set;

public record ConditionTemplateDto(
        Long id,
        String name,
        String description,
        Set<String> requiredVariables,
        boolean isCompatible // 현재 컨텍스트와 호환되는지 여부
) {}
