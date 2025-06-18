package io.spring.identityadmin.security.core;

import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.security.xacml.pep.CustomDynamicAuthorizationManager;
import io.spring.identityadmin.security.xacml.prp.PolicyRetrievalPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.ExpressionUtils;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component // [신규] 다른 곳에서 주입받을 수 있도록 Bean으로 등록
@RequiredArgsConstructor
public class ProtectableMethodAuthorizationManager {

    private final MethodSecurityExpressionHandler expressionHandler;
    private final PolicyRetrievalPoint policyRetrievalPoint;
    private final CustomDynamicAuthorizationManager dynamicAuthorizationManager;

    public void preAuthorize(Supplier<Authentication> authentication, MethodInvocation mi) {
        String finalExpression = getDynamicExpression(mi, "PRE_AUTHORIZE");
        if (finalExpression == null) return;

        EvaluationContext ctx = expressionHandler.createEvaluationContext(authentication, mi);
        if (!evaluate(finalExpression, ctx)) {
            log.warn("Access is denied for method [{}]. Pre-authorization expression evaluated to false: {}", mi.getMethod().getName(), finalExpression);
            throw new AccessDeniedException("Access is denied");
        }
    }

    public void postAuthorize(Supplier<Authentication> authentication, MethodInvocation mi, Object returnObject) {
        String finalExpression = getDynamicExpression(mi, "POST_AUTHORIZE");
        if (finalExpression == null) return;

        EvaluationContext ctx = expressionHandler.createEvaluationContext(authentication, mi);
        expressionHandler.setReturnObject(returnObject, ctx);

        if (!evaluate(finalExpression, ctx)) {
            log.warn("Access is denied for method [{}]. Post-authorization expression evaluated to false: {}", mi.getMethod().getName(), finalExpression);
            throw new AccessDeniedException("Access is denied");
        }
    }

    private boolean evaluate(String expressionString, EvaluationContext context) {
        try {
            Expression expression = expressionHandler.getExpressionParser().parseExpression(expressionString);
            return ExpressionUtils.evaluateAsBoolean(expression, context);
        } catch (Exception e) {
            log.error("Error evaluating SpEL expression: {}", expressionString, e);
            return false; // 평가 오류 시 안전하게 접근 거부
        }
    }

    private String getDynamicExpression(MethodInvocation mi, String phase) {
        Method method = mi.getMethod();
        String methodIdentifier = method.getDeclaringClass().getName() + "." + method.getName();

        List<Policy> policies = policyRetrievalPoint.findMethodPolicies(methodIdentifier, phase);

        if (CollectionUtils.isEmpty(policies)) return null;

        return dynamicAuthorizationManager.getExpressionFromPolicies(policies);
    }
}