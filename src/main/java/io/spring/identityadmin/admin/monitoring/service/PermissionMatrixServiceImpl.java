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
        List<Group> subjects = groupRepository.findAllWithRolesAndPermissions();

        // [최종 수정] 대시보드 요약용으로 모든 권한 대신, 주요 권한 5개만 선택
        List<PermissionDto> permissions = permissionCatalogService.getAvailablePermissions().stream()
                .limit(5)
                .toList();

        List<String> subjectNames = subjects.stream().map(Group::getName).collect(Collectors.toList());
        List<String> permissionDescriptions = permissions.stream().map(PermissionDto::getDescription).collect(Collectors.toList());

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

        return new PermissionMatrixDto(subjectNames, permissionDescriptions, matrixData);
    }
}
