package io.spring.identityadmin.iamw;

import io.spring.identityadmin.admin.repository.GroupRepository;
import io.spring.identityadmin.entity.Group;
import io.spring.identityadmin.entity.Users;
import io.spring.identityadmin.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GroupAuthorityResolver implements SubjectAuthorityResolver {
    private final GroupRepository groupRepository;

    @Override
    public boolean supports(String subjectType) {
        return "GROUP".equalsIgnoreCase(subjectType);
    }

    @Override
    public Set<GrantedAuthority> resolveAuthorities(Long subjectId) {
        Group group = groupRepository.findByIdWithRoles(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + subjectId));
        // 가상의 사용자를 만들어 그룹 권한만 계산
        Users virtualUser = Users.builder().userGroups(Set.of(new io.spring.identityadmin.entity.UserGroup(null, group))).build();
        return new HashSet<>(new CustomUserDetails(virtualUser).getAuthorities());
    }
}