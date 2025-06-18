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