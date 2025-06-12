package io.spring.identityadmin.security.xacml.pap.dto;

/**
 * 정책 간의 충돌 정보를 담는 DTO 입니다.
 */
public record PolicyConflictDto(
        Long newPolicyId,
        Long existingPolicyId,
        String conflictDescription // 예: "동일한 리소스에 대해 ALLOW와 DENY 정책이 충돌합니다."
) {}
