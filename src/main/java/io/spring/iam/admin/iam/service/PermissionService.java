package io.spring.iam.admin.iam.service;

import io.spring.iam.domain.dto.PermissionDto;
import io.spring.iam.domain.entity.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionService {
    Permission createPermission(Permission permission);
    Optional<Permission> getPermission(Long id);
    List<Permission> getAllPermissions();
    void deletePermission(Long id);
    Permission updatePermission(Long id, PermissionDto permissionDto);
    Optional<Permission> findByName(String name);
}
