package io.spring.identityadmin.iamw;

import io.spring.identityadmin.domain.dto.ResourceMetadataDto;
import io.spring.identityadmin.domain.dto.ResourceSearchCriteria;
import io.spring.identityadmin.entity.ManagedResource;
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

    @Override
    @Transactional
    public void refreshResources() {
        log.info("Starting resource synchronization...");
        List<ManagedResource> discoveredResources = scanners.stream()
                .flatMap(scanner -> scanner.scan().stream())
                .toList();

        List<ManagedResource> distinctResources = discoveredResources.stream()
                .filter(r -> r.getResourceIdentifier() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(ManagedResource::getResourceIdentifier, Function.identity(), (r1, r2) -> r1),
                        map -> new ArrayList<>(map.values())
                ));

        Map<String, ManagedResource> existingResources = managedResourceRepository.findAll().stream()
                .collect(Collectors.toMap(ManagedResource::getResourceIdentifier, Function.identity()));

        for (ManagedResource discovered : distinctResources) {
            ManagedResource existing = existingResources.get(discovered.getResourceIdentifier());
            if (existing == null) {
                managedResourceRepository.save(discovered);
                log.info("New resource registered: {}", discovered.getResourceIdentifier());
            } else {
                boolean updated = false;
                if (!existing.getFriendlyName().equals(discovered.getFriendlyName())) {
                    existing.setFriendlyName(discovered.getFriendlyName());
                    updated = true;
                }
                if (!existing.getDescription().equals(discovered.getDescription())) {
                    existing.setDescription(discovered.getDescription());
                    updated = true;
                }
                if(updated) {
                    managedResourceRepository.save(existing);
                    log.info("Resource updated: {}", existing.getResourceIdentifier());
                }
            }
        }
        log.info("Resource synchronization finished.");
    }

    @Override
    @Transactional
    public void updateResourceMetadata(Long resourceId, ResourceMetadataDto metadataDto) {
        ManagedResource resource = managedResourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with ID: " + resourceId));

        resource.setFriendlyName(metadataDto.getFriendlyName());
        resource.setDescription(metadataDto.getDescription());
        resource.setServiceOwner(metadataDto.getServiceOwner());

        managedResourceRepository.save(resource);
        log.info("Metadata updated for resource ID: {}", resourceId);
    }

    /**
     * Specification 관련 코드를 모두 제거하고, Querydsl 구현체(RepositoryCustom)를 호출합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ManagedResource> findResources(ResourceSearchCriteria searchCriteria, Pageable pageable) {
        return managedResourceRepository.findBySearch(searchCriteria, pageable);
    }
}
