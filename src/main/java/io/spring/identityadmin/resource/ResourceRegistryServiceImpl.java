package io.spring.identityadmin.resource;

import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.domain.dto.ResourceManagementDto;
import io.spring.identityadmin.domain.dto.ResourceMetadataDto;
import io.spring.identityadmin.domain.dto.ResourceSearchCriteria;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.repository.ManagedResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceRegistryServiceImpl implements ResourceRegistryService {

    private final List<ResourceScanner> scanners;
    private final ManagedResourceRepository managedResourceRepository;
    private final PermissionCatalogService permissionCatalogService;

    @Override
    @Transactional
    public void refreshAndSynchronizeResources() {
        log.info("Starting resource scanning and DB synchronization...");

        Map<String, ManagedResource> discoveredResourcesMap = scanners.stream()
                .flatMap(scanner -> scanner.scan().stream())
                .collect(Collectors.toMap(
                        ManagedResource::getResourceIdentifier,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        Map<String, ManagedResource> existingResourcesMap = managedResourceRepository.findAll().stream()
                .collect(Collectors.toMap(ManagedResource::getResourceIdentifier, Function.identity()));

        List<ManagedResource> resourcesToSave = new ArrayList<>();

        discoveredResourcesMap.forEach((identifier, discovered) -> {
            ManagedResource existing = existingResourcesMap.get(identifier);
            if (existing != null) {
                boolean needsUpdate = !existing.getFriendlyName().equals(discovered.getFriendlyName()) ||
                        !existing.getDescription().equals(discovered.getDescription()) ||
                        existing.isDefined() != discovered.isDefined();

                if (needsUpdate) {
                    existing.setFriendlyName(discovered.getFriendlyName());
                    existing.setDescription(discovered.getDescription());
                    existing.setDefined(discovered.isDefined());
                    resourcesToSave.add(existing);
                }
            } else {
                resourcesToSave.add(discovered);
            }
        });

        if (!resourcesToSave.isEmpty()) {
            managedResourceRepository.saveAll(resourcesToSave);
            log.info("{} new or updated ManagedResources have been saved.", resourcesToSave.size());
        }

        log.info("Resource synchronization process completed.");
    }

    @Override
    @Transactional
    public Permission defineResourceAsPermission(Long resourceId, ResourceMetadataDto metadataDto) {
        ManagedResource resource = managedResourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with ID: " + resourceId));

        resource.setFriendlyName(metadataDto.getFriendlyName());
        resource.setDescription(metadataDto.getDescription());
        resource.setManaged(true); // 권한화하는 순간, 관리 대상으로 명시
        resource.setDefined(true);   // 관리자가 직접 정의했으므로 true

        ManagedResource savedResource = managedResourceRepository.save(resource);
        log.info("Resource (ID: {}) has been defined by admin.", resourceId);

        return permissionCatalogService.synchronizePermissionFor(savedResource);
    }

    @Override
    @Transactional
    public void updateResourceManagementStatus(Long resourceId, ResourceManagementDto managedDto) {
        ManagedResource resource = managedResourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with ID: " + resourceId));
        resource.setManaged(managedDto.isManaged());
        managedResourceRepository.save(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ManagedResource> findResources(ResourceSearchCriteria criteria, Pageable pageable) {
        // RepositoryCustomImpl에서 검색 로직을 처리
        return managedResourceRepository.findByCriteria(criteria, pageable);
    }
}