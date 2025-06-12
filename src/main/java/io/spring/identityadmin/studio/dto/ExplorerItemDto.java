package io.spring.identityadmin.studio.dto;

/**
 * Authorization Studio의 Explorer 패널에 표시될 아이템을 위한 DTO 입니다.
 * 주체, 리소스(권한), 정책 등 다양한 타입의 아이템을 공통적으로 표현합니다.
 */
public record ExplorerItemDto(
        Long id,
        String name,
        String type, // "USER", "GROUP", "PERMISSION", "POLICY"
        String description
) {}
