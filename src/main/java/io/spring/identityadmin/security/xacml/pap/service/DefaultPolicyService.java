package io.spring.identityadmin.security.xacml.pap.service;

import io.spring.identityadmin.common.event.dto.PolicyChangedEvent;
import io.spring.identityadmin.common.event.service.IntegrationEventBus;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.domain.entity.policy.PolicyCondition;
import io.spring.identityadmin.domain.entity.policy.PolicyRule;
import io.spring.identityadmin.domain.entity.policy.PolicyTarget;
import io.spring.identityadmin.repository.PolicyRepository;
import io.spring.identityadmin.security.xacml.prp.PolicyRetrievalPoint;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.security.xacml.pep.CustomDynamicAuthorizationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DefaultPolicyService implements PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyRetrievalPoint policyRetrievalPoint;
    private final CustomDynamicAuthorizationManager authorizationManager;
    private final PolicyEnrichmentService policyEnrichmentService;
    private final ModelMapper modelMapper;
    private final IntegrationEventBus eventBus;

    @Override
    @Transactional(readOnly = true)
    public List<Policy> getAllPolicies() {
        return policyRepository.findAllWithDetails();
    }

    @Override
    @Transactional(readOnly = true)
    public Policy findById(Long id) {
        return policyRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found with ID: " + id));
    }

    @Override
    public Policy createPolicy(PolicyDto policyDto) {
        Policy policy = convertDtoToEntity(policyDto);
        policyEnrichmentService.enrichPolicyWithFriendlyDescription(policy);
        Policy savedPolicy = policyRepository.save(policy);
        eventBus.publish(new PolicyChangedEvent(savedPolicy.getId()));
        reloadAuthorizationSystem();
        log.info("Policy created and authorization system reloaded. Policy Name: {}", savedPolicy.getName());
        return savedPolicy;
    }

    @Override
    public void updatePolicy(PolicyDto policyDto) {
        Policy existingPolicy = findById(policyDto.getId());
        policyEnrichmentService.enrichPolicyWithFriendlyDescription(existingPolicy);
        updateEntityFromDto(existingPolicy, policyDto);
        Policy updatedPolicy = policyRepository.save(existingPolicy);
        eventBus.publish(new PolicyChangedEvent(policyDto.getId()));
        reloadAuthorizationSystem();
        log.info("Policy updated and authorization system reloaded. Policy ID: {}", updatedPolicy.getId());
    }

    @Override
    public void deletePolicy(Long id) {
        policyRepository.deleteById(id);
        reloadAuthorizationSystem();
        log.info("Policy deleted and authorization system reloaded. Policy ID: {}", id);
    }

    /**
     * 정책 변경 후 인가 시스템을 다시 로드하는 중앙화된 메서드.
     */
    private void reloadAuthorizationSystem() {
        policyRetrievalPoint.clearUrlPoliciesCache();
        policyRetrievalPoint.clearMethodPoliciesCache(); // 메서드 정책 캐시도 클리어
        authorizationManager.reload();
    }

    // --- DTO <-> Entity 변환 헬퍼 메서드 ---
    private Policy convertDtoToEntity(PolicyDto dto) {
        Policy policy = Policy.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .effect(dto.getEffect())
                .priority(dto.getPriority())
                .build();

        // Target 파싱 로직 (안정성 강화)
        if (dto.getTargets() != null) {
            Set<PolicyTarget> targets = dto.getTargets().stream().map(targetDto ->
                    PolicyTarget.builder()
                            .policy(policy)
                            .targetType(targetDto.getTargetType())
                            .targetIdentifier(targetDto.getTargetIdentifier())
                            .httpMethod("ALL".equals(targetDto.getHttpMethod()) ? null : targetDto.getHttpMethod()) // "ALL"은 null로 저장
                            .build()
            ).collect(Collectors.toSet());
            policy.setTargets(targets);
        }

        // 여러 규칙(Rule)을 처리하는 로직
        if (dto.getRules() != null) {
            Set<PolicyRule> policyRules = dto.getRules().stream().map(ruleDto -> {
                PolicyRule rule = PolicyRule.builder()
                        .policy(policy)
                        .description(ruleDto.getDescription())
                        .build();

                Set<PolicyCondition> conditions = ruleDto.getConditions().stream()
                        .map(conditionStr -> PolicyCondition.builder().rule(rule).expression(conditionStr).build())
                        .collect(Collectors.toSet());

                rule.setConditions(conditions);
                return rule;
            }).collect(Collectors.toSet());
            policy.setRules(policyRules);
        }

        return policy;
    }

    private void updateEntityFromDto(Policy policy, PolicyDto dto) {
        policy.setName(dto.getName());
        policy.setDescription(dto.getDescription());
        policy.setEffect(dto.getEffect());
        policy.setPriority(dto.getPriority());

        // "clear and add all" 전략 사용
        policy.getTargets().clear();
        policy.getRules().clear();

        // Target 변환 로직
        if (dto.getTargets() != null) {
            dto.getTargets().forEach(targetDto -> {
                policy.getTargets().add(PolicyTarget.builder()
                        .policy(policy)
                        .targetType(targetDto.getTargetType())
                        .targetIdentifier(targetDto.getTargetIdentifier())
                        .httpMethod("ALL".equals(targetDto.getHttpMethod()) ? null : targetDto.getHttpMethod())
                        .build());
            });
        }

        // 여러 규칙(Rule)을 처리하는 로직
        if (dto.getRules() != null) {
            dto.getRules().forEach(ruleDto -> {
                PolicyRule rule = PolicyRule.builder()
                        .policy(policy)
                        .description(ruleDto.getDescription())
                        .build();

                Set<PolicyCondition> conditions = ruleDto.getConditions().stream()
                        .map(conditionStr -> PolicyCondition.builder().rule(rule).expression(conditionStr).build())
                        .collect(Collectors.toSet());

                rule.setConditions(conditions);
                policy.getRules().add(rule);
            });
        }
    }
}
