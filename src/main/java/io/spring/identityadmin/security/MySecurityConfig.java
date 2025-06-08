package io.spring.identityadmin.security;

import io.spring.identityadmin.security.authorization.expression.CustomMethodSecurityExpressionHandler;
import io.spring.identityadmin.security.authorization.expression.CustomPermissionEvaluator;
import io.spring.identityadmin.security.authorization.service.MethodResourceService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class MySecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.formLogin(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(){
        return (web) -> web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * ModelMapper bean
     */
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            CustomPermissionEvaluator customPermissionEvaluator,
            RoleHierarchy roleHierarchy,
            MethodResourceService methodResourceService) {
        return new CustomMethodSecurityExpressionHandler(methodResourceService, customPermissionEvaluator, roleHierarchy);
    }

    // RoleHierarchy 빈 등록 (계층적 역할 지원)
    @Bean
    public RoleHierarchyImpl roleHierarchy() {
        return new RoleHierarchyImpl();
    }



}
