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
    public Permission synchronizePermissionFor(ManagedResource resource) {
        if (resource.getStatus() == ManagedResource.Status.NEEDS_DEFINITION) {
            throw new IllegalStateException("Cannot create permission from a resource that needs definition. Resource ID: " + resource.getId());
        }

        String permissionName = generatePermissionName(resource);

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseGet(() -> Permission.builder().name(permissionName).build());

        permission.setFriendlyName(resource.getFriendlyName());
        permission.setDescription(resource.getDescription());
        permission.setTargetType(resource.getResourceType().name());

        String actionType = "EXECUTE"; // 메서드 기반일 때 기본값
        if (resource.getResourceType() == ManagedResource.ResourceType.URL && resource.getHttpMethod() != null) {
            actionType = resource.getHttpMethod().name();
        }
        permission.setActionType(actionType);

        Permission savedPermission = permissionRepository.save(permission);
        log.info("Permission '{}' has been synchronized for resource '{}'.", savedPermission.getName(), resource.getResourceIdentifier());

        return savedPermission;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getAvailablePermissions() {
        return permissionRepository.findDefinedPermissionsWithDetails().stream()
                .map(p -> modelMapper.map(p, PermissionDto.class))
                .collect(Collectors.toList());
    }

    private String generatePermissionName(ManagedResource resource) {
        // PERM_리소스ID_리소스식별자(일부) 로 고유하고 예측 가능한 이름 생성
        String identifierPart = resource.getResourceIdentifier()
                .replaceAll("[^a-zA-Z0-9]", "_")
                .toUpperCase();
        if (identifierPart.length() > 50) {
            identifierPart = identifierPart.substring(identifierPart.length() - 50);
        }
        return String.format("PERM_%d_%s", resource.getId(), identifierPart);
    }
}