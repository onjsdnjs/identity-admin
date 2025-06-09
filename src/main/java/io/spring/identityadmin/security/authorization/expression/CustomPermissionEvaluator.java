package io.spring.identityadmin.security.authorization.expression;

import io.spring.identityadmin.admin.repository.PermissionRepository;
import io.spring.identityadmin.admin.service.DocumentService;
import io.spring.identityadmin.entity.Permission;
import io.spring.identityadmin.security.authorization.auth.PermissionAuthority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Optional;

@Component("customPermissionEvaluator")
@RequiredArgsConstructor
@Slf4j
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final PermissionRepository permissionRepository;
     private final DocumentService documentService;
    private final SpelExpressionParser expressionParser = new SpelExpressionParser();

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || !(permission instanceof String)) {
            return false;
        }

        String requiredPermissionAction = ((String) permission).toUpperCase();
        String targetType = (targetDomainObject != null) ? targetDomainObject.getClass().getSimpleName().toUpperCase() : null;

        return authentication.getAuthorities().stream()
                .filter(auth -> auth instanceof PermissionAuthority)
                .map(auth -> (PermissionAuthority) auth)
                .filter(pa -> pa.getActionType().equalsIgnoreCase(requiredPermissionAction) &&
                        (targetType == null || pa.getTargetType().equalsIgnoreCase(targetType)))
                .anyMatch(pa -> {
                    log.debug("User {} has base permission '{}'. Evaluating condition...", authentication.getName(), pa.getPermissionName());
                    return evaluateCondition(pa.getPermissionName(), authentication, targetDomainObject);
                });
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || !(permission instanceof String)) {
            return false;
        }

        String requiredAction = ((String) permission).toUpperCase();
        String targetDomainType = targetType.toUpperCase();

        return authentication.getAuthorities().stream()
                .filter(auth -> auth instanceof PermissionAuthority)
                .map(auth -> (PermissionAuthority) auth)
                .filter(pa -> pa.getTargetType().equalsIgnoreCase(targetDomainType) && pa.getActionType().equalsIgnoreCase(requiredAction))
                .anyMatch(pa -> {
                    log.debug("User {} has base permission '{}' for targetId {}. Evaluating condition...",
                            authentication.getName(), pa.getPermissionName(), targetId);
                    return evaluateCondition(pa.getPermissionName(), authentication, targetId);
                });
    }

    private boolean evaluateCondition(String permissionName, Authentication authentication, Object targetObject) {
        Optional<Permission> permissionOpt = permissionRepository.findByName(permissionName);
        if (permissionOpt.isEmpty()) {
            log.warn("Permission '{}' not found in database for condition evaluation.", permissionName);
            return false;
        }

        String condition = permissionOpt.get().getConditionExpression();
        if (!StringUtils.hasText(condition)) {
            return true; // 조건이 없으면 항상 통과
        }

        // [핵심 수정] SpEL 표현식이 #target을 사용하는데 실제 targetObject가 null 이면,
        // SpelEvaluationException을 발생시키는 대신 즉시 false를 반환하여 오류를 방지합니다.
        if (targetObject == null && condition.contains("#target")) {
            log.warn("Condition evaluation for permission '{}' requires a non-null target, but target was null. Denying access.", permissionName);
            return false;
        }

        try {
            EvaluationContext context = new StandardEvaluationContext();
            context.setVariable("auth", authentication);
            context.setVariable("user", authentication.getPrincipal());
            context.setVariable("target", targetObject);

            Expression expression = expressionParser.parseExpression(condition);
            Boolean result = expression.getValue(context, Boolean.class);
            log.debug("Evaluated condition '{}' for permission '{}': Result is {}", condition, permissionName, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error evaluating SpEL condition '{}' for permission '{}'", condition, permissionName, e);
            return false; // 평가 중 오류 발생 시 안전하게 거부
        }
    }
}