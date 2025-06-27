package io.spring.iam.admin.facade;

import io.spring.iam.domain.dto.UserDto;
import io.spring.iam.domain.entity.Group;
import io.spring.iam.domain.entity.Permission;
import io.spring.iam.domain.entity.Users;
import io.spring.iam.domain.entity.policy.Policy;
import io.spring.iam.repository.GroupRepository;
import io.spring.iam.repository.PermissionRepository;
import io.spring.iam.repository.UserRepository;
import io.spring.iam.security.xacml.pap.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CoreServiceFacadeImpl implements CoreServiceFacade {

    private final PolicyService policyService;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public Policy createPolicy(Policy policy) {
        // 실제 PolicyService 구현체를 호출하도록 위임
        // PolicyDto 변환 등 필요한 로직이 여기에 포함될 수 있음
        // 현재는 PolicyService가 PolicyDto를 받으므로 직접 호출 대신 예시로 남김
        throw new UnsupportedOperationException("Policy creation logic needs to be implemented via DTO conversion.");
    }

    @Override
    public Users createUser(UserDto userDto) {
        // UserAdminService (UserManagementService)를 통해 사용자 생성 로직 위임
        throw new UnsupportedOperationException("Not implemented yet. Should delegate to UserAdminService.");
    }

    @Override
    public Group getGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + groupId));
    }

    @Override
    public List<Permission> findPermissionsByIds(Set<Long> permissionIds) {
        return permissionRepository.findAllById(permissionIds);
    }

    @Override
    public String findUsernameById(Long userId) {
        return userRepository.findById(userId)
                .map(Users::getUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }
}