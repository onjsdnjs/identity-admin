package io.spring.identityadmin.admin.metadata.service;

import io.spring.identityadmin.domain.dto.PermissionDto;
import io.spring.identityadmin.domain.entity.FunctionCatalog;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.repository.FunctionCatalogRepository;
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
    private final FunctionCatalogRepository functionCatalogRepository;
    private final ModelMapper modelMapper;

    /**
     * [최종 수정] 리소스 동기화 시, FunctionCatalog를 찾아 Permission과의 관계를 명시적으로 설정합니다.
     */
    @Override
    @Transactional
    public void synchronize(List<ManagedResource> discoveredResources) {
        log.info("Starting permission catalog synchronization...");

        List<ManagedResource> resourcesToSync = discoveredResources.stream()
                .filter(ManagedResource::isDefined)
                .toList();

        for (ManagedResource resource : resourcesToSync) {
            // 1. 해당 리소스에 대한 FunctionCatalog가 있는지 확인하거나 생성합니다.
            FunctionCatalog catalog = functionCatalogRepository.findByManagedResource(resource)
                    .orElseGet(() -> {
                        log.info("Creating new FunctionCatalog for resource: {}", resource.getFriendlyName());
                        return functionCatalogRepository.save(FunctionCatalog.builder()
                                .managedResource(resource)
                                .friendlyName(resource.getFriendlyName())
                                .description(resource.getDescription())
                                .status(FunctionCatalog.CatalogStatus.ACTIVE) // isDefined = true 이므로 바로 ACTIVE
                                .build());
                    });

            // 2. Permission을 찾거나 새로 생성합니다.
            String permissionName = "PERM_" + resource.getResourceType() + "_" + resource.getId();
            Permission permission = permissionRepository.findByName(permissionName)
                    .orElseGet(() -> {
                        log.info("New permission will be created for resource: {}", resource.getFriendlyName());
                        return Permission.builder().name(permissionName).build();
                    });

            // 3. Permission 정보 업데이트
            permission.setDescription(resource.getFriendlyName());
            permission.setTargetType(resource.getResourceType().name());
            permission.setActionType(resource.getHttpMethod() != null ? resource.getHttpMethod().name() : "ACCESS");

            // 4. Permission과 FunctionCatalog의 관계를 설정합니다.
            // 이 코드를 통해 PERMISSION_FUNCTIONS 조인 테이블에 데이터가 입력됩니다.
            permission.getFunctions().add(catalog);

            permissionRepository.save(permission);
        }
        log.info("Permission catalog synchronized with {} defined resources.", resourcesToSync.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getAvailablePermissions() {
        return permissionRepository.findDefinedPermissionsWithDetails().stream()
                .map(p -> modelMapper.map(p, PermissionDto.class))
                .toList();
    }
}
