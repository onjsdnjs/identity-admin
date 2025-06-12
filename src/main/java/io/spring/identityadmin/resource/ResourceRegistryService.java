package io.spring.identityadmin.resource;

import io.spring.identityadmin.domain.dto.ResourceMetadataDto;
import io.spring.identityadmin.domain.dto.ResourceSearchCriteria;
import io.spring.identityadmin.domain.entity.ManagedResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * ResourceScanner를 통해 탐지된 리소스를 DB에 등록하고,
 * 사용자 친화적인 메타데이터를 관리하는 서비스입니다.
 */
public interface ResourceRegistryService {
    /**
     * 시스템의 모든 리소스를 재탐색하고 DB에 최신 상태로 동기화합니다.
     */
    void refreshResources();

    /**
     * 관리자 UI에 표시할 모든 리소스를 검색 조건에 맞게 조회합니다.
     * @param searchCriteria 검색 조건
     * @return 페이징 처리된 리소스 목록
     */
    Page<ManagedResource> findResources(ResourceSearchCriteria searchCriteria, Pageable pageable);

    List<ManagedResource> findAllForAdmin();

    void updateResource(Long id, ResourceMetadataDto metadataDto);

    void refreshAndSynchronizePermissions();
}