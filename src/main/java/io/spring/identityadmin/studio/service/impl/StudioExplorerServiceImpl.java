package io.spring.identityadmin.studio.service.impl;

import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.repository.GroupRepository;
import io.spring.identityadmin.repository.PolicyRepository;
import io.spring.identityadmin.repository.UserRepository;
import io.spring.identityadmin.studio.dto.ExplorerItemDto;
import io.spring.identityadmin.studio.service.StudioExplorerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudioExplorerServiceImpl implements StudioExplorerService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PermissionCatalogService permissionCatalogService;
    private final PolicyRepository policyRepository;

    @Override
    public Map<String, List<ExplorerItemDto>> getExplorerItems() {
        List<ExplorerItemDto> users = userRepository.findAll().stream()
                .map(user -> new ExplorerItemDto(user.getId(), user.getName(), "USER", user.getUsername()))
                .toList();

        List<ExplorerItemDto> groups = groupRepository.findAll().stream()
                .map(group -> new ExplorerItemDto(group.getId(), group.getName(), "GROUP", group.getDescription()))
                .toList();

        List<ExplorerItemDto> permissions = permissionCatalogService.getAvailablePermissions().stream()
                .map(perm -> new ExplorerItemDto(perm.getId(), perm.getDescription(), "PERMISSION", "내부 식별자: " + perm.getName()))
                .toList();

        List<ExplorerItemDto> policies = policyRepository.findAllWithDetails().stream()
                .map(policy -> new ExplorerItemDto(policy.getId(), policy.getName(), "POLICY", policy.getFriendlyDescription()))
                .toList();

        return Map.of(
                "users", users,
                "groups", groups,
                "permissions", permissions,
                "policies", policies
        );
    }
}