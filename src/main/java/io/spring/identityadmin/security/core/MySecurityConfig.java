package io.spring.identityadmin.security.core;

import io.spring.identityadmin.admin.monitoring.service.AuditLogService;
import io.spring.identityadmin.security.xacml.pdp.evaluation.method.CustomMethodSecurityExpressionHandler;
import io.spring.identityadmin.security.xacml.pdp.evaluation.method.CustomPermissionEvaluator;
import io.spring.identityadmin.security.xacml.pep.CustomDynamicAuthorizationManager;
import io.spring.identityadmin.security.xacml.pip.attribute.AttributeInformationPoint;
import io.spring.identityadmin.security.xacml.pip.context.ContextHandler;
import io.spring.identityadmin.security.xacml.pip.risk.RiskEngine;
import io.spring.identityadmin.security.xacml.prp.PolicyRetrievalPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class MySecurityConfig {
    private final CustomDynamicAuthorizationManager customDynamicAuthorizationManager;
    private final CustomAuthenticationProvider customAuthenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .anyRequest().access(customDynamicAuthorizationManager));
        http.formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/admin"));
        http.authenticationProvider(customAuthenticationProvider);
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            CustomPermissionEvaluator customPermissionEvaluator,
            RoleHierarchy roleHierarchy,
            PolicyRetrievalPoint policyRetrievalPoint,
            ContextHandler contextHandler,
            RiskEngine riskEngine,
            AttributeInformationPoint attributePIP,
            AuditLogService auditLogService) {
        return new CustomMethodSecurityExpressionHandler(
                customPermissionEvaluator, roleHierarchy, policyRetrievalPoint,
                contextHandler, riskEngine, attributePIP, auditLogService
        );
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




    // RoleHierarchy 빈 등록 (계층적 역할 지원)
    @Bean
    public RoleHierarchyImpl roleHierarchy() {
        return new RoleHierarchyImpl();
    }



}
