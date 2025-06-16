package io.spring.identityadmin.studio.service.impl;

import io.spring.identityadmin.admin.support.visualization.dto.GraphDataDto;
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
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudioVisualizerServiceImpl implements StudioVisualizerService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public GraphDataDto analyzeAccessPathAsGraph(Long subjectId, String subjectType, Long permissionId) {
        Permission targetPermission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with ID: " + permissionId));

        if ("USER".equalsIgnoreCase(subjectType)) {
            return analyzeUserAccessPathAsGraph(subjectId, targetPermission);
        } else if ("GROUP".equalsIgnoreCase(subjectType)) {
            return analyzeGroupAccessPathAsGraph(subjectId, targetPermission);
        } else {
            log.warn("Graph analysis for type '{}' is not supported.", subjectType);
            return new GraphDataDto(Collections.emptyList(), Collections.emptyList());
        }
    }

    private GraphDataDto analyzeUserAccessPathAsGraph(Long userId, Permission targetPermission) {
        Users user = userRepository.findByIdWithGroupsRolesAndPermissions(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        List<GraphDataDto.Node> nodes = new ArrayList<>();
        List<GraphDataDto.Edge> edges = new ArrayList<>();
        boolean accessGranted = false;

        String userNodeId = "user_" + user.getId();
        nodes.add(new GraphDataDto.Node(userNodeId, user.getName(), "USER", Map.of("email", user.getUsername())));
        String permNodeId = "perm_" + targetPermission.getId();

        // [핵심 수정] 권한 라벨이 null이 되지 않도록 처리
        String permLabel = StringUtils.hasText(targetPermission.getDescription()) ? targetPermission.getDescription() : targetPermission.getName();

        for (UserGroup userGroup : user.getUserGroups()) {
            Group group = userGroup.getGroup();
            String groupNodeId = "group_" + group.getId();
            nodes.add(new GraphDataDto.Node(groupNodeId, group.getName(), "GROUP", Map.of("description", group.getDescription())));
            edges.add(new GraphDataDto.Edge(userNodeId, groupNodeId, "소속"));

            for (GroupRole groupRole : group.getGroupRoles()) {
                Role role = groupRole.getRole();
                String roleNodeId = "role_" + role.getId();
                nodes.add(new GraphDataDto.Node(roleNodeId, role.getRoleName(), "ROLE", Map.of("description", role.getRoleDesc())));
                edges.add(new GraphDataDto.Edge(groupNodeId, roleNodeId, "역할 보유"));

                if (role.getRolePermissions().stream().anyMatch(rp -> rp.getPermission().equals(targetPermission))) {
                    accessGranted = true;
                    edges.add(new GraphDataDto.Edge(roleNodeId, permNodeId, "권한 포함 (허용)"));
                }
            }
        }

        nodes.add(new GraphDataDto.Node(permNodeId, permLabel, "PERMISSION", Map.of("name", targetPermission.getName(), "granted", accessGranted)));

        if (!accessGranted) {
            user.getUserGroups().stream()
                    .flatMap(ug -> ug.getGroup().getGroupRoles().stream())
                    .map(gr -> "role_" + gr.getRole().getId())
                    .forEach(roleNodeId -> edges.add(new GraphDataDto.Edge(roleNodeId, permNodeId, "권한 없음 (거부)")));
        }

        return new GraphDataDto(
                new ArrayList<>(new LinkedHashSet<>(nodes)),
                new ArrayList<>(new LinkedHashSet<>(edges))
        );
    }

    private GraphDataDto analyzeGroupAccessPathAsGraph(Long groupId, Permission targetPermission) {
        Group group = groupRepository.findByIdWithRoles(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + groupId));

        List<GraphDataDto.Node> nodes = new ArrayList<>();
        List<GraphDataDto.Edge> edges = new ArrayList<>();
        boolean accessGranted = false;

        String groupNodeId = "group_" + group.getId();
        nodes.add(new GraphDataDto.Node(groupNodeId, group.getName(), "GROUP", Map.of("description", group.getDescription())));
        String permNodeId = "perm_" + targetPermission.getId();

        // [핵심 수정] 권한 라벨이 null이 되지 않도록 처리
        String permLabel = StringUtils.hasText(targetPermission.getDescription()) ? targetPermission.getDescription() : targetPermission.getName();

        for (GroupRole groupRole : group.getGroupRoles()) {
            Role role = groupRole.getRole();
            String roleNodeId = "role_" + role.getId();
            nodes.add(new GraphDataDto.Node(roleNodeId, role.getRoleName(), "ROLE", Map.of("description", role.getRoleDesc())));
            edges.add(new GraphDataDto.Edge(groupNodeId, roleNodeId, "역할 보유"));

            if (role.getRolePermissions().stream().anyMatch(rp -> rp.getPermission().equals(targetPermission))) {
                accessGranted = true;
                edges.add(new GraphDataDto.Edge(roleNodeId, permNodeId, "권한 포함 (허용)"));
            }
        }

        nodes.add(new GraphDataDto.Node(permNodeId, permLabel, "PERMISSION", Map.of("name", targetPermission.getName(), "granted", accessGranted)));

        if (!accessGranted) {
            group.getGroupRoles().stream()
                    .map(gr -> "role_" + gr.getRole().getId())
                    .forEach(roleNodeId -> edges.add(new GraphDataDto.Edge(roleNodeId, permNodeId, "권한 없음 (거부)")));
        }

        return new GraphDataDto(
                new ArrayList<>(new LinkedHashSet<>(nodes)),
                new ArrayList<>(new LinkedHashSet<>(edges))
        );
    }

    // =================================================================
    //                    기존 메서드 (하위 호환성을 위해 유지)
    // =================================================================

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
                        log.debug("Access path found for user {} to permission {} via role {}", user.getUsername(), targetPermission.getName(), role.getRoleName());
                        path.add(new AccessPathNode("그룹", group.getName(), group.getDescription()));
                        path.add(new AccessPathNode("역할", role.getRoleName(), role.getRoleDesc()));
                        path.add(new AccessPathNode("권한", targetPermission.getDescription(), targetPermission.getName()));
                        return new AccessPathDto(path, true, "접근 허용: 역할 '" + role.getRoleName() + "'을 통해 권한이 부여되었습니다.");
                    }
                }
            }
        }

        log.debug("No access path found for user {} to permission {}", user.getUsername(), targetPermission.getName());
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
                    return new AccessPathDto(path, true, "접근 허용: 역할 '" + role.getRoleName() + "'을 통해 권한이 부여됩니다.");
                }
            }
        }
        path.add(new AccessPathNode("권한", targetPermission.getDescription(), targetPermission.getName()));
        return new AccessPathDto(path, false, "접근 거부: 해당 권한을 부여하는 경로를 찾을 수 없습니다.");
    }

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

        return permissionRepository.findAllByNameIn(permissionOrigins.keySet()).stream()
                .map(p -> new EffectivePermissionDto(p.getName(), p.getDescription(), permissionOrigins.get(p.getName())))
                .sorted(Comparator.comparing(EffectivePermissionDto::permissionDescription))
                .collect(Collectors.toList());
    }
}