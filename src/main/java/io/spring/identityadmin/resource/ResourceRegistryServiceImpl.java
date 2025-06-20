package io.spring.identityadmin.resource;

import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.ai.AINativeIAMAdvisor;
import io.spring.identityadmin.ai.dto.ResourceNameSuggestion;
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
    private final AINativeIAMAdvisor aINativeIAMAdvisor;

    @Override
    @Transactional
    public void refreshAndSynchronizeResources() {
        log.info("리소스 스캐닝 및 DB 동기화를 시작합니다...");

        // 모든 스캐너를 실행하여 현재 애플리케이션의 리소스를 탐지합니다.
        Map<String, ManagedResource> discoveredResourcesMap = scanners.stream()
                .flatMap(scanner -> {
                    try {
                        return scanner.scan().stream();
                    } catch (Exception e) {
                        log.error("리소스 스캐닝 중 오류 발생: {}", scanner.getClass().getSimpleName(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ManagedResource::getResourceIdentifier, Function.identity(), (e, r) -> e));

        log.info("모든 스캐너로부터 {}개의 고유한 리소스를 발견했습니다.", discoveredResourcesMap.size());

        Map<String, ManagedResource> existingResourcesMap = managedResourceRepository.findAll().stream()
                .collect(Collectors.toMap(ManagedResource::getResourceIdentifier, Function.identity()));

        List<ManagedResource> resourcesToSave = new ArrayList<>();

        discoveredResourcesMap.forEach((identifier, discovered) -> {
            ManagedResource existing = existingResourcesMap.get(identifier);
            if (existing == null) {
                // [AI 기능 연동] 새로운 리소스가 발견되면, AI에게 이름과 설명을 추천받습니다.
                log.debug("새로운 리소스 '{}' 발견. AI에게 이름과 설명을 요청합니다.", identifier);
                try {
                    ResourceNameSuggestion suggestion = aINativeIAMAdvisor.suggestResourceName(
                            discovered.getResourceIdentifier(),
                            discovered.getServiceOwner()
                    );
                    discovered.setFriendlyName(suggestion.friendlyName());
                    discovered.setDescription(suggestion.description());
                    log.info("AI 추천 적용 완료: '{}' -> '{}'", identifier, suggestion.friendlyName());
                } catch (Exception e) {
                    log.warn("AI 리소스 이름 추천 실패: {}. 기본값을 사용합니다.", identifier, e);
                    // AI 추천 실패 시 스캐너가 생성한 기본 이름 사용
                }
                resourcesToSave.add(discovered);
            }
            // 기존 리소스에 대한 업데이트 로직은 필요 시 추가
        });

        if (!resourcesToSave.isEmpty()) {
            managedResourceRepository.saveAll(resourcesToSave);
            log.info("{}개의 신규 또는 업데이트된 리소스가 데이터베이스에 저장되었습니다.", resourcesToSave.size());
        }

        log.info("리소스 동기화 프로세스가 완료되었습니다.");
    }

    /*@Override
    @Transactional
    public void refreshAndSynchronizeResources() {
        log.info("Starting resource scanning and DB synchronization...");

        Map<String, ManagedResource> discoveredResourcesMap = scanners.stream()
                .flatMap(scanner -> {
                    try {
                        return scanner.scan().stream();
                    } catch (Exception e) {
                        log.error("Error during resource scanning from scanner: {}", scanner.getClass().getSimpleName(), e);
                        return null; // 스트림에서 제외
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        ManagedResource::getResourceIdentifier,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        log.info("Discovered {} unique resources from all scanners.", discoveredResourcesMap.size());

        Map<String, ManagedResource> existingResourcesMap = managedResourceRepository.findAll().stream()
                .collect(Collectors.toMap(ManagedResource::getResourceIdentifier, Function.identity()));

        List<ManagedResource> resourcesToSave = new ArrayList<>();

        discoveredResourcesMap.forEach((identifier, discovered) -> {
            ManagedResource existing = existingResourcesMap.get(identifier);
            if (existing != null) {
                // 기존 리소스가 있을 경우, 스캔된 정보로 업데이트가 필요한지 확인
                boolean needsUpdate = !Objects.equals(existing.getFriendlyName(), discovered.getFriendlyName()) ||
                        !Objects.equals(existing.getDescription(), discovered.getDescription()) ||
                        !Objects.equals(existing.getApiDocsUrl(), discovered.getApiDocsUrl()) ||
                        !Objects.equals(existing.getSourceCodeLocation(), discovered.getSourceCodeLocation());


                if (needsUpdate) {
                    log.trace("Resource '{}' needs update.", identifier);
                    existing.setFriendlyName(discovered.getFriendlyName());
                    existing.setDescription(discovered.getDescription());
                    existing.setApiDocsUrl(discovered.getApiDocsUrl());
                    existing.setSourceCodeLocation(discovered.getSourceCodeLocation());

                    // [로직 개선] 관리자가 이미 상태를 변경한 경우(예: EXCLUDED), 스캔 결과로 덮어쓰지 않음
                    // 스캐너가 결정한 상태(NEEDS_DEFINITION 또는 PERMISSION_CREATED)는
                    // 기존 리소스가 초기 상태일 때만 적용
                    if (existing.getStatus() == ManagedResource.Status.NEEDS_DEFINITION) {
                        existing.setStatus(discovered.getStatus());
                    }

                    resourcesToSave.add(existing);
                }
            } else {
                // 새로운 리소스 추가
                log.trace("New resource '{}' found.", identifier);
                resourcesToSave.add(discovered);
            }
        });

        if (!resourcesToSave.isEmpty()) {
            managedResourceRepository.saveAll(resourcesToSave);
            log.info("{} new or updated ManagedResources have been saved to the database.", resourcesToSave.size());
        } else {
            log.info("No new or updated resources to save.");
        }

        log.info("Resource synchronization process completed.");
    }*/

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
        // [핵심 수정] 존재하지 않는 findAll(predicate, pageable) 대신,
        // ManagedResourceRepositoryCustom에 정의하고 구현한 findByCriteria를 호출합니다.
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