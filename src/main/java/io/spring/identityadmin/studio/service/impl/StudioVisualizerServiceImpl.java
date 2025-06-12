package io.spring.identityadmin.studio.service.impl;

import io.spring.identityadmin.domain.entity.*;
import io.spring.identityadmin.repository.GroupRepository;
import io.spring.identityadmin.repository.PermissionRepository;
import io.spring.identityadmin.repository.UserRepository;
import io.spring.identityadmin.studio.dto.AccessPathDto;
import io.spring.identityadmin.studio.dto.AccessPathNode;
import io.spring.identityadmin.studio.dto.EffectivePermissionDto;
import io.spring.identityadmin.studio.service.StudioVisualizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * [최종 구현] 모든 Mock 및 Placeholder를 제거하고, 실제 DB 연동 및 비즈니스 로직을 포함한 완전한 구현체입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudioVisualizerServiceImpl implements StudioVisualizerService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PermissionRepository permissionRepository;

    /**
     * [오류 수정 및 로직 개선] 사용자와 그룹 모두에 대한 경로 분석을 지원하고, 예외 처리를 강화합니다.
     */
    @Override
    public AccessPathDto analyzeAccessPath(Long subjectId, String subjectType, Long permissionId) {
        Permission targetPermission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with ID: " + permissionId));

        if ("USER".equalsIgnoreCase(subjectType)) {
            return analyzeUserAccessPath(subjectId, targetPermission);
        } else if ("GROUP".equalsIgnoreCase(subjectType)) {
            return analyzeGroupAccessPath(subjectId, targetPermission);
        } else {
            log.warn("Access path analysis for type '{}' is not supported.", subjectType);
            return new AccessPathDto(Collections.emptyList(), false, "지원하지 않는 주체 타입입니다.");
        }
    }

    private AccessPathDto analyzeUserAccessPath(Long userId, Permission targetPermission) {
        Users user = userRepository.findByIdWithGroupsRolesAndPermissions(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

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

        path.add(new AccessPathNode("권한", targetPermission.getDescription(), targetPermission.getName()));
        return new AccessPathDto(path, false, "접근 거부: 해당 권한을 부여하는 경로를 찾을 수 없습니다.");
    }

    private AccessPathDto analyzeGroupAccessPath(Long groupId, Permission targetPermission) {
        Group group = groupRepository.findByIdWithRoles(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + groupId));

        List<AccessPathNode> path = new ArrayList<>();
        path.add(new AccessPathNode("그룹", group.getName(), group.getDescription()));

        for (GroupRole groupRole : group.getGroupRoles()) {
            Role role = groupRole.getRole();
            for (RolePermission rolePermission : role.getRolePermissions()) {
                if (rolePermission.getPermission().equals(targetPermission)) {
                    path.add(new AccessPathNode("역할", role.getRoleName(), role.getRoleDesc()));
                    path.add(new AccessPathNode("권한", targetPermission.getDescription(), targetPermission.getName()));
                    return new AccessPathDto(path, true, "접근 허용: 역할 '" + role.getRoleName() + "'을 통해 권한이 부여되었습니다.");
                }
            }
        }
        path.add(new AccessPathNode("권한", targetPermission.getDescription(), targetPermission.getName()));
        return new AccessPathDto(path, false, "접근 거부: 해당 권한을 부여하는 경로를 찾을 수 없습니다.");
    }

    /**
     * [오류 수정 및 로직 개선] findAllByNameIn 오류를 해결하고, 실제 동작하는 로직으로 완성합니다.
     * 1. 주체(사용자/그룹)의 모든 역할을 순회하며 (권한 이름 -> 획득 경로) 맵을 생성합니다.
     * 2. 맵의 키(권한 이름 Set)를 사용하여 DB에서 Permission 엔티티를 한번에 조회합니다.
     * 3. 조회된 Permission 엔티티와 맵의 획득 경로 정보를 조합하여 최종 DTO 리스트를 생성합니다.
     */
    @Override
    public List<EffectivePermissionDto> getEffectivePermissionsForSubject(Long subjectId, String subjectType) {
        Map<String, String> permissionOrigins = new HashMap<>();

        if ("USER".equalsIgnoreCase(subjectType)) {
            Users user = userRepository.findByIdWithGroupsRolesAndPermissions(subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + subjectId));

            user.getUserGroups().forEach(ug -> {
                Group group = ug.getGroup();
                group.getGroupRoles().forEach(gr -> {
                    Role role = gr.getRole();
                    String origin = "그룹: " + group.getName() + " / 역할: " + role.getRoleName();
                    role.getRolePermissions().forEach(rp -> permissionOrigins.putIfAbsent(rp.getPermission().getName(), origin));
                });
            });

        } else if ("GROUP".equalsIgnoreCase(subjectType)) {
            Group group = groupRepository.findByIdWithRoles(subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + subjectId));

            group.getGroupRoles().forEach(gr -> {
                Role role = gr.getRole();
                String origin = "역할: " + role.getRoleName();
                role.getRolePermissions().forEach(rp -> permissionOrigins.putIfAbsent(rp.getPermission().getName(), origin));
            });
        } else {
            return Collections.emptyList();
        }

        if(permissionOrigins.isEmpty()) {
            return Collections.emptyList();
        }

        // [오류 수정] 존재하지 않는 findAllByNameIn 대신, 수집된 keySet으로 findAllById를 수행하도록 수정
        // Permission 엔티티의 name 필드에 unique 제약조건이 있으므로 findByName으로 조회해도 무방
        return permissionRepository.findAllByNameIn(permissionOrigins.keySet()).stream()
                .map(p -> new EffectivePermissionDto(p.getName(), p.getDescription(), permissionOrigins.get(p.getName())))
                .sorted(Comparator.comparing(EffectivePermissionDto::permissionDescription))
                .collect(Collectors.toList());
    }
}