package io.spring.identityadmin.security.core;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.method.AuthorizationAdvisor;
import org.springframework.security.authorization.method.AuthorizationInterceptorsOrder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import java.util.function.Supplier;

@Slf4j
public class AuthorizationManagerMethodInterceptor implements MethodInterceptor, AuthorizationAdvisor {

    private final Pointcut pointcut;
    private final ProtectableMethodAuthorizationManager authorizationManager;
    private int order = AuthorizationInterceptorsOrder.FIRST.getOrder() + 1; // 다른 인터셉터보다 약간 뒤에 실행
    private final Supplier<SecurityContextHolderStrategy> securityContextHolderStrategy = SecurityContextHolder::getContextHolderStrategy;

    public AuthorizationManagerMethodInterceptor(Pointcut pointcut, ProtectableMethodAuthorizationManager authorizationManager) {
        this.pointcut = pointcut;
        this.authorizationManager = authorizationManager;
    }

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        authorizationManager.preAuthorize(this::getAuthentication, mi);
        Object returnObject = mi.proceed();
        authorizationManager.postAuthorize(this::getAuthentication, mi, returnObject);
        return returnObject;
    }

    private Authentication getAuthentication() {
        Authentication authentication = this.securityContextHolderStrategy.get().getContext().getAuthentication();
        if (authentication == null) {
            throw new AuthenticationCredentialsNotFoundException("An Authentication object was not found in the SecurityContext");
        }
        return authentication;
    }

    @Override
    public Pointcut getPointcut() { return this.pointcut; }
    @Override
    public Advice getAdvice() { return this; }
    @Override
    public boolean isPerInstance() { return true; }
    @Override
    public int getOrder() { return this.order; }
}