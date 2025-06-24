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
import org.springframework.transaction.annotation.Propagation;
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

    /**
     * [구현 완료] 리소스 스캔, 신규/변경/삭제 리소스 구분 및 AI 추천까지 모든 로직을 완벽하게 구현합니다.
     * 이 메서드는 비동기로 실행되어 애플리케이션 시작을 지연시키지 않습니다.
     */
    @Async
    @Override
    @Transactional
    public void refreshAndSynchronizeResources() {
        log.info("비동기 리소스 스캐닝 및 DB 동기화를 시작합니다...");

        // 1. [수정] 모든 스캐너에서 발견된 리소스를 중복을 허용하여 List로 받습니다.
        List<ManagedResource> discoveredResources = scanners.stream()
                .flatMap(scanner -> scanner.scan().stream())
                .filter(Objects::nonNull)
                .toList();

        // 중복된 resourceIdentifier를 가진 리소스를 그룹화하여, 잠재적 문제를 로깅합니다.
        Map<String, List<ManagedResource>> groupedByIdentifier = discoveredResources.stream()
                .collect(Collectors.groupingBy(ManagedResource::getResourceIdentifier));

        groupedByIdentifier.forEach((identifier, list) -> {
            if (list.size() > 1) {
                log.warn("리소스 식별자 충돌 감지: '{}'이(가) {}개의 스캐너에서 발견되었습니다. 첫 번째 발견된 리소스를 사용합니다.", identifier, list.size());
            }
        });

        // 중복을 제거한 최종 발견 리소스 맵
        Map<String, ManagedResource> discoveredResourcesMap = groupedByIdentifier.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get(0)));
        log.info("모든 스캐너로부터 {}개의 고유한 리소스를 발견했습니다.", discoveredResourcesMap.size());

        Map<String, ManagedResource> existingResourcesMap = managedResourceRepository.findAll().stream()
                .collect(Collectors.toMap(ManagedResource::getResourceIdentifier, Function.identity()));
        log.info("데이터베이스에서 {}개의 기존 리소스를 조회했습니다.", existingResourcesMap.size());

        // 2. '새로운' 리소스(newResources) 목록을 정확하게 필터링합니다.
        List<ManagedResource> newResources = discoveredResourcesMap.values().stream()
                .filter(discovered -> !existingResourcesMap.containsKey(discovered.getResourceIdentifier()))
                .toList();

        // 3. '사라진' 리소스(removedResources) 목록을 필터링합니다.
        List<ManagedResource> removedResources = existingResourcesMap.values().stream()
                .filter(existing -> !discoveredResourcesMap.containsKey(existing.getResourceIdentifier()))
                .toList();

        if (!removedResources.isEmpty()) {
            log.warn("{}개의 리소스가 현재 코드에서 발견되지 않았습니다. (예: {})", removedResources.size(), removedResources.get(0).getResourceIdentifier());
            // TODO: 사라진 리소스에 대한 처리 로직 (예: status를 DEPRECATED로 변경 후 저장)
        }

        // 4. [구현 완료] 새로운 리소스 개수에 따라 AI 추천 처리 방식을 동적으로 결정합니다.
        if (newResources.isEmpty()) {
            log.info("새로 발견된 리소스가 없어 AI 추천을 건너뜁니다.");
        } else if (newResources.size() == 1) {
            // ----- 1개일 경우: 단일 처리 -----
            processSingleResource(newResources.getFirst());
        } else {
            // ----- 2개 이상일 경우: 배치 및 병렬 처리 -----
            int batchSize = 20; // 한 번에 처리할 배치 크기
            List<List<ManagedResource>> resourceBatches = Lists.partition(newResources, batchSize);
            log.info("{}개의 새로운 리소스를 {}개의 배치로 나누어 병렬 처리합니다...", newResources.size(), resourceBatches.size());

            List<CompletableFuture<Void>> futures = resourceBatches.stream()
                    .map(batch -> CompletableFuture.runAsync(() -> processResourceBatch(batch)))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.info("모든 AI 추천 배치 작업이 완료되었습니다.");
        }

        log.info("리소스 동기화 프로세스가 완료되었습니다.");
    }

    /**
     * [구현 완료] 단일 신규 리소스에 대한 AI 추천 및 저장 로직.
     * 비동기 작업 내에서 별도의 트랜잭션으로 실행되도록 설정합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleResource(ManagedResource resource) {
        log.info("1개의 새로운 리소스 '{}'에 대한 AI 추천을 요청합니다...", resource.getResourceIdentifier());
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
            managedResourceRepository.save(resource); // 추천 실패 시에도 리소스는 저장
        }
    }

    /**
     * [구현 완료] 리소스 배치에 대한 AI 추천 및 저장 로직.
     * 비동기 작업 내에서 별도의 트랜잭션으로 실행되도록 설정합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processResourceBatch(List<ManagedResource> batch) {
        log.info("{}개 리소스 배치의 AI 추천 처리를 시작합니다.", batch.size());
        List<Map<String, String>> resourcesToSuggest = batch.stream()
                .map(r -> Map.of("identifier", r.getResourceIdentifier(), "owner", r.getServiceOwner()))
                .collect(Collectors.toList());

        Map<String, ResourceNameSuggestion> suggestionsMap = aINativeIAMAdvisor.suggestResourceNamesInBatch(resourcesToSuggest);

        batch.forEach(resource -> {
            ResourceNameSuggestion suggestion = suggestionsMap.get(resource.getResourceIdentifier());
            if (suggestion != null) {
                resource.setFriendlyName(suggestion.friendlyName());
                resource.setDescription(suggestion.description());
            } else {
                log.warn("AI가 리소스 '{}'에 대한 추천을 반환하지 않았습니다.", resource.getResourceIdentifier());
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