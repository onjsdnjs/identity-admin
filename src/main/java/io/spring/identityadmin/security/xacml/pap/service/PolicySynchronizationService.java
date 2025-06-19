package io.spring.identityadmin.security.xacml.pap.service;

import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.entity.Role;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.domain.entity.policy.PolicyCondition;
import io.spring.identityadmin.repository.PolicyRepository;
import io.spring.identityadmin.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicySynchronizationService {

    private final PolicyRepository policyRepository;
    private final RoleRepository roleRepository;
    private final PolicyService policyService;

    /**
     * 특정 역할(Role)의 변경사항을 기반으로, 이에 매핑되는 기술 정책(Policy)을
     * 자동으로 생성하거나 업데이트하여 동기화합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchronizePolicyForRole(Role role) {

        String policyName = "AUTO_POLICY_FOR_" + role.getRoleName();

        // 1. 대상(Target) DTO 목록 생성: 역할에 포함된 모든 권한의 리소스를 DTO로 변환합니다.
        List<PolicyDto.TargetDto> targetDtos = role.getRolePermissions().stream()
                .map(rp -> rp.getPermission().getManagedResource())
                .filter(Objects::nonNull)
                .map(mr -> new PolicyDto.TargetDto(
                        mr.getResourceType().name(),
                        mr.getResourceIdentifier(),
                        mr.getHttpMethod() != null ? mr.getHttpMethod().name() : "ANY"
                ))
                .distinct()
                .collect(Collectors.toList());

        // 2. 역할에 포함된 모든 Permission의 이름을 기반으로 'OR' 결합된 SpEL 표현식을 생성합니다.
        String mergedPermissionsExpression = role.getRolePermissions().stream()
                .map(rp -> rp.getPermission().getName())
                .map(permissionName -> String.format("hasAuthority('%s')", permissionName))
                .collect(Collectors.joining(" or "));

        // 역할에 할당된 권한이 없을 경우, 이 정책은 누구도 통과할 수 없도록 'false'로 설정
        if (!StringUtils.hasText(mergedPermissionsExpression)) {
            mergedPermissionsExpression = "false";
        }

        PolicyDto.ConditionDto conditionDto = PolicyDto.ConditionDto.builder()
                .expression(mergedPermissionsExpression)
                .authorizationPhase(PolicyCondition.AuthorizationPhase.PRE_AUTHORIZE)
                .build();

        PolicyDto.RuleDto ruleDto = PolicyDto.RuleDto.builder()
                .description("Auto-generated rule for " + role.getRoleName())
                .conditions(List.of(conditionDto))
                .build();

        // 3. 최종 PolicyDto 생성
        PolicyDto policyDto = PolicyDto.builder()
                .name(policyName)
                .description(String.format("'%s' 역할을 위한 자동 생성 정책", role.getRoleDesc()))
                .effect(Policy.Effect.ALLOW)
                .priority(500)
                .targets(targetDtos)
                .rules(List.of(ruleDto))
                .build();

        // 4. 기존 정책이 있는지 확인하여 ID 설정 (업데이트를 위함)
        policyRepository.findByName(policyName)
                .ifPresent(existingPolicy -> policyDto.setId(existingPolicy.getId()));

        // 5. PolicyService에 DTO를 전달하여 정책 생성 또는 업데이트를 '요청'합니다.
        if (policyDto.getId() != null) {
            policyService.updatePolicy(policyDto);
        } else {
            policyService.createPolicy(policyDto);
        }

        log.info("Policy for role '{}' has been synchronized.", role.getRoleName());
    }
}