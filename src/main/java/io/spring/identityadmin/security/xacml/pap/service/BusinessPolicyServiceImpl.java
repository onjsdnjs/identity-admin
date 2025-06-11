package io.spring.identityadmin.security.xacml.pap.service;

import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import io.spring.identityadmin.domain.entity.ConditionTemplate;
import io.spring.identityadmin.domain.entity.business.BusinessResource;
import io.spring.identityadmin.domain.entity.business.BusinessResourceAction;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.domain.entity.policy.PolicyCondition;
import io.spring.identityadmin.domain.entity.policy.PolicyRule;
import io.spring.identityadmin.domain.entity.policy.PolicyTarget;
import io.spring.identityadmin.repository.BusinessActionRepository;
import io.spring.identityadmin.repository.BusinessResourceActionRepository;
import io.spring.identityadmin.repository.BusinessResourceRepository;
import io.spring.identityadmin.repository.ConditionTemplateRepository;
import io.spring.identityadmin.repository.UserRepository;
import io.spring.identityadmin.security.xacml.pep.CustomDynamicAuthorizationManager;
import io.spring.identityadmin.repository.GroupRepository;
import io.spring.identityadmin.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessPolicyServiceImpl implements BusinessPolicyService {

    private final PolicyRepository policyRepository;
    private final BusinessActionRepository businessActionRepository;
    private final BusinessResourceRepository businessResourceRepository;
    private final ConditionTemplateRepository conditionTemplateRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final CustomDynamicAuthorizationManager authorizationManager;
    private final BusinessResourceActionRepository businessResourceActionRepository;

    @Override
    @Transactional
    public Policy createPolicyFromBusinessRule(BusinessPolicyDto dto) {
        Policy policy = new Policy();
        // DTO를 기반으로 새로운 Policy 엔티티를 채움
        updatePolicyFromDto(policy, dto);
        Policy savedPolicy = policyRepository.save(policy);
        authorizationManager.reload();
        return savedPolicy;
    }

    @Override
    @Transactional
    public Policy updatePolicyFromBusinessRule(Long policyId, BusinessPolicyDto dto) {
        Policy existingPolicy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found with id: " + policyId));
        // DTO를 기반으로 기존 Policy 엔티티를 업데이트
        updatePolicyFromDto(existingPolicy, dto);
        Policy savedPolicy = policyRepository.save(existingPolicy);
        authorizationManager.reload();
        return savedPolicy;
    }

    @Override
    public BusinessPolicyDto getBusinessRuleForPolicy(Long policyId) {
        return null;
    }

    // BusinessPolicyDto를 Policy 엔티티로 번역하고 채우는 핵심 로직
    private void updatePolicyFromDto(Policy policy, BusinessPolicyDto dto) {
        policy.setName(dto.getPolicyName());
        policy.setDescription(dto.getDescription());
        policy.setEffect(Policy.Effect.ALLOW); // UI에서 선택하도록 확장 가능
        policy.setPriority(100); // UI에서 입력받도록 확장 가능

        List<String> conditions = new ArrayList<>();

        // 1. 주체(Subject) 조건 생성
        if (!CollectionUtils.isEmpty(dto.getSubjectUserIds())) {
            String userConditions = dto.getSubjectUserIds().stream()
                    .map(id -> userRepository.findById(id).orElseThrow().getUsername())
                    .map(username -> String.format("authentication.name == '%s'", username))
                    .collect(Collectors.joining(" or "));
            conditions.add("(" + userConditions + ")");
        }
        if (!CollectionUtils.isEmpty(dto.getSubjectGroupIds())) {
            String groupConditions = dto.getSubjectGroupIds().stream()
                    .map(id -> groupRepository.findById(id).orElseThrow().getName())
                    .map(groupName -> String.format("hasAuthority('GROUP_%s')", groupName.toUpperCase()))
                    .collect(Collectors.joining(" or "));
            conditions.add("(" + groupConditions + ")");
        }

        // 2. 행위(Action) + 자원(Resource) 조건 생성 -> hasAuthority(PERMISSION_NAME)
        Long resourceId = dto.getBusinessResourceId();
        Long actionId = dto.getBusinessActionId();

        // BusinessResourceActionId를 만들어 중간 테이블에서 매핑 정보를 찾음
        BusinessResourceAction.BusinessResourceActionId mappingId = new BusinessResourceAction.BusinessResourceActionId(resourceId, actionId);
        BusinessResourceAction mapping = businessResourceActionRepository.findById(mappingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 자원에 대한 행위가 정의되지 않았습니다."));

        // 매핑 테이블에서 찾은 기술적 권한 이름을 조건으로 추가
        conditions.add(String.format("hasAuthority('%s')", mapping.getMappedPermissionName()));

        // 3. 추가 조건(Contextual Condition) 생성
        if (dto.getConditions() != null) {
            dto.getConditions().forEach((templateId, params) -> {
                ConditionTemplate template = conditionTemplateRepository.findById(templateId).orElseThrow();
                String spel = String.format(template.getSpelTemplate().replace("?", "'%s'"), params.toArray());
                conditions.add(spel);
            });
        }

        // 4. 생성된 모든 조건을 AND로 결합하여 최종 PolicyRule 생성
        policy.getRules().clear();
        PolicyRule finalRule = PolicyRule.builder().policy(policy).description("Auto-generated from Business Rule").build();
        Set<PolicyCondition> finalConditions = conditions.stream()
                .map(condStr -> PolicyCondition.builder().rule(finalRule).expression(condStr).build())
                .collect(Collectors.toSet());
        finalRule.setConditions(finalConditions);
        policy.getRules().add(finalRule);

        // 5. Target 설정
        policy.getTargets().clear();
        BusinessResource resource = businessResourceRepository.findById(dto.getBusinessResourceId()).orElseThrow();
        PolicyTarget target = PolicyTarget.builder()
                .policy(policy).targetType(resource.getResourceType()).targetIdentifier("/**") // 모든 하위 개체를 대상으로 가정, 향후 특정 ID(#target.id)를 받는 로직으로 확장 가능
                .build();
        policy.getTargets().add(target);
    }
}