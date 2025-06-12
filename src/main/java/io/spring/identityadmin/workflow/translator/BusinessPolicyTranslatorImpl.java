package io.spring.identityadmin.workflow.translator;

import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.domain.entity.policy.PolicyCondition;
import io.spring.identityadmin.domain.entity.policy.PolicyRule;
import io.spring.identityadmin.domain.entity.policy.PolicyTarget;
import io.spring.identityadmin.repository.PermissionRepository;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BusinessPolicyTranslatorImpl implements BusinessPolicyTranslator {

    private final PermissionRepository permissionRepository;

    @Override
    public Policy translate(WizardContext context) {
        Policy policy = Policy.builder()
                .name(context.policyName())
                .description(context.policyDescription())
                .effect(Policy.Effect.ALLOW) // 마법사는 허용 정책만 생성
                .priority(500) // 중간 우선순위
                .build();

        // 1. 주체(Who) SpEL 조건 생성
        String subjectExpression = context.subjectIds().stream()
                .map(id -> "hasAuthority('USER_" + id + "')") // 예시: 주체 타입을 구분해야 함
                .collect(Collectors.joining(" or "));

        // 2. 권한(What) SpEL 조건 생성
        String permissionExpression = context.permissionIds().stream()
                .map(id -> permissionRepository.findById(id).orElseThrow().getName())
                .map(name -> "hasAuthority('" + name + "')")
                .collect(Collectors.joining(" and "));

        List<String> conditions = new ArrayList<>();
        if (!subjectExpression.isEmpty()) conditions.add("(" + subjectExpression + ")");
        if (!permissionExpression.isEmpty()) conditions.add("(" + permissionExpression + ")");

        PolicyRule rule = PolicyRule.builder()
                .policy(policy)
                .description("Wizard-generated rule")
                .conditions(conditions.stream()
                        .map(expr -> PolicyCondition.builder().expression(expr).build())
                        .collect(Collectors.toSet()))
                .build();

        // Rule의 Condition 들에 Rule 참조 설정
        rule.getConditions().forEach(c -> c.setRule(rule));

        // 3. 대상(Target) 설정 - 모든 권한의 Target Type을 가져와 설정해야 함
        // 여기서는 예시로 모든 URL을 대상으로 설정
        PolicyTarget target = PolicyTarget.builder()
                .policy(policy)
                .targetType("URL")
                .targetIdentifier("/**")
                .build();

        policy.setRules(Set.of(rule));
        policy.setTargets(Set.of(target));

        return policy;
    }
}
