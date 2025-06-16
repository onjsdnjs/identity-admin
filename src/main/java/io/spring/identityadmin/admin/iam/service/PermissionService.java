package io.spring.identityadmin.admin.iam.service;

import io.spring.identityadmin.domain.dto.PermissionDto;
import io.spring.identityadmin.domain.entity.Permission;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PermissionService {
    Permission createPermission(Permission permission);
    Optional<Permission> getPermission(Long id);
    List<Permission> getAllPermissions();
    void deletePermission(Long id);
    Permission updatePermission(Long id, PermissionDto permissionDto);
    Optional<Permission> findByName(String name);
}
