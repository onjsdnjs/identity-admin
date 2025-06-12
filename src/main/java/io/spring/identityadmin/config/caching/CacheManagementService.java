package io.spring.identityadmin.config.caching;

import io.spring.identityadmin.admin.dashboard.dto.PermissionMatrixDto;
import io.spring.identityadmin.config.caching.dto.CacheInvalidationContext;

/**
 * [신규] 시스템의 성능 최적화를 위해 캐시를 중앙에서 관리하는 서비스입니다.
 */
public interface CacheManagementService {
    /**
     * 복잡한 연산이 필요한 권한 매트릭스 데이터를 캐시에 저장합니다.
     */
    void cachePermissionMatrix(String key, PermissionMatrixDto matrix);

    /**
     * 특정 이벤트 발생 시, 연관된 캐시들을 일괄적으로 무효화하여 데이터 정합성을 유지합니다.
     * (예: 정책 변경 이벤트 발생 시, 권한 매트릭스와 대시보드 캐시를 무효화)
     */
    void invalidateRelatedCaches(CacheInvalidationContext context);
}