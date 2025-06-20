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
     * [수정] BusinessPolicyDto를 기반으로 새로운 접근 정책을 생성합니다.
     * DTO의 각 필드를 Policy, PolicyTarget, PolicyRule 엔티티에 올바르게 매핑합니다.
     */
    @Override
    public Policy createPolicyFromBusinessRule(BusinessPolicyDto dto) {
        log.info("비즈니스 규칙 기반 정책 생성을 시작합니다: {}", dto.getPolicyName());

        Policy policy = Policy.builder()
                .name(dto.getPolicyName())
                .description(dto.getDescription())
                .effect(dto.getEffect())
                .priority(50)
                .build();

        // 1. PolicyTarget 엔티티들을 생성하고 Policy에 추가합니다.
        Set<PolicyTarget> targets = createTargetsFromDto(dto);
        targets.forEach(policy::addTarget);

        // 2. PolicyRule과 그 하위의 PolicyCondition 엔티티들을 생성하고 Policy에 추가합니다.
        Set<PolicyRule> rules = createRulesFromDto(dto);
        rules.forEach(policy::addRule);

        Policy savedPolicy = policyRepository.save(policy);
        log.info("새로운 비즈니스 정책이 성공적으로 생성되었습니다. Policy ID: {}", savedPolicy.getId());

        return savedPolicy;
    }

    private Set<PolicyTarget> createTargetsFromDto(BusinessPolicyDto dto) {
        Set<PolicyTarget> targets = new HashSet<>();

        if (!CollectionUtils.isEmpty(dto.getSubjectUserIds())) {
            dto.getSubjectUserIds().forEach(userId -> targets.add(
                    PolicyTarget.builder()
                            .targetType("USER")
                            .targetIdentifier(String.valueOf(userId))
                            .build()
            ));
        }
        if (!CollectionUtils.isEmpty(dto.getSubjectGroupIds())) {
            dto.getSubjectGroupIds().forEach(groupId -> targets.add(
                    PolicyTarget.builder()
                            .targetType("GROUP")
                            .targetIdentifier(String.valueOf(groupId))
                            .build()
            ));
        }
        if (!CollectionUtils.isEmpty(dto.getBusinessResourceIds())) {
            dto.getBusinessResourceIds().forEach(resourceId -> targets.add(
                    PolicyTarget.builder()
                            .targetType("RESOURCE")
                            .targetIdentifier(String.valueOf(resourceId))
                            .build()
            ));
        }
        return targets;
    }

    private Set<PolicyRule> createRulesFromDto(BusinessPolicyDto dto) {
        StringBuilder conditionBuilder = new StringBuilder();

        // 1. 행위(Action)에 대한 SpEL 조건 생성
        if (!CollectionUtils.isEmpty(dto.getBusinessActionIds())) {
            String actionCondition = dto.getBusinessActionIds().stream()
                    .map(actionId -> String.format("hasAction('%d')", actionId))
                    .collect(Collectors.joining(" or "));
            conditionBuilder.append("(").append(actionCondition).append(")");
        }

        // 2. 선택된 조건 템플릿(Condition Templates)을 기반으로 SpEL 조건 생성
        if (!CollectionUtils.isEmpty(dto.getConditions())) {
            for (Map.Entry<Long, List<String>> entry : dto.getConditions().entrySet()) {
                Long templateId = entry.getKey();
                List<String> params = entry.getValue();

                ConditionTemplate template = conditionTemplateRepository.findById(templateId)
                        .orElseThrow(() -> new IllegalArgumentException("조건 템플릿을 찾을 수 없습니다: " + templateId));

                String formattedCondition = String.format(template.getSpelTemplate(), params.toArray());

                if (!conditionBuilder.isEmpty()) {
                    conditionBuilder.append(" and ");
                }
                conditionBuilder.append(formattedCondition);
            }
        }

        // 3. 완성된 SpEL로 PolicyRule과 PolicyCondition 엔티티 생성
        if (!conditionBuilder.isEmpty()) {
            // Rule을 먼저 생성합니다. 'name'이 없으므로 'description'을 사용합니다.
            PolicyRule rule = PolicyRule.builder()
                    .description(dto.getPolicyName() + " Rule")
                    .build();

            // Condition을 생성하고 Rule에 설정합니다.
            PolicyCondition condition = PolicyCondition.builder()
                    .expression(conditionBuilder.toString())
                    .build();

            // Rule에 Condition을 추가하고, 양방향 관계를 설정합니다.
            rule.getConditions().add(condition);
            condition.setRule(rule);

            return Set.of(rule);
        }

        return new HashSet<>();
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