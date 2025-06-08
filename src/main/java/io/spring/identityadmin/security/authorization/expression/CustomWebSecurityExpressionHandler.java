package io.spring.identityadmin.security.authorization.expression;

import io.spring.identityadmin.security.authorization.context.AuthorizationContext;
import io.spring.identityadmin.security.authorization.context.ContextHandler;
import io.spring.identityadmin.security.authorization.pip.AttributeInformationPoint;
import io.spring.identityadmin.security.authorization.risk.RiskEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.expression.SecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.DefaultHttpSecurityExpressionHandler;
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
     * <<< 핵심 수정: 스프링 시큐리티 6.1+의 올바른 확장 포인트인 createSecurityExpressionRoot를 오버라이드 >>>
     */
    @Override
    protected SecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication, RequestAuthorizationContext context) {
        // createSecurityExpressionRoot(Supplier, RequestContext)를 호출하여 일관성 유지
        return createSecurityExpressionRoot(() -> authentication, context);
    }

    /**
     * Supplier<Authentication>을 받는 오버로딩된 메서드를 만들어 로직을 중앙에서 관리.
     */
    private CustomWebSecurityExpressionRoot createSecurityExpressionRoot(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        // 1. ContextHandler를 통해 표준 AuthorizationContext 생성
        AuthorizationContext authorizationContext = contextHandler.create(authentication.get(), context.getRequest());

        // 2. 커스텀 Root 객체 생성 시, 표준 컨텍스트와 PIP 들을 함께 전달
        CustomWebSecurityExpressionRoot root = new CustomWebSecurityExpressionRoot(authentication.get(), new FilterInvocation("",null), riskEngine, attributePIP, authorizationContext);

        // 3. 부모 클래스가 하던 것처럼 필수 컴포넌트 설정
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setTrustResolver(new AuthenticationTrustResolverImpl());
        root.setRoleHierarchy(getRoleHierarchy());
        root.setDefaultRolePrefix("ROLE_");

        return root;
    }
}