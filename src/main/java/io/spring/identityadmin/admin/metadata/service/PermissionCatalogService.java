package io.spring.identityadmin.admin.metadata.service;

import io.spring.identityadmin.domain.dto.PermissionDto;
import io.spring.identityadmin.domain.entity.ManagedResource;
import java.util.List;

/**
 * [신규] 권한 카탈로그 서비스
 * 시스템의 모든 비즈니스 권한(Permission)을 카탈로그 형태로 관리합니다.
 * Resource 스캔 결과를 바탕으로 Permission을 자동 생성/업데이트하고,
 * 정책 생성 UI(마법사, Studio)에 사용될 권한 목록을 제공하는 핵심적인 역할을 합니다.
 */
public interface PermissionCatalogService {

    /**
     * 스캐너가 발견한 리소스 목록을 기반으로 시스템의 권한 카탈로그를 최신 상태로 동기화합니다.
     * - 새로운 리소스(@Operation 명시)는 신규 Permission으로 자동 생성됩니다.
     * - 기존 리소스의 @Operation 정보가 변경되면, 연결된 Permission의 설명도 자동 업데이트됩니다.
     * - 코드에서 삭제된 리소스에 연결된 Permission은 비활성화(deprecated) 처리될 수 있습니다.
     *
     * @param discoveredResources 스캐너가 발견한 리소스 목록 (Swagger 정보 포함)
     */
    void synchronize(List<ManagedResource> discoveredResources);

    /**
     * 정책 생성 UI(마법사, Studio)에서 관리자가 '선택'할 수 있는 모든 비즈니스 권한 목록을 조회합니다.
     *
     * @return 사용자 친화적인 권한 DTO 목록 (ID, 이름, 설명 등 포함)
     */
    List<PermissionDto> getAvailablePermissions();
}