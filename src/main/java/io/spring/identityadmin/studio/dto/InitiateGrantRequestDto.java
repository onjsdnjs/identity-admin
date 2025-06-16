package io.spring.identityadmin.studio.dto;

import java.util.Set;

/**
 * Studio 에서 권한 부여 마법사 시작을 요청하기 위한 DTO 입니다.
 */
public record InitiateGrantRequestDto(
        Set<Long> userIds,
        Set<Long> groupIds,
        Set<Long> permissionIds
) {}
