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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyOptimizationServiceImpl implements PolicyOptimizationService {

    private final PolicyRepository policyRepository;
    private final ModelMapper modelMapper;

    /**
     * [로직 변경] Mock 구현을 실제 규칙 기반 로직으로 대체합니다.
     * 1. DB에서 모든 정책과 그 상세 정보를 로드합니다.
     * 2. 각 정책의 '핵심 구성 요소'(효과, 타겟, 규칙)를 정규화하고 정렬하여 고유한 '정책 서명(Signature)' 문자열을 생성합니다.
     * 3. 이 '서명'이 동일한 정책들을 그룹화하여, 기능적으로 완벽히 중복되는 정책 목록을 찾아냅니다.
     */
    @Override
    public List<DuplicatePolicyDto> findDuplicatePolicies() {
        List<Policy> policies = policyRepository.findAllWithDetails();

        Map<String, List<Long>> signatureMap = policies.stream()
                .collect(Collectors.groupingBy(
                        this::createPolicySignature,
                        Collectors.mapping(Policy::getId, Collectors.toList())
                ));

        return signatureMap.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> new DuplicatePolicyDto("동일한 대상과 규칙을 가진 중복 정책", entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());
    }

    private String createPolicySignature(Policy policy) {
        String effect = policy.getEffect().name();

        String targets = policy.getTargets().stream()
                .map(t -> t.getTargetType() + ":" + t.getTargetIdentifier() + ":" + t.getHttpMethod())
                .sorted()
                .collect(Collectors.joining(","));

        String conditions = policy.getRules().stream()
                .flatMap(rule -> rule.getConditions().stream())
                .map(PolicyCondition::getExpression)
                .sorted()
                .collect(Collectors.joining("&&"));

        return String.join("|", effect, targets, conditions);
    }

    /**
     * [로직 변경] TODO를 제거하고 실제 병합 제안 로직을 구현합니다.
     * 1. 병합 대상 정책들을 DB에서 조회합니다.
     * 2. 모든 정책이 동일한 '대상(Target)'과 '효과(Effect)'를 갖는지 검증합니다.
     * 3. 각 정책의 '주체' 관련 조건(예: hasAuthority('GROUP_A'))을 추출합니다.
     * 4. 추출된 주체 조건들을 'or'로 결합하여 새로운 규칙을 생성합니다.
     * 5. 병합된 새로운 정책 DTO를 생성하여 반환합니다.
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
                .collect(Collectors.joining(" or "));

        PolicyDto.RuleDto mergedRule = PolicyDto.RuleDto.builder()
                .description("ID " + policyIds + " 정책들로부터 병합됨")
                .conditions(List.of(mergedCondition))
                .build();

        return PolicyDto.builder()
                .name("Merged-Policy-" + policyIds.hashCode())
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