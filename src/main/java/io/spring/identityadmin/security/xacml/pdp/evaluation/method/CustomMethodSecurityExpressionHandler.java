package io.spring.identityadmin.security.xacml.pdp.evaluation.method;

import io.spring.identityadmin.admin.monitoring.service.AuditLogService;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.security.xacml.pip.attribute.AttributeInformationPoint;
import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;
import io.spring.identityadmin.security.xacml.pip.context.ContextHandler;
import io.spring.identityadmin.security.xacml.pip.risk.RiskEngine;
import io.spring.identityadmin.security.xacml.prp.PolicyRetrievalPoint;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class CustomMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    private final PolicyRetrievalPoint policyRetrievalPoint;
    private final ContextHandler contextHandler;
    private final RiskEngine riskEngine;
    private final AttributeInformationPoint attributePIP;
    private final AuditLogService auditLogService;

    public CustomMethodSecurityExpressionHandler(
            CustomPermissionEvaluator customPermissionEvaluator,
            RoleHierarchy roleHierarchy,
            PolicyRetrievalPoint policyRetrievalPoint,
            ContextHandler contextHandler,
            RiskEngine riskEngine,
            AttributeInformationPoint attributePIP,
            AuditLogService auditLogService) {
        Assert.notNull(policyRetrievalPoint, "PolicyRetrievalPoint cannot be null");
        this.policyRetrievalPoint = policyRetrievalPoint;
        this.contextHandler = contextHandler;
        this.riskEngine = riskEngine;
        this.attributePIP = attributePIP;
        this.auditLogService = auditLogService;
        super.setPermissionEvaluator(customPermissionEvaluator);
        super.setRoleHierarchy(roleHierarchy);
        log.info("CustomMethodSecurityExpressionHandler initialized with DYNAMIC lookup and full AuthorizationContext.");
    }

    @Override
    public EvaluationContext createEvaluationContext(Supplier<Authentication> authentication, MethodInvocation mi) {

        // 1. 커스텀 Expression Root 객체 생성 및 설정
        Authentication auth = authentication.get();
        AuthorizationContext authorizationContext = contextHandler.create(auth, mi);
        CustomMethodSecurityExpressionRoot root = new CustomMethodSecurityExpressionRoot(auth, riskEngine, attributePIP, authorizationContext);

        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setTrustResolver(getTrustResolver());
        root.setRoleHierarchy(getRoleHierarchy());
        root.setDefaultRolePrefix(getDefaultRolePrefix());
        root.setThis(mi.getThis());

        // 2. ExpressionRoot를 기반으로 최종 EvaluationContext 생성
//        StandardEvaluationContext ctx = new StandardEvaluationContext(root);
        MethodBasedEvaluationContext ctx = new MethodBasedEvaluationContext(root, mi.getMethod(), mi.getArguments(), getParameterNameDiscoverer());
        ctx.setBeanResolver(getBeanResolver());

        // 3. PRP를 통해 동적 규칙(SpEL) 조회
        Method method = mi.getMethod();
        String methodIdentifier = method.getDeclaringClass().getName() + "." + method.getName();
        List<Policy> policies = policyRetrievalPoint.findMethodPolicies(methodIdentifier);

        // 4. 조회된 정책을 기반으로 최종 SpEL 표현식 생성 (기본값: denyAll)
        String finalExpression = "denyAll";
        if (!CollectionUtils.isEmpty(policies)) {
            finalExpression = buildExpressionFromPolicies(policies);
        } else {
            log.trace("No dynamic method policy for [{}]. Denying by default.", methodIdentifier);
        }

        // 5. 최종 표현식을 파싱하여 컨텍스트 변수 #dynamicRule 에 할당
        Expression dynamicRuleExpression = getExpressionParser().parseExpression(finalExpression);
        ctx.setVariable("dynamicRule", dynamicRuleExpression);

        log.debug("Dynamic SpEL for method [{}] is: {}", methodIdentifier, finalExpression);

        // 6. 감사 로그 기록
        auditLogService.logDecision(auth.getName(), methodIdentifier, "METHOD_INVOCATION", "EVALUATING", "Evaluating with dynamic rule: " + finalExpression, null);

        return ctx;
    }

    private String buildExpressionFromPolicies(List<Policy> policies) {
        // 가장 우선순위가 높은 정책 하나만 사용.
        Policy policy = policies.getFirst();

        String conditionExpression = policy.getRules().stream()
                .flatMap(rule -> rule.getConditions().stream())
                .map(condition -> "(" + condition.getExpression() + ")")
                .collect(Collectors.joining(" and "));

        if (conditionExpression.isEmpty()) {
            return (policy.getEffect() == Policy.Effect.ALLOW) ? "true" : "false";
        }
        if (policy.getEffect() == Policy.Effect.DENY) {
            return "!(" + conditionExpression + ")";
        }
        return conditionExpression;
    }
}