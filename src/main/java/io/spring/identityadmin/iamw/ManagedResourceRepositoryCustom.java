package io.spring.identityadmin.iamw;

import io.spring.identityadmin.domain.dto.ResourceSearch;
import io.spring.identityadmin.entity.ManagedResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ManagedResourceRepositoryCustom {
    /**
     * Querydsl을 사용하여 동적 검색 조건 및 페이징을 처리합니다.
     * @param criteria 검색 조건
     * @param pageable 페이징 정보
     * @return 페이징 처리된 리소스 목록
     */
    Page<ManagedResource> findBySearch(ResourceSearch criteria, Pageable pageable);
}
