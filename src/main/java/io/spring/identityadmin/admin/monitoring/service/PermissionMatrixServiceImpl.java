package io.spring.identityadmin.admin.monitoring.service;

import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.admin.monitoring.dto.MatrixFilter;
import io.spring.identityadmin.admin.monitoring.dto.PermissionMatrixDto;
import io.spring.identityadmin.domain.dto.PermissionDto;
import io.spring.identityadmin.domain.entity.Group;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.repository.GroupRepository;
import io.spring.identityadmin.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionMatrixServiceImpl implements PermissionMatrixService {

    private final GroupRepository groupRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionCatalogService permissionCatalogService;

    @Override
    @Transactional(readOnly = true)
    public PermissionMatrixDto getPermissionMatrix() {
        // 필터가 없는 경우, 모든 주체와 권한을 대상으로 조회
        return getPermissionMatrix(new MatrixFilter(null, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionMatrixDto getPermissionMatrix(MatrixFilter filter) {
        // [최종 구현] 필터 객체를 실제로 사용하는 로직을 구현합니다.

        // 1. 주체(그룹) 조회: 필터에 groupIds가 있으면 해당 그룹만, 없으면 전체 그룹 조회
        List<Group> subjects = (filter != null && !CollectionUtils.isEmpty(filter.subjectIds()))
                ? groupRepository.findAllById(filter.subjectIds())
                : groupRepository.findAllWithRolesAndPermissions();

        // 2. 권한(리소스) 조회: 필터에 permissionIds가 있으면 해당 권한만, 없으면 전체 권한 조회
        List<Permission> permissions = (filter != null && !CollectionUtils.isEmpty(filter.permissionIds()))
                ? permissionRepository.findAllById(filter.permissionIds())
                : permissionRepository.findAll();

        // 이하 로직은 이전과 동일...
        List<String> subjectNames = subjects.stream().map(Group::getName).collect(Collectors.toList());
        List<String> permissionDescriptions = permissions.stream().map(Permission::getDescription).collect(Collectors.toList());

        Map<String, Map<String, String>> matrixData = new HashMap<>();
        for (Group group : subjects) {
            Map<String, String> rowData = new HashMap<>();
            Set<String> groupPermissions = group.getGroupRoles().stream()
                    .flatMap(gr -> gr.getRole().getRolePermissions().stream())
                    .map(rp -> rp.getPermission().getDescription())
                    .collect(Collectors.toSet());

            for (Permission perm : permissions) {
                rowData.put(perm.getDescription(), groupPermissions.contains(perm.getDescription()) ? "GRANT" : "NONE");
            }
            matrixData.put(group.getName(), rowData);
        }

        return new PermissionMatrixDto(subjectNames, permissionDescriptions, matrixData);
    }
}
