package io.spring.identityadmin.domain.dto;

import lombok.Data;
@Data
public class RoleHierarchyListDto {
    private Long id;
    private String description;
    private String hierarchyString;
    private boolean isActive;
}
