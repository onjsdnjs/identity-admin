package io.spring.identityadmin.resource;

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

    @Override
    @Transactional
    public void refreshResources() {
        List<ManagedResource> discoveredResources = scanners.stream()
                .flatMap(scanner -> scanner.scan().stream())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(ManagedResource::getResourceIdentifier, Function.identity(), (r1, r2) -> r1),
                        map -> new ArrayList<>(map.values())
                ));

        Map<String, ManagedResource> existingResources = managedResourceRepository.findAll().stream()
                .collect(Collectors.toMap(ManagedResource::getResourceIdentifier, Function.identity()));

        for (ManagedResource discovered : discoveredResources) {
            existingResources.computeIfAbsent(discovered.getResourceIdentifier(), k -> {
                log.info("New resource registered: {}", discovered.getResourceIdentifier());
                return managedResourceRepository.save(discovered);
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