package io.spring.identityadmin.security.authorization.manager;

import io.spring.identityadmin.entity.policy.Policy;
import io.spring.identityadmin.entity.policy.PolicyTarget;
import io.spring.identityadmin.security.authorization.resolver.ExpressionAuthorizationManagerResolver;
import io.spring.identityadmin.security.authorization.service.PolicyRetrievalPoint;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcherEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component("customDynamicAuthorizationManager")
@RequiredArgsConstructor
public class CustomDynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final PolicyRetrievalPoint policyRetrievalPoint;
    private final ExpressionAuthorizationManagerResolver managerResolver;
    private List<RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>>> mappings;

    @PostConstruct
    public void initialize() {
        log.info("Initializing dynamic authorization mappings from Policy model...");
        this.mappings = new ArrayList<>();

        List<Policy> urlPolicies = policyRetrievalPoint.findUrlPolicies();

        for (Policy policy : urlPolicies) {
            String finalExpression = buildExpressionFromPolicy(policy);
            for (PolicyTarget target : policy.getTargets()) {
                // Policy의 targetType이 'URL'인 경우에만 매핑 추가 (향후 확장 대비)
                if ("URL".equals(target.getTargetType())) {
                    RequestMatcher matcher = PathPatternRequestMatcher.withDefaults().matcher(target.getTargetIdentifier());
                    AuthorizationManager<RequestAuthorizationContext> manager = managerResolver.resolve(finalExpression);
                    this.mappings.add(new RequestMatcherEntry<>(matcher, manager));
                    log.debug("Policy mapping loaded - URL '{}' mapped to expression '{}'", target.getTargetIdentifier(), finalExpression);
                }
            }
        }
        log.info("Initialization complete. {} URL policy mappings configured.", this.mappings.size());
    }

    private String buildExpressionFromPolicy(Policy policy) {
        String conditionExpression = policy.getRules().stream()
                .flatMap(rule -> rule.getConditions().stream())
                .map(condition -> "(" + condition.getExpression() + ")")
                .collect(Collectors.joining(" and "));

        if (conditionExpression.isEmpty()) {
            // 조건이 없는 경우, 정책의 Effect에 따라 모든 접근을 허용 또는 거부
            return (policy.getEffect() == Policy.Effect.ALLOW) ? "permitAll" : "denyAll";
        }

        if (policy.getEffect() == Policy.Effect.DENY) {
            return "!(" + conditionExpression + ")";
        }
        return conditionExpression;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        for (RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>> mapping : this.mappings) {
            if (mapping.getRequestMatcher().matcher(context.getRequest()).isMatch()) {
                return mapping.getEntry().check(authentication, context);
            }
        }
        // 매칭되는 규칙이 없을 경우 기본적으로 접근 거부
        log.trace("No matching policy found for request: {}. Denying access by default.", context.getRequest().getRequestURI());
        return new AuthorizationDecision(false);
    }

    public synchronized void reload() {
        log.info("Reloading dynamic authorization mappings from data source...");
        policyRetrievalPoint.clearUrlPoliciesCache();
        initialize();
        log.info("Dynamic authorization mappings reloaded successfully.");
    }
}