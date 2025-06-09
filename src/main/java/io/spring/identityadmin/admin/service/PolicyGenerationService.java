package io.spring.identityadmin.admin.service;

import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import io.spring.identityadmin.entity.policy.Policy;

/**
 * BusinessPolicyDto를 기반으로 실제 인가 엔진이 사용할
 * 기술적인 Policy 객체를 생성(번역)하는 책임을 갖는다.
 */
public interface PolicyGenerationService {

    /**
     * DTO를 기반으로 새로운 Policy 엔티티를 생성한다.
     * @param dto 고객이 UI에서 설정한 비즈니스 정책 DTO
     * @return DB에 저장될 준비가 된 기술적 Policy 엔티티
     */
    Policy generatePolicy(BusinessPolicyDto dto);

    /**
     * DTO를 기반으로 기존 Policy 엔티티를 업데이트한다.
     * @param existingPolicy 업데이트할 기존 Policy 엔티티
     * @param dto 고객이 UI에서 수정한 비즈니스 정책 DTO
     */
    void updatePolicy(Policy existingPolicy, BusinessPolicyDto dto);
}
