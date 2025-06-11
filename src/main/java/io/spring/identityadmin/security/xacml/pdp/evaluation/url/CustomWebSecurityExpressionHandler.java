package io.spring.identityadmin.security.xacml.pdp.evaluation.url;

import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;
import io.spring.identityadmin.security.xacml.pip.context.ContextHandler;
import io.spring.identityadmin.security.xacml.pip.attribute.AttributeInformationPoint;
import io.spring.identityadmin.security.xacml.pip.risk.RiskEngine;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.SecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.DefaultHttpSecurityExpressionHandler;
import org.springframework.security.web.access.expression.WebSecurityExpressionRoot;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * URL 기반 인가를 위한 커스텀 Expression Handler.
 * DefaultHttpSecurityExpressionHandler를 올바르게 상속받아 SpEL 컨텍스트를 확장한다.
 */
@Component("customWebSecurityExpressionHandler")
@RequiredArgsConstructor
public class CustomWebSecurityExpressionHandler extends DefaultHttpSecurityExpressionHandler {

    private final RiskEngine riskEngine;
    private final ContextHandler contextHandler;
    private final AttributeInformationPoint attributePIP;

    /**
     * WebExpressionAuthorizationManager에 의해 호출되는 실제 진입점입니다.
     * 이 메서드를 오버라이드하여 커스텀 EvaluationContext를 생성합니다.
     */
    @Override
    public EvaluationContext createEvaluationContext(Supplier<Authentication> authentication, RequestAuthorizationContext requestContext) {

        // 1. CustomWebSecurityExpressionRoot를 생성하기 위해 FilterInvocation 객체를 만듭니다.
        Authentication auth = authentication.get();
        HttpServletRequest request = requestContext.getRequest();

        // 2. ContextHandler를 통해 표준 AuthorizationContext를 생성합니다.
        AuthorizationContext authorizationContext = contextHandler.create(auth, request);

        // 3. 우리의 커스텀 기능을 포함하는 CustomWebSecurityExpressionRoot를 인스턴스화합니다.
        CustomWebSecurityExpressionRoot root = new CustomWebSecurityExpressionRoot(auth, request, riskEngine, attributePIP, authorizationContext);

        // 4. ExpressionRoot에 표준 헬퍼 컴포넌트들을 설정합니다.
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setTrustResolver(new AuthenticationTrustResolverImpl());
        root.setRoleHierarchy(getRoleHierarchy());
        root.setDefaultRolePrefix("ROLE_");

        // 5. 생성된 커스텀 root 객체를 기반으로 StandardEvaluationContext를 생성합니다.
        StandardEvaluationContext ctx = new StandardEvaluationContext(root);
        ctx.setBeanResolver(getBeanResolver());

        // 6. RequestAuthorizationContext에 추가 변수가 있다면(미래 확장성) 컨텍스트에 복사합니다.
        requestContext.getVariables().forEach(ctx::setVariable);

        return ctx;
    }
}