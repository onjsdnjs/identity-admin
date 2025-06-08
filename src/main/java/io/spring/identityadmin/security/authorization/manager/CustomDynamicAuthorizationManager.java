package io.spring.identityadmin.security.authorization.manager;

import io.spring.identityadmin.entity.policy.Policy;
import io.spring.identityadmin.entity.policy.PolicyTarget;
import io.spring.identityadmin.security.authorization.resolver.ExpressionAuthorizationManagerResolver;
import io.spring.identityadmin.security.authorization.service.DynamicAuthorizationService;
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
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component("customDynamicAuthorizationManager")
@RequiredArgsConstructor
public class CustomDynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final PolicyRetrievalPoint policyRetrievalPoint; // <<< DynamicAuthorizationService 대신 PRP 주입
    private final ExpressionAuthorizationManagerResolver managerResolver;
    private final DynamicAuthorizationService dynamicAuthorizationService;
    private List<RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>>> mappings;

    @PostConstruct
    public void initialize() {
        log.info("Initializing dynamic authorization mappings...");

        List<RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>>> newMappings = new ArrayList<>();

        // 1. Policy 기반 매핑 추가
        List<Policy> urlPolicies = policyRetrievalPoint.findUrlPolicies();
        for (Policy policy : urlPolicies) {
            String finalExpression = buildExpressionFromPolicy(policy);
            for (PolicyTarget target : policy.getTargets()) {
                RequestMatcher matcher = PathPatternRequestMatcher.withDefaults().matcher(target.getTargetIdentifier());
                AuthorizationManager<RequestAuthorizationContext> manager = managerResolver.resolve(finalExpression);
                newMappings.add(new RequestMatcherEntry<>(matcher, manager));
                log.debug("Policy mapping - URL '{}' to expression '{}'", target.getTargetIdentifier(), finalExpression);
            }
        }

        // 2. Resources 테이블 기반 매핑 추가 (DynamicAuthorizationService 활용)
        Map<String, String> urlRoleMappings = dynamicAuthorizationService.getUrlRoleMappings();
        for (Map.Entry<String, String> entry : urlRoleMappings.entrySet()) {
            String urlPattern = entry.getKey();
            String expression = entry.getValue();

            // Policy와 중복되지 않는 URL만 추가
            boolean isDuplicate = newMappings.stream()
                    .anyMatch(mapping -> mapping.getRequestMatcher().toString().contains(urlPattern));

            if (!isDuplicate) {
                RequestMatcher matcher = PathPatternRequestMatcher.withDefaults().matcher(urlPattern);
                AuthorizationManager<RequestAuthorizationContext> manager = managerResolver.resolve(expression);
                newMappings.add(new RequestMatcherEntry<>(matcher, manager));
                log.debug("Resources mapping - URL '{}' to expression '{}'", urlPattern, expression);
            }
        }

        this.mappings = newMappings;
        log.info("Initialization complete. {} total mappings configured ({} from Policy, {} from Resources).",
                mappings.size(), urlPolicies.size(), urlRoleMappings.size());
    }

    private String buildExpressionFromPolicy(Policy policy) {
        // 모든 Rule과 Condition을 'and'로 연결하여 하나의 SpEL 표현식으로 만듦
        // DENY 정책은 표현식 전체를 not() 으로 감쌀 수 있음
        StringBuilder expressionBuilder = new StringBuilder();

        policy.getRules().forEach(rule -> {
            rule.getConditions().forEach(condition -> {
                if (expressionBuilder.length() > 0) {
                    expressionBuilder.append(" and ");
                }
                expressionBuilder.append("(").append(condition.getExpression()).append(")");
            });
        });

        String finalExpression = expressionBuilder.toString();
        if (policy.getEffect() == Policy.Effect.DENY) {
            return "!" + finalExpression;
        }
        return finalExpression.isEmpty() ? "permitAll" : finalExpression;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        // 이 부분의 로직은 변경 없음 (Dispatcher 역할은 동일)
        for (RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>> mapping : this.mappings) {
            if (mapping.getRequestMatcher().matcher(context.getRequest()).isMatch()) {
                return mapping.getEntry().check(authentication, context);
            }
        }
        return new AuthorizationDecision(false);
    }

    public synchronized void reload() {
        log.info("Reloading dynamic authorization mappings from Policy model...");
        policyRetrievalPoint.clearUrlPoliciesCache(); // PRP의 캐시를 비웁니다.
        dynamicAuthorizationService.clearCache();
        initialize();
    }
}