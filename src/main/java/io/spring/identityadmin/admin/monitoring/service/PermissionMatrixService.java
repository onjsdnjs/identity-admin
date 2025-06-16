package io.spring.identityadmin.admin.monitoring.service;


import io.spring.identityadmin.admin.monitoring.dto.MatrixFilter;
import io.spring.identityadmin.admin.monitoring.dto.PermissionMatrixDto;

public interface PermissionMatrixService {
    /**
     * [최종 리팩토링] 기본 권한 매트릭스를 조회합니다. (필터 없음)
     * 스마트 대시보드의 요약용으로 사용됩니다.
     */
    PermissionMatrixDto getPermissionMatrix();

    /**
     * [최종 리팩토링] 특정 필터 조건에 맞는 권한 매트릭스를 조회합니다.
     * 향후 구현될 상세 분석 기능(예: Authorization Studio)을 위한 확장성을 확보합니다.
     */
    PermissionMatrixDto getPermissionMatrix(MatrixFilter filter);
}