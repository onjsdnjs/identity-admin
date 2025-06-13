package io.spring.identityadmin.admin.metadata.service;

import io.spring.identityadmin.domain.dto.PermissionDto;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCatalogServiceImpl implements PermissionCatalogService {

    private final PermissionRepository permissionRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public void synchronize(List<ManagedResource> discoveredResources) {
        log.info("Starting permission catalog synchronization...");

        // isManaged가 true 이고, 의미있는 이름(@Operation summary)을 가진 리소스만 필터링
        List<ManagedResource> resourcesToSync = discoveredResources.stream()
                .filter(ManagedResource::isDefined)
                .toList();

        for (ManagedResource resource : resourcesToSync) {
            String permissionName = "PERM_" + resource.getResourceType() + "_" + resource.getId();
            Permission permission = permissionRepository.findByName(permissionName)
                    .orElseGet(() -> {
                        log.info("New permission will be created for resource: {}", resource.getFriendlyName());
                        return Permission.builder().name(permissionName).build();
                    });

            // 개발자가 코드에 명시한 이름과 설명으로 업데이트
            permission.setDescription(resource.getFriendlyName()); // 이것이 사용자 친화적 이름
            permission.setTargetType(resource.getResourceType().name());
            permission.setActionType(resource.getHttpMethod() != null ? resource.getHttpMethod().name() : "ACCESS");

            permissionRepository.save(permission);
        }
        log.info("Permission catalog synchronized with {} resources.", resourcesToSync.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getAvailablePermissions() {
        return permissionRepository.findDefinedPermissions().stream()
                .map(p -> modelMapper.map(p, PermissionDto.class))
                .toList();
    }
}
