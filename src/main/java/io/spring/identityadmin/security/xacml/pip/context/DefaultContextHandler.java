package io.spring.identityadmin.security.xacml.pip.context;

import jakarta.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInvocation; // MethodInvocation 임포트
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Component
public class DefaultContextHandler implements ContextHandler {
    @Override
    public AuthorizationContext create(Authentication authentication, HttpServletRequest request) {
        // 기존 URL 기반 컨텍스트 생성 로직은 그대로 유지
        ResourceDetails resource = new ResourceDetails("URL", request.getRequestURI());
        EnvironmentDetails environment = new EnvironmentDetails(request.getRemoteAddr(), LocalDateTime.now(), request);

        return new AuthorizationContext(authentication, resource, request.getMethod(), environment);
    }

    /**
     * [신규] 메서드 기반 보안을 위한 컨텍스트 생성 로직 구현
     */
    @Override
    public AuthorizationContext create(Authentication authentication, MethodInvocation invocation) {
        Method method = invocation.getMethod();
        String resourceIdentifier = method.getDeclaringClass().getName() + "." + method.getName();

        ResourceDetails resource = new ResourceDetails("METHOD", resourceIdentifier);

        // 메서드 호출 시점에는 HttpServletRequest가 없으므로 IP, 원본 요청 등은 null
        EnvironmentDetails environment = new EnvironmentDetails(null, LocalDateTime.now(), null);

        // 메서드 호출은 'INVOKE'라는 행동으로 정의
        return new AuthorizationContext(authentication, resource, "INVOKE", environment);
    }
}
