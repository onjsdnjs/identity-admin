package io.spring.identityadmin.resource;

import io.spring.identityadmin.domain.dto.ResourceMetadataDto;
import io.spring.identityadmin.domain.dto.ResourceSearchCriteria;
import io.spring.identityadmin.domain.entity.FunctionCatalog;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.repository.FunctionCatalogRepository;
import io.spring.identityadmin.repository.ManagedResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final FunctionCatalogRepository functionCatalogRepository;

    @Override
    @Transactional
    public void refreshResources() {
        // 1. 시스템의 모든 기술 리소스 스캔
        List<ManagedResource> discoveredResources = scanners.stream()
                .flatMap(scanner -> scanner.scan().stream())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(ManagedResource::getResourceIdentifier, Function.identity(), (r1, r2) -> r1),
                        map -> new ArrayList<>(map.values())
                ));

        Map<String, ManagedResource> existingResources = managedResourceRepository.findAll().stream()
                .collect(Collectors.toMap(ManagedResource::getResourceIdentifier, Function.identity()));

        // 2. 새로 발견된 리소스 저장 및 기능 카탈로그 생성
        for (ManagedResource discovered : discoveredResources) {
            ManagedResource resource = existingResources.computeIfAbsent(discovered.getResourceIdentifier(), k -> {
                log.info("새로운 기술 리소스 발견 및 저장: {}", discovered.getResourceIdentifier());
                return managedResourceRepository.save(discovered);
            });

            // 3. 해당 리소스에 대한 기능 카탈로그가 없으면 새로 생성
            functionCatalogRepository.findByManagedResource(resource).orElseGet(() -> {
                log.info("신규 기능 카탈로그 항목 생성: {}", resource.getFriendlyName());
                FunctionCatalog catalog = FunctionCatalog.builder()
                        .managedResource(resource)
                        .friendlyName(resource.getFriendlyName()) // 스캐너가 제안한 이름
                        .description(resource.getDescription())   // 스캐너가 제안한 설명
                        .status(FunctionCatalog.CatalogStatus.UNCONFIRMED)
                        .build();
                return functionCatalogRepository.save(catalog);
            });
        }
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