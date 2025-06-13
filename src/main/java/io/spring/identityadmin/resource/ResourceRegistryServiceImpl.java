package io.spring.identityadmin.resource;

import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.domain.dto.ResourceMetadataDto;
import io.spring.identityadmin.domain.dto.ResourceSearchCriteria;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.repository.ManagedResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    /**
     * [최종 구현] refreshResources 로직을 통합하여, 리소스 스캔, 저장, 권한 동기화를
     * 하나의 트랜잭션으로 완벽하게 처리합니다.
     */
    @Override
    @Transactional
    public void refreshAndSynchronizePermissions() {
        log.info("Starting resource scanning and permission synchronization...");

        // 1. 시스템의 모든 기술 리소스 스캔
        Map<String, ManagedResource> discoveredResourcesMap = scanners.stream()
                .flatMap(scanner -> scanner.scan().stream())
                .collect(Collectors.toMap(
                        ManagedResource::getResourceIdentifier,
                        Function.identity(),
                        (existing, replacement) -> existing // 중복 키 발생 시 기존 값 유지
                ));

        // 2. DB에 이미 존재하는 리소스와 비교하여 신규/업데이트 대상 선정
        Map<String, ManagedResource> existingResourcesMap = managedResourceRepository.findAll().stream()
                .collect(Collectors.toMap(ManagedResource::getResourceIdentifier, Function.identity()));

        List<ManagedResource> resourcesToSave = discoveredResourcesMap.values().stream()
                .map(discovered -> {
                    ManagedResource existing = existingResourcesMap.get(discovered.getResourceIdentifier());
                    if (existing != null) {
                        // 기존 리소스가 있으면, 스캔된 정보(@Operation의 summary, description)로 업데이트
                        existing.setFriendlyName(discovered.getFriendlyName());
                        existing.setDescription(discovered.getDescription());
                        existing.setServiceOwner(discovered.getServiceOwner());
                        return existing;
                    } else {
                        // 새로운 리소스면 그대로 반환
                        return discovered;
                    }
                })
                .collect(Collectors.toList());

        // 3. 신규/업데이트된 ManagedResource를 DB에 저장
        managedResourceRepository.saveAll(resourcesToSave);
        log.info("{} ManagedResources have been saved or updated.", resourcesToSave.size());

        // 4. 저장된 최신 리소스 목록 전체를 권한 카탈로그 서비스에 전달하여 Permission 테이블과 동기화
        List<ManagedResource> allLatestResources = managedResourceRepository.findAll();
        permissionCatalogService.synchronize(allLatestResources);

        log.info("Resource scanning and permission synchronization process completed successfully.");
    }

    @Override
    @Transactional
    public void updateResource(Long id, ResourceMetadataDto metadataDto) {
        ManagedResource resource = managedResourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with ID: " + id));
        resource.setFriendlyName(metadataDto.getFriendlyName());
        resource.setDescription(metadataDto.getDescription());
        resource.setManaged(metadataDto.isManaged());
        managedResourceRepository.save(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ManagedResource> findResources(ResourceSearchCriteria criteria, Pageable pageable) {
        criteria.setManaged(true); // 워크벤치에는 관리 대상 리소스만 표시
        Page<ManagedResource> byCriteria = managedResourceRepository.findByCriteria(criteria, pageable);
        return managedResourceRepository.findByCriteria(criteria, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagedResource> findAllForAdmin() {
        return managedResourceRepository.findAll(Sort.by("resourceType", "serviceOwner", "friendlyName"));
    }
}