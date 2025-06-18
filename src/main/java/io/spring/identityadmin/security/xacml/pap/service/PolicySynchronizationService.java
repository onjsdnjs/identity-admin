package io.spring.identityadmin.security.xacml.pap.service;

import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.entity.Role;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.repository.PolicyRepository;
import io.spring.identityadmin.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    private final ModelMapper modelMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchronizePolicyForRole(Long roleId) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        // 1. 정책의 '명세'인 PolicyDto를 생성합니다.
        String policyName = "AUTO_POLICY_FOR_" + role.getRoleName();

        // 대상(Target) DTO 목록 생성
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

        // 규칙(Rule) DTO 생성
        String conditionSpel = String.format("hasAuthority('%s')", role.getRoleName());
        PolicyDto.RuleDto ruleDto = new PolicyDto.RuleDto("Auto-generated rule for " + role.getRoleName(), List.of(conditionSpel));

        // 최종 PolicyDto 생성
        PolicyDto policyDto = PolicyDto.builder()
                .name(policyName)
                .description(String.format("'%s' 역할을 위한 자동 생성 정책", role.getRoleDesc()))
                .effect(Policy.Effect.ALLOW)
                .priority(500)
                .targets(targetDtos)
                .rules(List.of(ruleDto))
                .build();

        // 2. 기존에 자동 생성된 정책이 있는지 확인하여 ID를 설정 (업데이트를 위함)
        policyRepository.findByName(policyName)
                .ifPresent(existingPolicy -> policyDto.setId(existingPolicy.getId()));

        // 3. PolicyService에 DTO를 전달하여 정책 생성 또는 업데이트를 '요청'합니다.
        if(policyDto.getId() != null) {
            policyService.updatePolicy(policyDto);
        } else {
            policyService.createPolicy(policyDto);
        }

        log.info("Policy specification for role '{}' has been sent to PolicyService for synchronization.", role.getRoleName());
    }
}
