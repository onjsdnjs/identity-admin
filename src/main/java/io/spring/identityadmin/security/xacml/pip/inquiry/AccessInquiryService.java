package io.spring.identityadmin.security.xacml.pip.inquiry;

import io.spring.identityadmin.domain.dto.EntitlementDto;

import java.util.List;

/**
 * 현재 정책들을 기반으로 접근 권한 현황을 조회하는 서비스입니다.
 * '워크벤치'의 핵심 데이터 제공자 역할을 합니다.
 */
public interface AccessInquiryService {
    /**
     * 특정 리소스에 접근 권한이 있는 주체(사용자/그룹) 목록을 조회합니다.
     * @param resourceId 조회할 리소스의 ID
     * @return 해당 리소스에 대한 권한 부여(Entitlement) 정보 목록
     */
    List<EntitlementDto> getEntitlementsForResource(Long resourceId);

    /**
     * 특정 주체(사용자/그룹)에게 부여된 모든 접근 권한 목록을 조회합니다.
     * @param subjectId 조회할 주체의 ID (사용자 or 그룹)
     * @param subjectType 주체의 타입 ("USER" or "GROUP")
     * @return 해당 주체에 대한 권한 부여(Entitlement) 정보 목록
     */
    List<EntitlementDto> getEntitlementsForSubject(Long subjectId, String subjectType);
}
