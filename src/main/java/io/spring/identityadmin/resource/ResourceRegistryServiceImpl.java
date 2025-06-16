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
import org.springframework.util.CollectionUtils;

import java.util.*;
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
                // [오류 수정] isDefined() 대신 getStatus()를 사용하여 비교합니다.
                // 스캔된 정보와 DB 정보가 다를 때만 업데이트합니다.
                boolean needsUpdate = !Objects.equals(existing.getFriendlyName(), discovered.getFriendlyName()) ||
                        !Objects.equals(existing.getDescription(), discovered.getDescription()) ||
                        !Objects.equals(existing.getApiDocsUrl(), discovered.getApiDocsUrl()) ||
                        !Objects.equals(existing.getSourceCodeLocation(), discovered.getSourceCodeLocation());

                if (needsUpdate) {
                    existing.setFriendlyName(discovered.getFriendlyName());
                    existing.setDescription(discovered.getDescription());
                    existing.setApiDocsUrl(discovered.getApiDocsUrl());
                    existing.setSourceCodeLocation(discovered.getSourceCodeLocation());
                    // isDefined 상태는 스캐너가 결정하므로 함께 업데이트
                    if (discovered.getStatus() == ManagedResource.Status.NEEDS_DEFINITION) {
                        // 기존 상태가 정의 완료였다면 스캐너에 의해 미정의로 바뀌지 않도록 함
                        if(existing.getStatus() != ManagedResource.Status.PERMISSION_CREATED && existing.getStatus() != ManagedResource.Status.POLICY_CONNECTED){
                            existing.setStatus(ManagedResource.Status.NEEDS_DEFINITION);
                        }
                    } else {
                        existing.setStatus(discovered.getStatus());
                    }

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
        resource.setStatus(ManagedResource.Status.PERMISSION_CREATED); // 상태 변경

        ManagedResource savedResource = managedResourceRepository.save(resource);
        log.info("Resource (ID: {}) has been defined by admin. Status set to PERMISSION_CREATED.", resourceId);

        return permissionCatalogService.synchronizePermissionFor(savedResource);
    }

    @Override
    @Transactional
    public void updateResourceManagementStatus(Long resourceId, ResourceManagementDto managedDto) {
        ManagedResource resource = managedResourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with ID: " + resourceId));
        if (resource.getPermission() != null) {
            resource.setStatus(ManagedResource.Status.PERMISSION_CREATED);
        } else {
            resource.setStatus(ManagedResource.Status.NEEDS_DEFINITION);
        }
        managedResourceRepository.save(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ManagedResource> findResources(ResourceSearchCriteria criteria, Pageable pageable) {
        // RepositoryCustomImpl 에서 검색 로직을 처리
        return managedResourceRepository.findByCriteria(criteria, pageable);
    }

    @Override
    @Transactional
    public void excludeResourceFromManagement(Long resourceId) {
        ManagedResource resource = managedResourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with ID: " + resourceId));
        resource.setStatus(ManagedResource.Status.EXCLUDED);
        managedResourceRepository.save(resource);
        log.info("Resource (ID: {}) has been excluded from management.", resourceId);
    }

    @Override
    @Transactional
    public void restoreResourceToManagement(Long resourceId) {
        ManagedResource resource = managedResourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with ID: " + resourceId));
        // 복원 시, 권한이 이미 생성되었는지 여부에 따라 상태를 결정
        if (resource.getPermission() != null) {
            resource.setStatus(ManagedResource.Status.PERMISSION_CREATED);
        } else {
            resource.setStatus(ManagedResource.Status.NEEDS_DEFINITION);
        }
        managedResourceRepository.save(resource);
        log.info("Resource (ID: {}) has been restored to management.", resourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getAllServiceOwners() {
        return managedResourceRepository.findAllServiceOwners();
    }

    @Override
    @Transactional
    public void batchUpdateStatus(List<Long> ids, ManagedResource.Status status) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        List<ManagedResource> resourcesToUpdate = managedResourceRepository.findAllById(ids);
        if (resourcesToUpdate.isEmpty()) {
            return;
        }

        for (ManagedResource resource : resourcesToUpdate) {
            // 복원 로직과 동일하게, 권한 존재 여부에 따라 상태를 결정
            if (status == ManagedResource.Status.NEEDS_DEFINITION) {
                if (resource.getPermission() != null) {
                    resource.setStatus(ManagedResource.Status.PERMISSION_CREATED);
                } else {
                    resource.setStatus(ManagedResource.Status.NEEDS_DEFINITION);
                }
            } else {
                resource.setStatus(status);
            }
        }

        managedResourceRepository.saveAll(resourcesToUpdate);
        log.info("Batch updated status for {} resources to {}", resourcesToUpdate.size(), status);
    }
}