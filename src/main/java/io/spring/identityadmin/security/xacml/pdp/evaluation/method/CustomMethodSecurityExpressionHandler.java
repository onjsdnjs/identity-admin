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
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
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

    /**
     * [최종 수정] Spring Security의 표준 확장 포인트를 오버라이드하여
     * 올바르게 상속 구조를 갖춘 CustomMethodSecurityExpressionRoot를 생성합니다.
     */
    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication, MethodInvocation invocation) {
        // 1. 컨텍스트 핸들러를 통해 표준 AuthorizationContext 생성
        String methodIdentifier = invocation.getMethod().getDeclaringClass().getName() + "." + invocation.getMethod().getName();
        AuthorizationContext authorizationContext = contextHandler.create(authentication, invocation);

        // 2. 올바르게 구현된 CustomMethodSecurityExpressionRoot 인스턴스화
        CustomMethodSecurityExpressionRoot root = new CustomMethodSecurityExpressionRoot(authentication, riskEngine, attributePIP, authorizationContext);

        // 3. 필수 컴포넌트 설정
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setTrustResolver(getTrustResolver());
        root.setRoleHierarchy(getRoleHierarchy());
        root.setDefaultRolePrefix(getDefaultRolePrefix());
        root.setThis(invocation.getThis()); // #this 객체 설정

        // 감사 로그 기록
        auditLogService.logDecision(authentication.getName(), methodIdentifier, "METHOD_INVOCATION", "EVALUATING", "Method authorization evaluation started.", null);

        return root;
    }

    @Override
    public EvaluationContext createEvaluationContext(Supplier<Authentication> authentication, MethodInvocation mi) {
        // 1. 부모 클래스의 메서드를 호출하여 기본적인 EvaluationContext(#root 객체 포함)를 생성
        EvaluationContext ctx = super.createEvaluationContext(authentication, mi);

        // 2. 현재 호출된 메서드의 식별자 생성
        Method method = mi.getMethod();
        String methodIdentifier = method.getDeclaringClass().getName() + "." + method.getName();

        // 3. PRP를 통해 DB 에서 해당 메서드에 대한 정책 조회
        List<Policy> policies = policyRetrievalPoint.findMethodPolicies(methodIdentifier);

        // 4. 조회된 정책을 기반으로 최종 SpEL 표현식 생성
        String finalExpression = "false"; // 기본값은 거부
        if (!CollectionUtils.isEmpty(policies)) {
            finalExpression = buildExpressionFromPolicies(policies);
        } else {
            log.trace("No dynamic method policy for [{}]. Denying by default.", methodIdentifier);
        }

        log.debug("Dynamic SpEL for method [{}] is: {}", methodIdentifier, finalExpression);

        // 5. 최종 표현식을 파싱하여 컨텍스트 변수 #dynamicRule 에 할당
        Expression dynamicRuleExpression = getExpressionParser().parseExpression(finalExpression);
        ctx.setVariable("dynamicRule", dynamicRuleExpression);

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