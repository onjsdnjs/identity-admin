package io.spring.identityadmin.security.xacml.pap.service;

import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import io.spring.identityadmin.domain.entity.policy.Policy;

/**
 * 컨트롤러의 요청을 받아 비즈니스 정책의 생명주기를 관리하는 메인 서비스.
 * PolicyGenerationService, DefaultPolicyService 등 다른 서비스를 조율한다.
 */
public interface BusinessPolicyService {

    /**
     * 비즈니스 규칙 DTO를 받아 새로운 정책을 생성하고 저장한다.
     * @param dto 사용자가 UI에서 입력한 비즈니스 정책 정보
     * @return 생성된 Policy 엔티티
     */
    Policy createPolicyFromBusinessRule(BusinessPolicyDto dto);

    /**
     * 비즈니스 규칙 DTO를 받아 기존 정책을 수정하고 저장한다.
     * @param policyId 수정할 정책의 ID
     * @param dto 사용자가 UI에서 수정한 비즈니스 정책 정보
     * @return 수정된 Policy 엔티티
     */
    Policy updatePolicyFromBusinessRule(Long policyId, BusinessPolicyDto dto);

    /**
     * 기존의 기술적 Policy를 비즈니스 규칙 DTO로 변환하여 반환한다. (수정 화면을 채우기 위함)
     * @param policyId 조회할 정책의 ID
     * @return 변환된 BusinessPolicyDto
     */
    BusinessPolicyDto getBusinessRuleForPolicy(Long policyId);

}
