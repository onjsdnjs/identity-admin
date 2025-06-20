package io.spring.identityadmin.resource;

import com.google.common.collect.Lists;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
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

    @Async
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

        // 1. AI 추천이 필요한 '새로운' 리소스만 필터링하여 목록으로 만듭니다.
        List<ManagedResource> newResources = discoveredResourcesMap.values().stream()
                .filter(discovered -> !existingResourcesMap.containsKey(discovered.getResourceIdentifier()))
                .collect(Collectors.toList());

        if (newResources.isEmpty()) {
            log.info("새로 발견된 리소스가 없습니다.");

        } else if (newResources.size() == 1) {
            // ----- 1개일 경우: 단일 처리 -----
            ManagedResource singleResource = newResources.getFirst();
            log.info("1개의 새로운 리소스 '{}'에 대한 AI 추천을 요청합니다...", singleResource.getResourceIdentifier());
            processSingleResource(singleResource);

        } else {
            // 리소스를 20개 단위의 작은 배치로 나눕니다.
            int batchSize = 20;
            List<List<ManagedResource>> resourceBatches = Lists.partition(newResources, batchSize);
            log.info("{}개의 새로운 리소스를 {}개의 배치로 나누어 병렬 처리합니다...", newResources.size(), resourceBatches.size());

            // 각 배치를 비동기 병렬로 처리합니다.
            List<CompletableFuture<Void>> futures = resourceBatches.stream()
                    .map(batch -> CompletableFuture.runAsync(() -> processResourceBatch(batch)))
                    .toList();

            // 모든 병렬 작업이 완료될 때까지 기다립니다.
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            log.info("모든 AI 추천 배치 작업이 완료되었습니다.");
        }

        log.info("리소스 동기화 프로세스가 완료되었습니다.");
    }

    @Transactional
    public void processSingleResource(ManagedResource resource) {
        try {
            ResourceNameSuggestion suggestion = aINativeIAMAdvisor.suggestResourceName(
                    resource.getResourceIdentifier(),
                    resource.getServiceOwner()
            );
            resource.setFriendlyName(suggestion.friendlyName());
            resource.setDescription(suggestion.description());
            managedResourceRepository.save(resource);
            log.info("AI 추천 적용 완료: '{}' -> '{}'", resource.getResourceIdentifier(), suggestion.friendlyName());
        } catch (Exception e) {
            log.warn("AI 리소스 이름 추천 실패: {}. 기본값을 사용합니다.", resource.getResourceIdentifier(), e);
            managedResourceRepository.save(resource);
        }
    }

    @Transactional
    public void processResourceBatch(List<ManagedResource> batch) {
        List<Map<String, String>> resourcesToSuggest = batch.stream()
                .map(r -> Map.of("identifier", r.getResourceIdentifier(), "owner", r.getServiceOwner()))
                .collect(Collectors.toList());

        Map<String, ResourceNameSuggestion> suggestionsMap = aINativeIAMAdvisor.suggestResourceNamesInBatch(resourcesToSuggest);

        batch.forEach(resource -> {
            ResourceNameSuggestion suggestion = suggestionsMap.get(resource.getResourceIdentifier());
            if (suggestion != null) {
                resource.setFriendlyName(suggestion.friendlyName());
                resource.setDescription(suggestion.description());
            }
        });

        managedResourceRepository.saveAll(batch);
        log.info("{}개의 리소스 배치 처리가 완료되었습니다.", batch.size());
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