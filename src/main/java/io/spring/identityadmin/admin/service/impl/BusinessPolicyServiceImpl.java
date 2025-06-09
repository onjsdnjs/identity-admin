package io.spring.identityadmin.admin.service.impl;

import io.spring.identityadmin.admin.repository.*;
import io.spring.identityadmin.admin.service.BusinessPolicyService;
import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.entity.BusinessAction;
import io.spring.identityadmin.entity.BusinessResource;
import io.spring.identityadmin.entity.ConditionTemplate;
import io.spring.identityadmin.entity.policy.Policy;
import io.spring.identityadmin.entity.policy.PolicyCondition;
import io.spring.identityadmin.entity.policy.PolicyRule;
import io.spring.identityadmin.entity.policy.PolicyTarget;
import io.spring.identityadmin.repository.UserRepository;
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

    private final DefaultPolicyService policyService; // 기존 PAP 서비스
    private final PolicyRepository policyRepository;
    private final BusinessActionRepository businessActionRepository;
    private final BusinessResourceRepository businessResourceRepository;
    private final ConditionTemplateRepository conditionTemplateRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public Policy createPolicyFromBusinessRule(BusinessPolicyDto dto) {
        Policy generatedPolicy = generatePolicy(dto);
        // DTO로 변환하여 기존 생성 로직 호출
        PolicyDto technicalDto = toTechnicalDto(generatedPolicy);
        return policyService.createPolicy(technicalDto);
    }

    @Override
    @Transactional
    public Policy updatePolicyFromBusinessRule(Long policyId, BusinessPolicyDto dto) {
        Policy existingPolicy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found with id: " + policyId));

        // 기존 Policy 엔티티를 DTO 기반으로 업데이트
        updatePolicy(existingPolicy, dto);

        // DTO로 변환하여 기존 수정 로직 호출
        PolicyDto technicalDto = toTechnicalDto(existingPolicy);
        return policyService.updatePolicy(technicalDto);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessPolicyDto getBusinessRuleForPolicy(Long policyId) {
        // 이 부분은 기술적 Policy를 다시 비즈니스 DTO로 역변환하는 복잡한 로직이 필요 (향후 구현)
        // 지금은 간단히 예시로 남겨둠
        throw new UnsupportedOperationException("Reverse translation from Technical Policy to Business DTO is not implemented yet.");
    }

    // BusinessPolicyDto를 Policy 엔티티로 번역하는 핵심 로직
    private Policy generatePolicy(BusinessPolicyDto dto) {
        Policy policy = Policy.builder()
                .name(dto.getPolicyName())
                .description(dto.getDescription())
                .effect(Policy.Effect.ALLOW) // 기본값은 허용
                .priority(100) // 기본 우선순위
                .build();

        updatePolicy(policy, dto);
        return policy;
    }

    private void updatePolicy(Policy policy, BusinessPolicyDto dto) {
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
        BusinessAction action = businessActionRepository.findById(dto.getBusinessActionId()).orElseThrow();
        conditions.add(String.format("hasAuthority('%s')", action.getMappedPermissionName()));

        // 3. 추가 조건(Contextual Condition) 생성
        if (dto.getConditions() != null) {
            dto.getConditions().forEach((templateId, params) -> {
                ConditionTemplate template = conditionTemplateRepository.findById(templateId).orElseThrow();
                // 파라미터를 SpEL 템플릿에 채워넣음 (예: hasIpAddress('%s') -> hasIpAddress('192...'))
                String spel = String.format(template.getSpelTemplate().replace("?", "'%s'"), params.toArray());
                conditions.add(spel);
            });
        }

        // 4. 생성된 모든 조건을 AND로 결합하여 최종 PolicyRule 생성
        PolicyRule finalRule = PolicyRule.builder().policy(policy).description("Auto-generated rule").build();
        Set<PolicyCondition> finalConditions = conditions.stream()
                .map(condStr -> PolicyCondition.builder().rule(finalRule).expression(condStr).build())
                .collect(Collectors.toSet());
        finalRule.setConditions(finalConditions);

        policy.getRules().clear();
        policy.getRules().add(finalRule);

        // 5. Target 설정
        BusinessResource resource = businessResourceRepository.findById(dto.getBusinessResourceId()).orElseThrow();
        PolicyTarget target = PolicyTarget.builder()
                .policy(policy).targetType(resource.getResourceType()).targetIdentifier("/**") // 여기서는 모든 하위 경로를 대상으로 가정
                .build();
        policy.getTargets().clear();
        policy.getTargets().add(target);
    }

    /**
     * [완성] Policy 엔티티를 기술적인 PolicyDto로 변환합니다.
     * DefaultPolicyService와의 호환성을 위해 필요합니다.
     */
    private PolicyDto toTechnicalDto(Policy policy) {
        PolicyDto dto = new PolicyDto();
        dto.setId(policy.getId());
        dto.setName(policy.getName());
        dto.setDescription(policy.getDescription());
        dto.setEffect(policy.getEffect());
        dto.setPriority(policy.getPriority());

        // PolicyTarget Set -> TargetDto List -> String List (오래된 DTO 형식에 맞춤)
        List<String> targetStrings = policy.getTargets().stream()
                .map(t -> String.format("%s:%s", t.getTargetType(), t.getTargetIdentifier()))
                .collect(Collectors.toList());
        dto.setTargets(targetStrings);

        // PolicyRule/PolicyCondition Set -> RuleDto List
        List<PolicyDto.RuleDto> ruleDtos = policy.getRules().stream()
                .map(rule -> {
                    PolicyDto.RuleDto ruleDto = new PolicyDto.RuleDto();
                    ruleDto.setDescription(rule.getDescription());
                    ruleDto.setConditions(
                            rule.getConditions().stream()
                                    .map(PolicyCondition::getExpression)
                                    .collect(Collectors.toList())
                    );
                    return ruleDto;
                }).collect(Collectors.toList());
        dto.setRules(ruleDtos);

        return dto;
    }
}