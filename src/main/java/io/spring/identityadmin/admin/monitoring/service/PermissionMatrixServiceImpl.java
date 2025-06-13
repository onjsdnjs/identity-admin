package io.spring.identityadmin.admin.monitoring.service;

import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.admin.monitoring.dto.MatrixFilter;
import io.spring.identityadmin.admin.monitoring.dto.PermissionMatrixDto;
import io.spring.identityadmin.domain.dto.PermissionDto;
import io.spring.identityadmin.domain.entity.Group;
import io.spring.identityadmin.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(readOnly = true)
    public PermissionMatrixDto getPermissionMatrix(MatrixFilter filter) {

        List<Group> subjects = groupRepository.findAllWithRolesAndPermissions();
        List<PermissionDto> permissions = permissionCatalogService.getAvailablePermissions();
        List<String> subjectNames = subjects.stream().map(Group::getName).toList();
        List<String> permissionNames = permissions.stream().map(PermissionDto::getDescription).toList();
        Map<String, Map<String, String>> matrixData = new HashMap<>();
        for (Group group : subjects) {
            Map<String, String> rowData = new HashMap<>();
            Set<String> groupPermissions = group.getGroupRoles().stream()
                    .flatMap(gr -> gr.getRole().getRolePermissions().stream())
                    .map(rp -> rp.getPermission().getDescription()).collect(Collectors.toSet());
            for (PermissionDto perm : permissions) {
                rowData.put(perm.getDescription(), groupPermissions.contains(perm.getDescription()) ? "GRANT" : "NONE");
            }
            matrixData.put(group.getName(), rowData);
        }
        return new PermissionMatrixDto(subjectNames, permissionNames, matrixData);
    }
}
