package io.spring.identityadmin.service.dto;

/**
 * AI가 특정 사용자에게 추천하는 역할 정보를 담는 DTO.
 */
public record RecommendedRoleDto(
        Long roleId,
        String roleName,
        String reason, // 추천 이유 (예: "동일 부서 동료 90%가 보유")
        double confidence // 추천 신뢰도 (0.0 ~ 1.0)
) {}
