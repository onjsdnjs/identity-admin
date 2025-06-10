package io.spring.identityadmin.domain.dto;

import lombok.Data;

@Data
public class RoleListDto {
    private Long id;
    private String roleName;
    private String roleDesc;
    private int permissionCount;
}
