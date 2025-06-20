package io.spring.identityadmin.security.xacml.pap.service;

import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import io.spring.identityadmin.domain.entity.ConditionTemplate;
import io.spring.identityadmin.domain.entity.Users;
import io.spring.identityadmin.domain.entity.business.BusinessResource;
import io.spring.identityadmin.domain.entity.business.BusinessResourceAction;
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
    private final GroupRepository groupRepository;
    private final CustomDynamicAuthorizationManager authorizationManager;
    private final PolicyEnrichmentService policyEnrichmentService;

    /**
     * BusinessPolicyDto를 기반으로 새로운 접근 정책을 생성합니다.
     * 이 메서드는 AINativeIAMSynapseArbiter에서 자연어 정책 생성을 위해 호출될 수 있습니다.
     *
     * @param dto 정책 생성을 위한 비즈니스 요구사항이 담긴 DTO
     * @return 생성된 Policy 엔티티
     */
    @Override
    public Policy createBusinessPolicy(BusinessPolicyDto dto) {
        log.info("비즈니스 규칙에 기반한 정책 생성을 시작합니다: {}", dto.getPolicyName());

        Policy policy = new Policy();
        policy.setName(dto.getPolicyName());
        policy.setDescription(dto.getDescription());
        policy.setEffect(dto.getEffect());

        // 1. Policy의 대상을 정의하는 PolicyTarget 엔티티들을 생성합니다.
        Set<PolicyTarget> targets = createTargetsFromSubjects(dto.getSubjects());
        policy.setTargets(targets);
        targets.forEach(target -> target.setPolicy(policy)); // 양방향 연관관계 설정

        // 2. 최종 SpEL 조건 문자열을 조립합니다.
        String finalCondition = buildSpelCondition(dto);
        policy.setCondition(finalCondition);

        // 3. 완성된 Policy 엔티티를 저장하고 반환합니다.
        Policy savedPolicy = policyRepository.save(policy);
        log.info("새로운 비즈니스 정책이 성공적으로 생성되었습니다. Policy ID: {}", savedPolicy.getId());

        return savedPolicy;
    }

    /**
     * BusinessPolicyDto에 포함된 정보를 바탕으로 최종 SpEL 조건 문자열을 생성합니다.
     *
     * @param dto 비즈니스 정책 DTO
     * @return 조합된 최종 SpEL 문자열
     */
    private String buildSpelCondition(BusinessPolicyDto dto) {
        StringBuilder conditionBuilder = new StringBuilder();

        // 1. 기본 조건 생성 (예: 리소스 및 행위에 대한 권한 확인)
        // 이 예제에서는 리소스와 액션을 조합하여 'hasAuthority'로 변환합니다.
        // 예: PERM_CUSTOMER_DATA_READ_VIEW
        String basePermissionCondition = createBasePermissionCondition(dto.getResources(), dto.getActions());
        if (StringUtils.hasText(basePermissionCondition)) {
            conditionBuilder.append(basePermissionCondition);
        }

        // 2. AI 리스크 평가 활성화 여부에 따라 AI 조건 추가
        if (dto.isAiRiskAssessmentEnabled()) {
            if (!conditionBuilder.isEmpty()) {
                conditionBuilder.append(" and ");
            }
            // 예시: #ai.assessContext().score >= 0.7
            String aiCondition = String.format("#ai.assessContext().score >= %.2f", dto.getRequiredTrustScore());
            conditionBuilder.append(aiCondition);
            log.debug("AI 리스크 평가 조건이 추가되었습니다: {}", aiCondition);
        }

        // 3. 관리자가 직접 입력한 추가 SpEL 조건이 있다면 AND로 연결
        if (StringUtils.hasText(dto.getCustomConditionSpel())) {
            if (!conditionBuilder.isEmpty()) {
                conditionBuilder.append(" and ");
            }
            conditionBuilder.append("(").append(dto.getCustomConditionSpel()).append(")");
            log.debug("사용자 정의 SpEL 조건이 추가되었습니다: {}", dto.getCustomConditionSpel());
        }

        return conditionBuilder.toString();
    }

    /**
     * 서브젝트 문자열 목록(예: "GROUP_DEV", "ROLE_MANAGER")을
     * PolicyTarget 엔티티 Set으로 변환합니다.
     *
     * @param subjects 서브젝트 식별자 문자열 목록
     * @return PolicyTarget 엔티티 Set
     */
    private Set<PolicyTarget> createTargetsFromSubjects(Set<String> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return new HashSet<>();
        }

        Set<PolicyTarget> targets = new HashSet<>();
        for (String subject : subjects) {
            String[] parts = subject.split("_", 2);
            if (parts.length == 2) {
                String type = parts[0].toUpperCase();
                String idOrName = parts[1]; // 실제로는 name을 id로 변환하는 과정이 필요할 수 있습니다.

                // PolicyTarget.Type enum에 GROUP, ROLE 등이 정의되어 있어야 합니다.
                try {
                    PolicyTarget.Type targetType = PolicyTarget.Type.valueOf(type);
                    targets.add(new PolicyTarget(targetType, idOrName));
                } catch (IllegalArgumentException e) {
                    log.warn("지원하지 않는 서브젝트 타입입니다: {}", type);
                }
            }
        }
        return targets;
    }

    /**
     * 리소스와 액션 목록을 기반으로 기본적인 hasAuthority() SpEL 조건을 생성합니다.
     * (이 부분은 시스템의 권한(Permission) 명명 규칙에 따라 달라집니다)
     *
     * @param resources 리소스 식별자 목록
     * @param actions   액션 식별자 목록
     * @return 생성된 hasAuthority() SpEL 문자열
     */
    private String createBasePermissionCondition(Set<String> resources, Set<String> actions) {
        if (resources == null || resources.isEmpty() || actions == null || actions.isEmpty()) {
            return "";
        }
        // 예시: 리소스가 'CUSTOMER_DATA', 액션이 'READ', 'VIEW'일 경우
        // 'PERM_CUSTOMER_DATA_READ', 'PERM_CUSTOMER_DATA_VIEW' 권한이 있는지 확인하는 SpEL 생성
        return resources.stream()
                .flatMap(resource -> actions.stream().map(action -> String.format("'%s_%s'", resource, action)))
                .map(permission -> String.format("hasAuthority(%s)", permission))
                .collect(Collectors.joining(" or ")); // 여러 권한 중 하나만 있어도 되면 or
    }

    @Override
    @Transactional
    public Policy createPolicyFromBusinessRule(BusinessPolicyDto dto) {
        Policy policy = new Policy();
        translateAndApplyDtoToPolicy(policy, dto);
        policyEnrichmentService.enrichPolicyWithFriendlyDescription(policy); // 설명 자동 생성
        Policy savedPolicy = policyRepository.save(policy);
        authorizationManager.reload(); // 인가 시스템 런타임 갱신
        log.info("Successfully created a new policy '{}' from business rule.", savedPolicy.getName());
        return savedPolicy;
    }

    @Override
    @Transactional
    public Policy updatePolicyFromBusinessRule(Long policyId, BusinessPolicyDto dto) {
        Policy existingPolicy = policyRepository.findByIdWithDetails(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found with id: " + policyId));
        translateAndApplyDtoToPolicy(existingPolicy, dto);
        policyEnrichmentService.enrichPolicyWithFriendlyDescription(existingPolicy); // 설명 자동 생성
        Policy savedPolicy = policyRepository.save(existingPolicy);
        authorizationManager.reload(); // 인가 시스템 런타임 갱신
        log.info("Successfully updated the policy '{}' from business rule.", savedPolicy.getName());
        return savedPolicy;
    }

    @Override
    public BusinessPolicyDto translatePolicyToBusinessRule(Long policyId) {
        // 역번역 기능은 복잡도가 높아 향후 과제로 남겨둡니다.
        // 현재는 생성/수정 기능에 집중합니다.
        throw new UnsupportedOperationException("Translating technical policy back to business rule is not yet implemented.");
    }

    @Override
    public BusinessPolicyDto getBusinessRuleForPolicy(Long policyId) {
        throw new UnsupportedOperationException("Translating technical policy back to business rule is not yet implemented.");
    }

    /**
     * BusinessPolicyDto를 Policy 엔티티로 번역하고 적용하는 핵심 로직
     */
    private void translateAndApplyDtoToPolicy(Policy policy, BusinessPolicyDto dto) {
        policy.setName(dto.getPolicyName());
        policy.setDescription(dto.getDescription());
        policy.setEffect(dto.getEffect());
        policy.setPriority(500); // 비즈니스 정책은 중간 우선순위 부여

        List<String> subjectConditions = new ArrayList<>();
        List<String> actionConditions = new ArrayList<>();
        List<String> contextualConditions = new ArrayList<>();

        // 1. 주체(Subject) 조건 생성
        if (!CollectionUtils.isEmpty(dto.getSubjectUserIds())) {
            subjectConditions.add(dto.getSubjectUserIds().stream()
                    .map(id -> userRepository.findById(id).map(Users::getUsername)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + id)))
                    .map(username -> String.format("authentication.name == '%s'", username))
                    .collect(Collectors.joining(" or ")));
        }
        if (!CollectionUtils.isEmpty(dto.getSubjectGroupIds())) {
            subjectConditions.add(dto.getSubjectGroupIds().stream()
                    .map(id -> String.format("hasAuthority('GROUP_%d')", id)) // CustomUserDetails에서 'GROUP_{ID}' 형태의 권한을 부여해야 함
                    .collect(Collectors.joining(" or ")));
        }

        // 2. 행위(Action) + 자원(Resource) 조건 생성 -> hasAuthority(PERMISSION_NAME)
        if (!CollectionUtils.isEmpty(dto.getBusinessActionIds()) && !CollectionUtils.isEmpty(dto.getBusinessResourceIds())) {
            // 모든 자원/행위 조합에 대한 기술 권한을 찾음
            Set<String> requiredPermissions = new HashSet<>();
            for (Long resourceId : dto.getBusinessResourceIds()) {
                for (Long actionId : dto.getBusinessActionIds()) {
                    BusinessResourceAction.BusinessResourceActionId mappingId = new BusinessResourceAction.BusinessResourceActionId(resourceId, actionId);
                    businessResourceActionRepository.findById(mappingId)
                            .ifPresent(mapping -> requiredPermissions.add(mapping.getMappedPermissionName()));
                }
            }
            if(!requiredPermissions.isEmpty()) {
                actionConditions.add(requiredPermissions.stream()
                        .map(perm -> String.format("hasAuthority('%s')", perm))
                        .collect(Collectors.joining(" and ")));
            }
        }

        // 3. 추가 컨텍스트 조건(Contextual Condition) 생성
        if (dto.getConditions() != null) {
            dto.getConditions().forEach((templateId, params) -> {
                ConditionTemplate template = conditionTemplateRepository.findById(templateId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid ConditionTemplate ID: " + templateId));

                if (template.getParameterCount() != params.size()) {
                    throw new IllegalArgumentException(
                            String.format("Condition '%s' requires %d parameter(s), but %d were provided.",
                                    template.getName(), template.getParameterCount(), params.size())
                    );
                }

                String spel = String.format(template.getSpelTemplate().replace("?", "'%s'"), params.toArray());
                contextualConditions.add(spel);
            });
        }

        // 4. 모든 조건들을 AND로 결합하여 최종 PolicyRule 생성
        List<String> allConditions = new ArrayList<>();
        if (!subjectConditions.isEmpty()) {
            allConditions.add("(" + String.join(" or ", subjectConditions) + ")");
        }
        if (!actionConditions.isEmpty()) {
            allConditions.add("(" + String.join(" and ", actionConditions) + ")");
        }
        allConditions.addAll(contextualConditions);

        policy.getRules().clear();
        PolicyRule finalRule = PolicyRule.builder()
                .policy(policy)
                .description("Generated from Policy Authoring Workbench")
                .build();
        Set<PolicyCondition> finalPolicyConditions = allConditions.stream()
                .map(condStr -> PolicyCondition.builder().rule(finalRule).expression(condStr).build())
                .collect(Collectors.toSet());
        finalRule.setConditions(finalPolicyConditions);
        policy.getRules().add(finalRule);

        // 5. Target 설정
        policy.getTargets().clear();
        if(!CollectionUtils.isEmpty(dto.getBusinessResourceIds())) {
            dto.getBusinessResourceIds().forEach(resourceId -> {
                BusinessResource resource = businessResourceRepository.findById(resourceId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid BusinessResource ID: " + resourceId));
                // TODO: 향후 개별 자원 ID까지 타겟팅 가능하도록 확장. 현재는 타입의 모든 자원을 대상으로 함.
                policy.getTargets().add(PolicyTarget.builder()
                        .policy(policy)
                        .targetType(resource.getResourceType())
                        .targetIdentifier("/**")
                        .build());
            });
        }
    }
}