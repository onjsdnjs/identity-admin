package io.spring.identityadmin.security.xacml.pep;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.identityadmin.admin.monitoring.service.AuditLogService;
import io.spring.identityadmin.ai.dto.TrustAssessment;
import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.domain.entity.policy.PolicyCondition;
import io.spring.identityadmin.domain.entity.policy.PolicyTarget;
import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;
import io.spring.identityadmin.security.xacml.pip.context.ContextHandler;
import io.spring.identityadmin.security.xacml.prp.PolicyRetrievalPoint;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.ArrayList;


@Slf4j
@Component("customDynamicAuthorizationManager")
@RequiredArgsConstructor
public class CustomDynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final PolicyRetrievalPoint policyRetrievalPoint;
    private final ExpressionAuthorizationManagerResolver managerResolver;
    private List<RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>>> mappings;
    private static final Pattern AUTHORITY_PATTERN = Pattern.compile("^[A-Z_]+$");
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final ContextHandler contextHandler;

    @PostConstruct
    public void initialize() {
        log.info("Initializing dynamic authorization mappings from Policy model...");
        this.mappings = new ArrayList<>();

        List<Policy> urlPolicies = policyRetrievalPoint.findUrlPolicies();

        for (Policy policy : urlPolicies) {
            String expression = getExpressionFromPolicy(policy);

            for (PolicyTarget target : policy.getTargets()) {
                if ("URL".equals(target.getTargetType())) {
                    RequestMatcher matcher = PathPatternRequestMatcher.withDefaults().matcher(target.getTargetIdentifier());
                    AuthorizationManager<RequestAuthorizationContext> manager = managerResolver.resolve(expression);
                    this.mappings.add(new RequestMatcherEntry<>(matcher, manager));
                    log.debug("Policy mapping loaded - URL '{}' mapped to expression '{}' using {}", target.getTargetIdentifier(), expression, manager.getClass().getSimpleName());
                }
            }
        }
        log.info("Initialization complete. {} URL policy mappings configured.", this.mappings.size());
    }

    /**
     * 정책 객체로부터 최종 인가 표현식 문자열을 생성합니다.
     * 여러 조건은 OR로 결합되며, 순수 권한 문자열은 hasAnyAuthority()로 묶어 효율을 높입니다.
     */
    public String getExpressionFromPolicy(Policy policy) {
        List<String> conditionExpressions = policy.getRules().stream()
                .flatMap(rule -> rule.getConditions().stream())
                .map(PolicyCondition::getExpression)
                .toList();

        if (conditionExpressions.isEmpty()) {
            return (policy.getEffect() == Policy.Effect.ALLOW) ? "permitAll" : "denyAll";
        }

        String finalExpression;

        // 1. 조건이 단 하나일 경우
        if (conditionExpressions.size() == 1) {
            finalExpression = conditionExpressions.getFirst(); // 괄호 없이 순수 표현식(예: 'ROLE_ADMIN' 또는 'hasRole(''USER'')')을 그대로 사용

            // 2. 조건이 여러 개일 경우
        } else {
            boolean allAreSimpleAuthorities = conditionExpressions.stream().allMatch(expr -> AUTHORITY_PATTERN.matcher(expr).matches());

            // 2-1. 모든 조건이 순수 권한 문자열이면 hasAnyAuthority()로 효율적으로 묶음
            if (allAreSimpleAuthorities) {
                finalExpression = "hasAnyAuthority(" +
                        conditionExpressions.stream().map(auth -> "'" + auth + "'").collect(Collectors.joining(",")) +
                        ")";
                // 2-2. SpEL이 하나라도 섞여 있으면 or 로 결합
            } else {
                finalExpression = conditionExpressions.stream()
                        .map(expr -> "(" + expr + ")")
                        .collect(Collectors.joining(" or "));
            }
        }

        if (policy.getEffect() == Policy.Effect.DENY) {
            return "!(" + finalExpression + ")";
        }
        return finalExpression;
    }

    /**
     * 여러 정책을 받아 최종 SpEL 표현식으로 조합합니다.
     * 각 정책의 표현식은 OR로 결합됩니다.
     * 동적 메서드 인가(ProtectableMethodAuthorizationManager)에서 사용됩니다.
     */
    public String getExpressionFromPolicies(List<Policy> policies) {
        if (policies == null || policies.isEmpty()) {
            return "denyAll"; // 적용할 정책이 없으면 기본적으로 거부
        }

        return policies.stream()
                .map(this::getExpressionFromPolicy) // 기존 단일 정책 변환 로직 재사용
                .map(expr -> "(" + expr + ")")
                .collect(Collectors.joining(" or ")); // 여러 정책은 OR로 결합
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext context) {
        log.trace("Checking authorization for request: {}", context.getRequest().getRequestURI());

        final HttpServletRequest request = context.getRequest();
        final Authentication authentication = authenticationSupplier.get();

        AuthorizationContext authorizationContext = contextHandler.create(authentication, request);

        for (RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>> mapping : this.mappings) {
            if (mapping.getRequestMatcher().matcher(context.getRequest()).isMatch()) {
                log.debug("Request matched by '{}'. Delegating to its AuthorizationManager.", mapping.getRequestMatcher());

                AuthorizationManager<RequestAuthorizationContext> manager = mapping.getEntry();
                AuthorizationDecision decision = manager.check(authenticationSupplier, context);

                logAuthorizationAttempt(authentication, authorizationContext, decision);
                return decision;
            }
        }
        log.trace("No matching policy found for request. Denying access by default.");
        AuthorizationDecision authorizationDecision = new AuthorizationDecision(true);
        logAuthorizationAttempt(authentication, authorizationContext, authorizationDecision);

        return authorizationDecision;
    }

    /**
     * 인가 시도 및 그 결과를 상세히 감사 로그에 기록합니다.
     * XAI의 핵심인 AI 평가 근거를 포함합니다.
     */
    private void logAuthorizationAttempt(Authentication authentication, AuthorizationContext context, AuthorizationDecision decision) {

        String principal = (authentication != null && authentication.isAuthenticated()) ? ((UserDto)authentication.getPrincipal()).getName() : "anonymousUser";
        String resource = context.resource().identifier();
        String action = context.action();
        String result = decision.isGranted() ? "ALLOW" : "DENY";
        String clientIp = context.environment().remoteIp();

        String reason;
        TrustAssessment assessment = (TrustAssessment) context.attributes().get("ai_assessment");

        if (assessment != null) {
            try {
                reason = "AI 평가 결과: " + objectMapper.writeValueAsString(assessment);
            } catch (JsonProcessingException e) {
                reason = "AI 평가 결과 직렬화 실패. 점수: " + assessment.score();
            }
        } else {
            reason = "정적 규칙 매칭"; // AI 평가가 없었다면 일반 규칙 매칭
        }

        auditLogService.logDecision(principal, resource, action, result, reason, clientIp);
    }

    public synchronized void reload() {
        log.info("Reloading dynamic authorization mappings from data source...");
        policyRetrievalPoint.clearUrlPoliciesCache();
        initialize();
        log.info("Dynamic authorization mappings reloaded successfully.");
    }
}