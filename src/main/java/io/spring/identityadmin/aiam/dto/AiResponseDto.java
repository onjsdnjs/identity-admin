package io.spring.identityadmin.aiam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.spring.identityadmin.domain.entity.policy.Policy;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI의 JSON 응답을 유연하게 받기 위한 중간 DTO.
 * ID 필드를 Object 타입으로 받아, 숫자와 문자열을 모두 처리할 수 있습니다.
 */
public record AiResponseDto(
        @JsonProperty("policyName") String policyName,
        @JsonProperty("description") String description,
        @JsonProperty("roleIds") Set<Object> roleIds, // 숫자 또는 문자열
        @JsonProperty("permissionIds") Set<Object> permissionIds, // 숫자 또는 문자열
        @JsonProperty("conditional") boolean conditional,
        @JsonProperty("conditions") Map<String, List<String>> conditions, // templateId는 문자열일 수 있음
        @JsonProperty("aiRiskAssessmentEnabled") boolean aiRiskAssessmentEnabled,
        @JsonProperty("requiredTrustScore") double requiredTrustScore,
        @JsonProperty("customConditionSpel") String customConditionSpel,
        @JsonProperty("effect") Policy.Effect effect
) {}
