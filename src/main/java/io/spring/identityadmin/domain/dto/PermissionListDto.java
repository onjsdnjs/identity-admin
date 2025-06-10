package io.spring.identityadmin.domain.dto;

import lombok.Data;
@Data
public class PermissionListDto {
    private Long id;
    private String name;
    private String description;
    private String targetType;
    private String actionType;
}