package io.spring.identityadmin.security.xacml.pip.resolver;

import io.spring.identityadmin.domain.entity.Users;
import io.spring.identityadmin.repository.UserRepository;
import io.spring.identityadmin.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserAuthorityResolver implements SubjectAuthorityResolver {
    private final UserRepository userRepository;

    @Override
    public boolean supports(String subjectType) {
        return "USER".equalsIgnoreCase(subjectType);
    }

    @Override
    public Set<GrantedAuthority> resolveAuthorities(Long subjectId) {
        Users user = userRepository.findByIdWithGroupsRolesAndPermissions(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + subjectId));
        return new HashSet<>(new CustomUserDetails(user).getAuthorities());
    }
}
