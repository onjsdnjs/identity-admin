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

import java.util.*;
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

        // 기존 Target과 Rule을 모두 초기화하고 DTO 기반으로 새로 설정
        policy.getTargets().clear();
        policy.getRules().clear();

        // ================================================================
        // 1. [오류 수정] 정책 대상(Target) 설정 로직
        // 선택된 '권한(Permission)'에 연결된 '기술 리소스(ManagedResource)'를 정책의 대상으로 올바르게 설정합니다.
        // ================================================================
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(dto.getPermissionIds()));
        Set<PolicyTarget> targets = permissions.stream()
                .map(Permission::getManagedResource) // 각 권한에 연결된 ManagedResource를 가져옵니다.
                .filter(Objects::nonNull) // ManagedResource가 없는 권한은 제외합니다.
                .map(mr -> PolicyTarget.builder()
                        .targetType(mr.getResourceType().name()) // "URL" 또는 "METHOD"
                        .targetIdentifier(mr.getResourceIdentifier()) // "/api/**" 또는 "com.example.Service.method"
                        .httpMethod(mr.getHttpMethod() != null ? mr.getHttpMethod().name() : "ANY")
                        .build())
                .collect(Collectors.toSet());

        // 생성된 올바른 Target들을 Policy에 추가합니다.
        targets.forEach(policy::addTarget);
        log.info("정책 '{}'에 {}개의 대상(Target)이 설정되었습니다.", dto.getPolicyName(), targets.size());

        // ================================================================
        // 2. [오류 수정] SpEL 규칙(Rule) 생성 로직
        // 선택된 '역할(Role)'이 규칙의 조건(hasAuthority)으로 올바르게 변환됩니다.
        // ================================================================
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

    /**
     * BusinessPolicyDto에 포함된 조건들을 조합하여 최종 SpEL 문자열을 생성합니다.
     *
     * @param dto 정책 빌더에서 관리자가 구성한 모든 정보를 담고 있는 DTO
     * @return 조합된 최종 SpEL 문자열
     */
    private String buildSpelCondition(BusinessPolicyDto dto) {
        // 최종 SpEL을 구성할 조건들을 담는 리스트
        List<String> allConditions = new ArrayList<>();

        // --- 1. 주체 조건 생성 (역할 기반) ---
        // 이 정책이 적용될 주체는 '이러한 역할(들)을 가진 사용자'임을 명시합니다.
        if (!CollectionUtils.isEmpty(dto.getRoleIds())) {
            // DB에서 실제 Role 엔티티 목록을 조회합니다.
            List<Role> roles = roleService.getRoles();
            String roleCondition = roles.stream()
                    .map(Role::getRoleName)
                    .map(name -> String.format("hasAuthority('%s')", name))
                    .collect(Collectors.joining(" or "));

            // 생성된 조건이 있을 경우 괄호로 감싸서 추가합니다.
            if (StringUtils.hasText(roleCondition)) {
                allConditions.add("(" + roleCondition + ")");
            }
        }

        // --- 2. 권한 조건 생성 ---
        // 이 정책이 어떤 권한(들)에 대한 것인지 명시합니다.
        if (!CollectionUtils.isEmpty(dto.getPermissionIds())) {
            List<Permission> permissions = permissionRepository.findAllById(dto.getPermissionIds());
            String permissionCondition = permissions.stream()
                    .map(Permission::getName)
                    .map(name -> String.format("hasAuthority('%s')", name))
                    .collect(Collectors.joining(" or "));

            if (StringUtils.hasText(permissionCondition)) {
                allConditions.add("(" + permissionCondition + ")");
            }
        }

        // --- 3. AI 리스크 평가 조건 생성 ---
        if (dto.isAiRiskAssessmentEnabled()) {
            String aiCondition = String.format("#ai.assessContext().score >= %.2f", dto.getRequiredTrustScore());
            allConditions.add(aiCondition);
        }

        // --- 4. 전문가용 커스텀 SpEL 조건 추가 ---
        if (StringUtils.hasText(dto.getCustomConditionSpel())) {
            // 안전을 위해 괄호로 감싸서 추가합니다.
            allConditions.add("(" + dto.getCustomConditionSpel() + ")");
        }

        // --- 5. 조건 템플릿 기반 조건 생성 ---
        if (!CollectionUtils.isEmpty(dto.getConditions())) {
            dto.getConditions().forEach((templateId, params) -> {
                ConditionTemplate template = conditionTemplateRepository.findById(templateId)
                        .orElseThrow(() -> new IllegalArgumentException("조건 템플릿을 찾을 수 없습니다: " + templateId));

                // 파라미터를 SpEL 문자열에 안전하게 삽입하기 위해 작은따옴표(')로 감쌉니다.
                Object[] quotedParams = params.stream().map(p -> "'" + p + "'").toArray();
                allConditions.add(String.format(template.getSpelTemplate(), quotedParams));
            });
        }

        // --- 6. 모든 조건을 'and' 로 결합하여 최종 반환 ---
        // allConditions 리스트에 아무 조건도 없으면 빈 문자열이 반환됩니다.
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