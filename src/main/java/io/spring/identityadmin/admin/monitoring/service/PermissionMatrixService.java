package io.spring.identityadmin.admin.monitoring.service;


import io.spring.identityadmin.admin.monitoring.dto.MatrixFilter;
import io.spring.identityadmin.admin.monitoring.dto.PermissionMatrixDto;

/**
 * 복잡한 권한 관계를 분석하여 UI에 표시할 권한 매트릭스를 생성하는 책임을 가집니다.
 */
public interface PermissionMatrixService {
    PermissionMatrixDto getPermissionMatrix(MatrixFilter filter);
}
