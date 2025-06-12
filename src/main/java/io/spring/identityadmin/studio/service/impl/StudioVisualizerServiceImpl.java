package io.spring.identityadmin.studio.service.impl;

import io.spring.identityadmin.domain.entity.*;
import io.spring.identityadmin.repository.PermissionRepository;
import io.spring.identityadmin.repository.UserRepository;
import io.spring.identityadmin.studio.dto.AccessPathDto;
import io.spring.identityadmin.studio.dto.AccessPathNode;
import io.spring.identityadmin.studio.dto.EffectivePermissionDto;
import io.spring.identityadmin.studio.service.StudioVisualizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudioVisualizerServiceImpl implements StudioVisualizerService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public AccessPathDto analyzeAccessPath(Long subjectId, String subjectType, Long permissionId) {
        // 이 로직은 실제 구현 시 더 정교하게 만들어져야 합니다. 여기서는 핵심 개념을 보여줍니다.
        if (!"USER".equalsIgnoreCase(subjectType)) {
            return new AccessPathDto(Collections.emptyList(), false, "현재 사용자 타입만 지원합니다.");
        }

        Users user = userRepository.findByIdWithGroupsRolesAndPermissions(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + subjectId));

        Permission targetPermission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionId));

        List<AccessPathNode> path = new ArrayList<>();
        path.add(new AccessPathNode("사용자", user.getName(), user.getUsername()));

        for (UserGroup userGroup : user.getUserGroups()) {
            Group group = userGroup.getGroup();
            for (GroupRole groupRole : group.getGroupRoles()) {
                Role role = groupRole.getRole();
                for (RolePermission rolePermission : role.getRolePermissions()) {
                    if (rolePermission.getPermission().equals(targetPermission)) {
                        path.add(new AccessPathNode("그룹", group.getName(), group.getDescription()));
                        path.add(new AccessPathNode("역할", role.getRoleName(), role.getRoleDesc()));
                        path.add(new AccessPathNode("권한", targetPermission.getDescription(), targetPermission.getName()));
                        return new AccessPathDto(path, true, "접근 허용: 역할 '" + role.getRoleName() + "'을 통해 권한이 부여되었습니다.");
                    }
                }
            }
        }

        return new AccessPathDto(path, false, "접근 거부: 해당 권한을 부여하는 경로를 찾을 수 없습니다.");
    }

    @Override
    public List<EffectivePermissionDto> getEffectivePermissionsForSubject(Long subjectId, String subjectType) {
        if (!"USER".equalsIgnoreCase(subjectType)) {
            return Collections.emptyList();
        }
        Users user = userRepository.findByIdWithGroupsRolesAndPermissions(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + subjectId));

        Map<String, EffectivePermissionDto> effectivePermissions = new LinkedHashMap<>();

        user.getUserGroups().forEach(ug -> {
            Group group = ug.getGroup();
            group.getGroupRoles().forEach(gr -> {
                Role role = gr.getRole();
                String origin = "그룹: " + group.getName() + " / 역할: " + role.getRoleName();
                role.getRolePermissions().forEach(rp -> {
                    Permission p = rp.getPermission();
                    effectivePermissions.putIfAbsent(p.getName(), new EffectivePermissionDto(p.getName(), p.getDescription(), origin));
                });
            });
        });

        return new ArrayList<>(effectivePermissions.values());
    }
}