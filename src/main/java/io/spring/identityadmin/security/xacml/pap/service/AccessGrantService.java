package io.spring.identityadmin.security.xacml.pap.service;

import io.spring.identityadmin.domain.dto.GrantRequestDto;
import io.spring.identityadmin.domain.dto.RevokeRequestDto;
import io.spring.identityadmin.domain.entity.policy.Policy;

/**
 * 사용자 친화적인 요청을 받아 실제 기술 정책(Policy)으로 변환하고,
 * 권한의 생명주기를 관리하는 서비스입니다.
 */
public interface AccessGrantService {
    /**
     * 다수의 주체에게 다수의 리소스에 대한 권한을 일괄 부여합니다.
     *
     * @param grantRequest 권한 부여에 필요한 정보(주체, 리소스, 행위, 조건)를 담은 DTO
     * @return 생성/수정된 기술 정책(Policy) 목록
     */
    Policy grantAccess(GrantRequestDto grantRequest);

    /**
     * 특정 주체의 특정 리소스에 대한 접근 권한을 회수합니다.
     *
     * @param revokeRequest 권한 회수에 필요한 정보를 담은 DTO
     */
    void revokeAccess(RevokeRequestDto revokeRequest);
}

