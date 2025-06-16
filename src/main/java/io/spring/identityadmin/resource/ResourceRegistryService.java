package io.spring.identityadmin.resource;

import io.spring.identityadmin.domain.dto.ResourceMetadataDto;
import io.spring.identityadmin.domain.dto.ResourceSearchCriteria;
import io.spring.identityadmin.domain.entity.ManagedResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ResourceRegistryService {
    /**
     * [역할 통합] 시스템의 모든 리소스를 재탐색하고, 이를 바탕으로 '권한 카탈로그(Permission Table)'를
     * 자동으로 생성 및 동기화하는 유일한 진입점입니다.
     */
    void refreshAndSynchronizePermissions();

    Page<ManagedResource> findResources(ResourceSearchCriteria searchCriteria, Pageable pageable);
    List<ManagedResource> findAllForAdmin();
    void updateResource(Long id, ResourceMetadataDto metadataDto);
}