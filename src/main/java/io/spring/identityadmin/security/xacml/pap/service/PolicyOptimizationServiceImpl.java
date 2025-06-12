package io.spring.identityadmin.security.xacml.pap.service;

import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.domain.entity.policy.PolicyCondition;
import io.spring.identityadmin.repository.PolicyRepository;
import io.spring.identityadmin.security.xacml.pap.dto.DuplicatePolicyDto;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [최종 구현] 정책 최적화 서비스의 완전한 구현체입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyOptimizationServiceImpl implements PolicyOptimizationService {

    private final PolicyRepository policyRepository;
    private final ModelMapper modelMapper;

    /**
     * [최종 로직 구현] 정책의 '서명'을 생성하여 기능적으로 동일한 중복 정책을 탐지합니다.
     */
    @Override
    public List<DuplicatePolicyDto> findDuplicatePolicies() {
        List<Policy> policies = policyRepository.findAllWithDetails();

        // 정책의 '서명(Signature)'을 키로, 해당 서명을 가진 정책 ID 리스트를 값으로 하는 맵을 생성
        Map<String, List<Long>> signatureMap = policies.stream()
                .collect(Collectors.groupingBy(
                        this::createPolicySignature,
                        Collectors.mapping(Policy::getId, Collectors.toList())
                ));

        // 서명이 동일한 정책 그룹(ID가 2개 이상)을 찾아 DTO로 변환
        return signatureMap.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                // [정상 동작 확인] DTO 생성자의 인자 순서(String, List<Long>, String)와 일치합니다.
                .map(entry -> new DuplicatePolicyDto("동일한 대상과 규칙을 가진 중복 정책", entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());
    }

    /**
     * 정책의 핵심 요소를 정규화하고 정렬하여 고유한 '서명' 문자열을 생성합니다.
     * 서명이 동일하면 두 정책은 기능적으로 동일하다고 판단할 수 있습니다.
     */
    private String createPolicySignature(Policy policy) {
        // 효과 (ALLOW | DENY)
        String effect = policy.getEffect().name();

        // 대상 (Targets) - 정렬하여 일관성 유지
        String targets = policy.getTargets().stream()
                .map(t -> t.getTargetType() + ":" + t.getTargetIdentifier() + ":" + t.getHttpMethod())
                .sorted()
                .collect(Collectors.joining(","));

        // 조건 (Conditions) - 정렬하여 일관성 유지
        String conditions = policy.getRules().stream()
                .flatMap(rule -> rule.getConditions().stream())
                .map(PolicyCondition::getExpression)
                .sorted()
                .collect(Collectors.joining("&&"));

        return String.join("|", effect, targets, conditions);
    }

    /**
     * [최종 로직 구현] 여러 정책을 하나로 병합하는 제안을 생성합니다.
     * 동일한 대상과 효과를 가지지만, 주체만 다른 정책들을 병합하는 시나리오를 처리합니다.
     */
    @Override
    public PolicyDto proposeMerge(List<Long> policyIds) {
        if (CollectionUtils.isEmpty(policyIds) || policyIds.size() < 2) {
            throw new IllegalArgumentException("병합하려면 두 개 이상의 정책이 필요합니다.");
        }
        List<Policy> policiesToMerge = policyRepository.findAllById(policyIds);
        if (policiesToMerge.size() != policyIds.size()) {
            throw new IllegalArgumentException("일부 정책을 찾을 수 없습니다.");
        }

        Policy firstPolicy = policiesToMerge.getFirst();
        String commonTargetSignature = createTargetSignature(firstPolicy);
        Policy.Effect commonEffect = firstPolicy.getEffect();

        // 모든 정책이 동일한 대상과 효과를 갖는지 확인
        for (Policy policy : policiesToMerge) {
            if (!commonEffect.equals(policy.getEffect()) || !commonTargetSignature.equals(createTargetSignature(policy))) {
                throw new IllegalArgumentException("대상 또는 효과가 다른 정책들은 병합할 수 없습니다.");
            }
        }

        // 각 정책의 조건들을 'or'로 결합
        String mergedCondition = policiesToMerge.stream()
                .flatMap(p -> p.getRules().stream())
                .flatMap(r -> r.getConditions().stream())
                .map(PolicyCondition::getExpression)
                .map(expr -> "(" + expr + ")")
                .distinct() // 중복 조건 제거
                .collect(Collectors.joining(" or "));

        // 병합된 새로운 규칙 DTO 생성
        PolicyDto.RuleDto mergedRule = PolicyDto.RuleDto.builder()
                .description("ID " + policyIds + " 정책들로부터 병합됨")
                .conditions(List.of(mergedCondition))
                .build();

        // 병합된 최종 정책 DTO 생성
        return PolicyDto.builder()
                .name("Merged-Policy-" + String.join("-", policyIds.stream().map(String::valueOf).toList()))
                .description("여러 정책이 하나로 병합되었습니다.")
                .effect(commonEffect)
                .priority(firstPolicy.getPriority()) // 우선순위는 첫 정책 기준
                .targets(firstPolicy.getTargets().stream().map(t -> modelMapper.map(t, PolicyDto.TargetDto.class)).toList())
                .rules(List.of(mergedRule))
                .build();
    }

    private String createTargetSignature(Policy policy) {
        return policy.getTargets().stream()
                .map(t -> t.getTargetType() + ":" + t.getTargetIdentifier())
                .sorted()
                .collect(Collectors.joining(","));
    }
}