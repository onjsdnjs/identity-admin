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
import io.spring.identityadmin.repository.ConditionTemplateRepository;
import io.spring.identityadmin.repository.PermissionRepository;
import io.spring.identityadmin.repository.PolicyRepository;
import io.spring.identityadmin.repository.RoleRepository;
import io.spring.identityadmin.security.xacml.pep.CustomDynamicAuthorizationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * [최종 완성본] 비즈니스 규칙을 실제 정책으로 변환하고 관리하는 서비스 구현체.
 * '계층적 정책 모델링' 아키텍처를 완벽하게 반영하여, RBAC 관계 설정과
 * 조건부 ABAC 정책 생성을 모두 처리합니다.
 */
@Slf4j
@Service
@Transactional
public class BusinessPolicyServiceImpl implements BusinessPolicyService {

    private final PolicyRepository policyRepository;
    private final RoleService roleService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ConditionTemplateRepository conditionTemplateRepository;
    private final PolicyEnrichmentService policyEnrichmentService;
    private final CustomDynamicAuthorizationManager authorizationManager;

    // 순환 참조 해결을 위한 @Lazy 사용
    public BusinessPolicyServiceImpl(PolicyRepository policyRepository,
                                     @Lazy RoleService roleService, // RoleService는 Lazy 로딩
                                     RoleRepository roleRepository,
                                     PermissionRepository permissionRepository,
                                     ConditionTemplateRepository conditionTemplateRepository,
                                     PolicyEnrichmentService policyEnrichmentService,
                                     CustomDynamicAuthorizationManager authorizationManager) {
        this.policyRepository = policyRepository;
        this.roleService = roleService;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.conditionTemplateRepository = conditionTemplateRepository;
        this.policyEnrichmentService = policyEnrichmentService;
        this.authorizationManager = authorizationManager;
    }

    @Override
    public Policy createPolicyFromBusinessRule(BusinessPolicyDto dto) {
        if (CollectionUtils.isEmpty(dto.getRoleIds()) || CollectionUtils.isEmpty(dto.getPermissionIds())) {
            throw new IllegalArgumentException("정책을 생성하려면 최소 하나 이상의 역할과 권한이 선택되어야 합니다.");
        }
        log.info("'{}' 정책 생성 시작. 대상 역할 ID: {}, 대상 권한 ID: {}", dto.getPolicyName(), dto.getRoleIds(), dto.getPermissionIds());

        // 1. 역할-권한 관계(RBAC)를 먼저 설정합니다.
        updateRolePermissionMappings(dto.getRoleIds(), dto.getPermissionIds());

        // 2. 조건부 정책(ABAC)이 필요한 경우에만 Policy 엔티티를 생성합니다.
        if (!dto.isConditional()) {
            log.info("단순 역할-권한 할당이 완료되었습니다. 별도의 조건부 정책은 생성되지 않았습니다.");
            return null;
        }

        Policy policy = new Policy();
        translateAndApplyDtoToPolicy(policy, dto);

        policyEnrichmentService.enrichPolicyWithFriendlyDescription(policy);

        Policy savedPolicy = policyRepository.save(policy);
        authorizationManager.reload();

        log.info("조건부 정책 '{}'(ID: {})이(가) 성공적으로 생성되었습니다.", savedPolicy.getName(), savedPolicy.getId());
        return savedPolicy;
    }

    @Override
    public Policy updatePolicyFromBusinessRule(Long policyId, BusinessPolicyDto dto) {
        Policy existingPolicy = policyRepository.findByIdWithDetails(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found with id: " + policyId));
        log.info("정책 '{}'(ID: {}) 업데이트 시작.", existingPolicy.getName(), policyId);

        // 1. 역할-권한 관계(RBAC) 업데이트
        updateRolePermissionMappings(dto.getRoleIds(), dto.getPermissionIds());

        // 2. 조건부 정책(ABAC) 내용 업데이트
        translateAndApplyDtoToPolicy(existingPolicy, dto);
        policyEnrichmentService.enrichPolicyWithFriendlyDescription(existingPolicy);

        Policy updatedPolicy = policyRepository.save(existingPolicy);
        authorizationManager.reload();

        log.info("정책 '{}'(ID: {})이(가) 성공적으로 업데이트되었습니다.", updatedPolicy.getName(), updatedPolicy.getId());
        return updatedPolicy;
    }

    /**
     * BusinessPolicyDto를 Policy 엔티티로 번역하고 적용하는 핵심 로직
     */
    private void translateAndApplyDtoToPolicy(Policy policy, BusinessPolicyDto dto) {
        policy.setName(dto.getPolicyName());
        policy.setDescription(dto.getDescription());
        policy.setEffect(dto.getEffect());
        policy.setPriority(100);

        // 기존 Target과 Rule을 모두 초기화하고 DTO 기반으로 새로 설정
        policy.getTargets().clear();
        policy.getRules().clear();

        // 정책 대상(Target) 설정: 선택된 '권한'에 연결된 '기술 리소스'를 대상으로 설정.
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(dto.getPermissionIds()));
        Set<PolicyTarget> targets = permissions.stream()
                .map(Permission::getManagedResource)
                .filter(Objects::nonNull)
                .map(mr -> PolicyTarget.builder()
                        .targetType(mr.getResourceType().name())
                        .targetIdentifier(mr.getResourceIdentifier())
                        .httpMethod(mr.getHttpMethod() != null ? mr.getHttpMethod().name() : "ANY")
                        .build())
                .collect(Collectors.toSet());
        targets.forEach(policy::addTarget);

        // SpEL 규칙(Rule) 생성: 선택된 '역할'이 규칙의 조건(hasAuthority)으로 변환됨.
        String spelCondition = buildSpelCondition(dto);
        if (StringUtils.hasText(spelCondition)) {
            PolicyRule rule = PolicyRule.builder()
                    .description("지능형 빌더에서 생성/수정된 동적 규칙")
                    .build();

            PolicyCondition condition = PolicyCondition.builder()
                    .expression(spelCondition)
                    .build();

            rule.getConditions().add(condition);
            policy.addRule(rule);
        }
    }

    private void updateRolePermissionMappings(Set<Long> roleIds, Set<Long> permissionIdsToAdd) {
        if (CollectionUtils.isEmpty(roleIds)) return;

        for (Long roleId : roleIds) {
            Role role = roleService.getRole(roleId);

            // 기존 권한 목록에 새로운 권한을 추가 (중복은 Set이 알아서 처리)
            List<Long> currentPermissionIds = role.getRolePermissions().stream()
                    .map(rp -> rp.getPermission().getId())
                    .collect(Collectors.toList());

            Set<Long> updatedPermissionIdSet = new HashSet<>(currentPermissionIds);
            updatedPermissionIdSet.addAll(permissionIdsToAdd);

            roleService.updateRole(role, new ArrayList<>(updatedPermissionIdSet));
        }
    }

    private String buildSpelCondition(BusinessPolicyDto dto) {
        List<String> allConditions = new ArrayList<>();

        // 1. 주체(역할) 조건 생성
        List<Role> roles = roleRepository.findAllById(dto.getRoleIds());
        String roleCondition = roles.stream()
                .map(Role::getRoleName)
                .map(name -> String.format("hasAuthority('%s')", name))
                .collect(Collectors.joining(" or "));
        if (StringUtils.hasText(roleCondition)) {
            allConditions.add("(" + roleCondition + ")");
        }

        // 2. AI 리스크 평가 조건
        if (dto.isAiRiskAssessmentEnabled()) {
            allConditions.add(String.format("#ai.assessContext().score >= %.2f", dto.getRequiredTrustScore()));
        }

        // 3. 전문가용 커스텀 SpEL 조건
        if (StringUtils.hasText(dto.getCustomConditionSpel())) {
            allConditions.add("(" + dto.getCustomConditionSpel() + ")");
        }

        // 4. 조건 템플릿 기반 조건
        if (!CollectionUtils.isEmpty(dto.getConditions())) {
            dto.getConditions().forEach((templateId, params) -> {
                ConditionTemplate template = conditionTemplateRepository.findById(templateId)
                        .orElseThrow(() -> new IllegalArgumentException("조건 템플릿을 찾을 수 없습니다: " + templateId));

                Object[] quotedParams = params.stream().map(p -> "'" + p + "'").toArray();
                allConditions.add(String.format(template.getSpelTemplate(), quotedParams));
            });
        }

        // 모든 조건들을 'and'로 결합
        return String.join(" and ", allConditions);
    }

    @Override
    public BusinessPolicyDto getBusinessRuleForPolicy(Long policyId) {
        // TODO: Policy 엔티티를 BusinessPolicyDto로 '역번역'하는 로직 구현 (향후 과제)
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    @Override
    public BusinessPolicyDto translatePolicyToBusinessRule(Long policyId) {
        // TODO: getBusinessRuleForPolicy와 통합 또는 별도 구현 (향후 과제)
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }
}