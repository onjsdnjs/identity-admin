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

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCatalogServiceImpl implements PermissionCatalogService {

    private final PermissionRepository permissionRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public void synchronize(List<ManagedResource> discoveredResources) {
        log.info("Starting permission catalog synchronization with {} discovered resources.", discoveredResources.size());
        for (ManagedResource resource : discoveredResources) {
            // 기술 식별자를 기반으로 기존 권한이 있는지 확인
            String permissionName = "PERM_" + resource.getResourceType() + "_" + resource.getId();

            Permission permission = permissionRepository.findByName(permissionName)
                    .orElseGet(() -> {
                        log.info("New permission will be created for resource: {}", resource.getFriendlyName());
                        return Permission.builder().name(permissionName).build();
                    });

            // 개발자가 코드에 명시한 이름과 설명으로 업데이트
            permission.setDescription(resource.getFriendlyName() + " (" + resource.getDescription() + ")");
            permission.setTargetType(resource.getResourceType().name());
            permission.setActionType(resource.getHttpMethod() != null ? resource.getHttpMethod().name() : "ACCESS");

            permissionRepository.save(permission);
        }
        log.info("Permission catalog synchronization completed.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getAvailablePermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> modelMapper.map(p, PermissionDto.class))
                .toList();
    }
}
