package io.spring.identityadmin.domain.dto;

import java.util.Map;

public record AiGeneratedPolicyDraftDto(
        BusinessPolicyDto policyData,
        Map<String, String> subjectIdToNameMap,
        Map<String, String> permissionIdToNameMap,
        Map<String, String> conditionIdToNameMap
) {}