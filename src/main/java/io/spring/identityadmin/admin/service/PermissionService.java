package io.spring.identityadmin.admin.service;

import io.springsecurity.springsecurity6x.entity.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionService {
    Permission createPermission(Permission permission);
    Optional<Permission> getPermission(Long id);
    List<Permission> getAllPermissions();
    void deletePermission(Long id);
    Permission updatePermission(Permission permission);
    Optional<Permission> findByName(String name);
}
