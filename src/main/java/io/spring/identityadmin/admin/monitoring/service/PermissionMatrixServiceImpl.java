package io.spring.identityadmin.admin.monitoring.service;

import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.admin.monitoring.dto.MatrixFilter;
import io.spring.identityadmin.admin.monitoring.dto.PermissionMatrixDto;
import io.spring.identityadmin.domain.dto.PermissionDto;
import io.spring.identityadmin.domain.entity.Group;
import io.spring.identityadmin.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionMatrixServiceImpl implements PermissionMatrixService {

    private final GroupRepository groupRepository;
    private final PermissionCatalogService permissionCatalogService;

    @Override
    public PermissionMatrixDto getPermissionMatrix(MatrixFilter filter) {
        List<Group> subjects = groupRepository.findAllWithRolesAndUsers(); // 모든 그룹을 주체로 사용
        List<PermissionDto> permissions = permissionCatalogService.getAvailablePermissions(); // 모든 권한을 대상으로 사용

        List<String> subjectNames = subjects.stream().map(Group::getName).collect(Collectors.toList());
        List<String> permissionNames = permissions.stream().map(PermissionDto::getDescription).collect(Collectors.toList());

        Map<String, Map<String, String>> matrixData = new HashMap<>();

        for (Group group : subjects) {
            Map<String, String> rowData = new HashMap<>();
            Set<String> groupPermissions = group.getGroupRoles().stream()
                    .flatMap(gr -> gr.getRole().getRolePermissions().stream())
                    .map(rp -> rp.getPermission().getDescription())
                    .collect(Collectors.toSet());

            for (PermissionDto perm : permissions) {
                rowData.put(perm.getDescription(), groupPermissions.contains(perm.getDescription()) ? "GRANT" : "NONE");
            }
            matrixData.put(group.getName(), rowData);
        }

        return new PermissionMatrixDto(subjectNames, permissionNames, matrixData);
    }
}
