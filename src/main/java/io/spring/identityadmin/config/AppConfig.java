package io.spring.identityadmin.config;

import io.spring.identityadmin.security.core.AuthorizationManagerMethodInterceptor;
import io.spring.identityadmin.security.core.ProtectableMethodAuthorizationManager;
import io.spring.identityadmin.resource.Protectable;
import org.modelmapper.ModelMapper;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    /**
     * ModelMapper bean
     */
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public AuthorizationManagerMethodInterceptor protectableAuthorizationAdvisor(
            ProtectableMethodAuthorizationManager protectableMethodAuthorizationManager) {

        Pointcut pointcut = new ComposablePointcut(classOrMethod());
        return new AuthorizationManagerMethodInterceptor(pointcut, protectableMethodAuthorizationManager);
    }

    private static Pointcut classOrMethod() {
        return Pointcuts.union(new AnnotationMatchingPointcut(null, Protectable.class, true),
                new AnnotationMatchingPointcut(Protectable.class, true));
    }

}
