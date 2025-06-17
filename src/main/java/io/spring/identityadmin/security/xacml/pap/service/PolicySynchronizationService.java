package io.spring.identityadmin.security.xacml.pap.service;

import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.domain.entity.Role;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.domain.entity.policy.PolicyCondition;
import io.spring.identityadmin.domain.entity.policy.PolicyRule;
import io.spring.identityadmin.domain.entity.policy.PolicyTarget;
import io.spring.identityadmin.repository.PolicyRepository;
import io.spring.identityadmin.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicySynchronizationService {

    private final PolicyRepository policyRepository;
    private final RoleRepository roleRepository;
    private final PolicyService policyService;
    private final ModelMapper modelMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 별도의 트랜잭션으로 동작 보장
    public void synchronizePolicyForRole(Long roleId) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        String policyName = "AUTO_POLICY_FOR_" + role.getRoleName();
        Policy policy = policyRepository.findByName(policyName)
                .orElseGet(() -> Policy.builder()
                        .name(policyName)
                        .effect(Policy.Effect.ALLOW)
                        .priority(500) // 자동 생성 정책은 중간 우선순위
                        .build());

        policy.setDescription(String.format("'%s' 역할을 위한 자동 생성 정책", role.getRoleDesc()));

        // 규칙(Rule) 설정: 해당 역할을 가졌는지만 확인
        policy.getRules().clear();
        PolicyRule rule = PolicyRule.builder().policy(policy).build();
        String conditionSpel = String.format("hasAuthority('%s')", role.getRoleName());
        rule.setConditions(Set.of(PolicyCondition.builder().rule(rule).expression(conditionSpel).build()));
        policy.getRules().add(rule);

        // 대상(Target) 설정: 역할에 포함된 모든 권한의 리소스를 대상으로 함
        Set<PolicyTarget> targets = role.getRolePermissions().stream()
                .map(rp -> rp.getPermission())
                .map(Permission::getManagedResource)
                .filter(Objects::nonNull)
                .map(mr -> PolicyTarget.builder()
                        .policy(policy)
                        .targetType(mr.getResourceType().name())
                        .httpMethod(mr.getHttpMethod() != null ? mr.getHttpMethod().name() : null)
                        .targetIdentifier(mr.getResourceIdentifier())
                        .build())
                .collect(Collectors.toSet());

        policy.getTargets().clear();
        policy.getTargets().addAll(targets);
        PolicyDto policyDto = modelMapper.map(policy, PolicyDto.class);
        policyService.createPolicy(policyDto);

        log.info("Policy for role '{}' has been synchronized.", role.getRoleName());
    }
}
