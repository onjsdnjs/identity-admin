package io.spring.identityadmin.workflow.translator;

import io.spring.identityadmin.domain.entity.FunctionCatalog;
import io.spring.identityadmin.domain.entity.Permission;
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

        List<String> conditions = new ArrayList<>();

        // [오류 수정] 존재하지 않는 subjectIds() 대신, 올바른 subjects() 메서드를 사용하여 SpEL 조건을 생성합니다.
        String subjectExpression = context.subjects().stream()
                .map(subject -> String.format("hasAuthority('%s_%d')", subject.type(), subject.id()))
                .collect(Collectors.joining(" or "));

        if (!subjectExpression.isEmpty()) {
            conditions.add("(" + subjectExpression + ")");
        }

        // 권한(What) SpEL 조건 생성
        List<Permission> permissions = permissionRepository.findAllById(context.permissionIds());
        String permissionExpression = permissions.stream()
                .map(Permission::getName)
                .map(name -> String.format("hasAuthority('%s')", name))
                .collect(Collectors.joining(" and "));

        if (!permissionExpression.isEmpty()) {
            conditions.add("(" + permissionExpression + ")");
        }

        // 규칙(Rule) 엔티티 생성
        PolicyRule rule = PolicyRule.builder()
                .policy(policy)
                .description("Wizard-generated rule for: " + context.policyDescription())
                .build();

        Set<PolicyCondition> policyConditions = conditions.stream()
                .map(expr -> PolicyCondition.builder().expression(expr).rule(rule).build())
                .collect(Collectors.toSet());
        rule.setConditions(policyConditions);

        // 대상(Target) 엔티티 생성
        Set<PolicyTarget> targets = permissions.stream()
                .flatMap(p -> p.getFunctions().stream())
                .map(FunctionCatalog::getManagedResource)
                .map(mr -> PolicyTarget.builder()
                        .policy(policy)
                        .targetType(mr.getResourceType().name())
                        .httpMethod(mr.getHttpMethod() != null ? mr.getHttpMethod().name() : null)
                        .targetIdentifier(mr.getResourceIdentifier())
                        .build())
                .collect(Collectors.toSet());

        policy.setRules(Set.of(rule));
        policy.setTargets(targets);

        return policy;
    }
}