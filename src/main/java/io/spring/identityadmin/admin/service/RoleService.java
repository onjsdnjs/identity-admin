package io.spring.identityadmin.admin.service;


import io.spring.identityadmin.entity.Role;

import java.util.List;

public interface RoleService {
    Role getRole(long id);
    List<Role> getRoles();
    List<Role> getRolesWithoutExpression();
    Role createRole(Role role, List<Long> permissionIds);
    Role updateRole(Role role, List<Long> permissionIds);
    void deleteRole(long id);
}
