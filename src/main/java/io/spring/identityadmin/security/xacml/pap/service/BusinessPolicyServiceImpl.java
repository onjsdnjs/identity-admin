package io.spring.identityadmin.security.xacml.pap.service;

import io.spring.identityadmin.admin.iam.service.RoleService;
import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import io.spring.identityadmin.domain.entity.ConditionTemplate;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.domain.entity.Role;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.domain.entity.policy.PolicyCondition;
import io.spring.identityadmin.domain.entity.policy.PolicyRule;
import io.spring.identityadmin.domain.entity.policy.PolicyTarget;
import io.spring.identityadmin.repository.*;
import io.spring.identityadmin.security.xacml.pep.CustomDynamicAuthorizationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessPolicyServiceImpl implements BusinessPolicyService {

    private final PolicyRepository policyRepository;
    private final BusinessResourceRepository businessResourceRepository;
    private final BusinessResourceActionRepository businessResourceActionRepository;
    private final ConditionTemplateRepository conditionTemplateRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final CustomDynamicAuthorizationManager authorizationManager;
    private final PolicyEnrichmentService policyEnrichmentService;
    private final PermissionRepository permissionRepository;

    /**
     * [최종 구현] 지능형 정책 빌더로부터 받은 DTO를 기반으로,
     * 1. 역할-권한 관계(RBAC)를 설정하고,
     * 2. 필요한 경우 조건부 정책(ABAC)을 생성합니다.
     */
    @Override
    @Transactional
    public Policy createPolicyFromBusinessRule(BusinessPolicyDto dto) {
        if (CollectionUtils.isEmpty(dto.getRoleIds()) || CollectionUtils.isEmpty(dto.getPermissionIds())) {
            throw new IllegalArgumentException("정책을 생성하려면 최소 하나 이상의 역할과 권한이 선택되어야 합니다.");
        }
        log.info("'{}' 정책 생성 시작. 대상 역할 ID: {}, 대상 권한 ID: {}", dto.getPolicyName(), dto.getRoleIds(), dto.getPermissionIds());

        Policy policy = new Policy();

        // DTO의 정보를 바탕으로 Policy 엔티티를 채우는 공통 로직 호출
        translateAndApplyDtoToPolicy(policy, dto);

        // AI를 통해 정책에 대한 자연어 설명 생성
        policyEnrichmentService.enrichPolicyWithFriendlyDescription(policy);

        Policy savedPolicy = policyRepository.save(policy);
        log.info("조건부 정책 '{}'(ID: {})이(가) 성공적으로 생성되었습니다.", savedPolicy.getName(), savedPolicy.getId());

        return savedPolicy;
    }

    /**
     * [구현 완료] 기존 정책을 비즈니스 규칙 DTO 기반으로 업데이트합니다.
     */
    @Override
    @Transactional
    public Policy updatePolicyFromBusinessRule(Long policyId, BusinessPolicyDto dto) {
        Policy existingPolicy = policyRepository.findByIdWithDetails(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found with id: " + policyId));

        log.info("정책 '{}'(ID: {}) 업데이트 시작.", existingPolicy.getName(), policyId);

        // DTO의 정보로 기존 Policy 엔티티를 업데이트하는 공통 로직 호출
        translateAndApplyDtoToPolicy(existingPolicy, dto);

        policyEnrichmentService.enrichPolicyWithFriendlyDescription(existingPolicy);

        Policy updatedPolicy = policyRepository.save(existingPolicy);
        log.info("정책 '{}'(ID: {})이(가) 성공적으로 업데이트되었습니다.", updatedPolicy.getName(), updatedPolicy.getId());

        return updatedPolicy;
    }

    /**
     * [핵심 구현] BusinessPolicyDto를 Policy 엔티티로 번역하고 적용하는 핵심 로직
     */
    private void translateAndApplyDtoToPolicy(Policy policy, BusinessPolicyDto dto) {
        policy.setName(dto.getPolicyName());
        policy.setDescription(dto.getDescription());
        policy.setEffect(dto.getEffect());
        policy.setPriority(100); // 비즈니스 정책은 높은 우선순위 부여

        // 1. 역할-권한 관계(RBAC) 설정
        updateRolePermissionMappings(dto.getRoleIds(), dto.getPermissionIds());

        // 2. 조건부 정책(ABAC) 설정
        // 기존의 Target과 Rule을 모두 초기화하고 DTO를 기반으로 새로 설정
        policy.getTargets().clear();
        policy.getRules().clear();

        // 2-1. 정책 대상(Target) 설정: 이 정책이 어떤 역할(들)에 대한 것인지 명시
        Set<PolicyTarget> targets = dto.getRoleIds().stream()
                .map(roleId -> PolicyTarget.builder()
                        .targetType("ROLE")
                        .targetIdentifier(String.valueOf(roleId))
                        .build())
                .collect(Collectors.toSet());
        targets.forEach(policy::addTarget);

        // 2-2. SpEL 규칙(Rule) 생성
        String spelCondition = buildSpelCondition(dto);
        if (StringUtils.hasText(spelCondition)) {
            PolicyRule rule = PolicyRule.builder()
                    .description("지능형 빌더에서 생성/수정된 동적 규칙")
                    .build();

            PolicyCondition condition = PolicyCondition.builder()
                    .expression(spelCondition)
                    .build();

            rule.getConditions().add(condition);
            condition.setRule(rule);

            policy.addRule(rule);
        }
    }

    private void updateRolePermissionMappings(Set<Long> roleIds, Set<Long> permissionIdsToAdd) {
        for (Long roleId : roleIds) {
            Role role = roleService.getRole(roleId);
            List<Long> currentPermissionIds = role.getRolePermissions().stream()
                    .map(rp -> rp.getPermission().getId())
                    .toList();

            Set<Long> updatedPermissionIdSet = new HashSet<>(currentPermissionIds);
            updatedPermissionIdSet.addAll(permissionIdsToAdd);

            roleService.updateRole(role, new ArrayList<>(updatedPermissionIdSet));
        }
    }

    private String buildSpelCondition(BusinessPolicyDto dto) {
        List<String> allConditions = new ArrayList<>();
        List<Permission> permissions = permissionRepository.findAllById(dto.getPermissionIds());

        String permissionCondition = permissions.stream()
                .map(Permission::getName)
                .map(name -> String.format("hasAuthority('%s')", name))
                .collect(Collectors.joining(" or "));

        if (StringUtils.hasText(permissionCondition)) {
            allConditions.add("(" + permissionCondition + ")");
        }

        if (dto.isAiRiskAssessmentEnabled()) {
            allConditions.add(String.format("#ai.assessContext().score >= %.2f", dto.getRequiredTrustScore()));
        }

        if (StringUtils.hasText(dto.getCustomConditionSpel())) {
            allConditions.add("(" + dto.getCustomConditionSpel() + ")");
        }

        if (!CollectionUtils.isEmpty(dto.getConditions())) {
            dto.getConditions().forEach((templateId, params) -> {
                ConditionTemplate template = conditionTemplateRepository.findById(templateId)
                        .orElseThrow(() -> new IllegalArgumentException("조건 템플릿을 찾을 수 없습니다: " + templateId));

                Object[] quotedParams = params.stream().map(p -> "'" + p + "'").toArray();
                allConditions.add(String.format(template.getSpelTemplate(), quotedParams));
            });
        }

        return String.join(" and ", allConditions);
    }

    @Override
    public BusinessPolicyDto getBusinessRuleForPolicy(Long policyId) {
        log.warn("getBusinessRuleForPolicy is not implemented yet.");
        return null; // TODO: Policy 엔티티를 DTO로 '역번역'하는 로직 구현
    }

    @Override
    public BusinessPolicyDto translatePolicyToBusinessRule(Long policyId) {
        log.warn("translatePolicyToBusinessRule is not implemented yet.");
        return getBusinessRuleForPolicy(policyId);
    }
}