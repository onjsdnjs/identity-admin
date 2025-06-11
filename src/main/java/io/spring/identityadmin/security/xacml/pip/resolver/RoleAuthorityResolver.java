package io.spring.identityadmin.security.xacml.pip.resolver;

import io.spring.identityadmin.domain.entity.Role;
import io.spring.identityadmin.security.xacml.pip.repository.RoleRepository;
import io.spring.identityadmin.security.core.auth.PermissionAuthority;
import io.spring.identityadmin.security.core.auth.RoleAuthority;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RoleAuthorityResolver implements SubjectAuthorityResolver {
    private final RoleRepository roleRepository;

    @Override
    public boolean supports(String subjectType) {
        return "ROLE".equalsIgnoreCase(subjectType);
    }

    @Override
    public Set<GrantedAuthority> resolveAuthorities(Long subjectId) {
        Role role = roleRepository.findByIdWithPermissions(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + subjectId));

        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new RoleAuthority(role));
        role.getRolePermissions().forEach(rp -> authorities.add(new PermissionAuthority(rp.getPermission())));

        return authorities;
    }
}
