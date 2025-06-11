package io.spring.identityadmin.security.xacml.pip.context;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DefaultContextHandler implements ContextHandler {
    @Override
    public AuthorizationContext create(Authentication authentication, HttpServletRequest request) {
        ResourceDetails resource = new ResourceDetails("URL", request.getRequestURI());
        EnvironmentDetails environment = new EnvironmentDetails(request.getRemoteAddr(), LocalDateTime.now(), request);

        return new AuthorizationContext(authentication, resource, request.getMethod(), environment);
    }
}
