package io.spring.identityadmin.security.xacml.pdp.evaluation.method;

import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.repository.PermissionRepository;
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
@Slf4j
@RequiredArgsConstructor
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final PermissionRepository permissionRepository;
    private final SpelExpressionParser expressionParser = new SpelExpressionParser();

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || targetDomainObject == null || !(permission instanceof String)) {
            return false;
        }
        String targetType = targetDomainObject.getClass().getSimpleName().toUpperCase();
        return evaluate(authentication, targetDomainObject, targetType, (String) permission);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        // 이 메서드는 targetDomainObject가 없으므로 SpEL에서 #target을 사용할 수 없습니다.
        // 필요 시 targetId와 targetType으로 DB에서 객체를 조회한 후 위 메서드를 호출하도록 구현할 수 있습니다.
        // 현재 설계에서는 이 메서드보다 targetDomainObject를 사용하는 PostAuthorize에 집중합니다.
        log.warn("hasPermission based on targetId and targetType is not fully supported for dynamic SpEL evaluation.");
        return false;
    }

    /**
     * [최종 진화 로직]
     * targetType과 actionType에 맞는 Permission을 DB에서 찾아,
     * 그 안에 저장된 SpEL 표현식을 동적으로 실행합니다.
     */
    private boolean evaluate(Authentication authentication, Object targetObject, String targetType, String action) {

        String permissionName = String.format("%s_%s", targetType, action.toUpperCase());
        Optional<Permission> permissionOpt = permissionRepository.findByName(permissionName);

        if (permissionOpt.isEmpty()) {
            log.trace("No specific permission found for {} on {}. Access denied.", action, targetType);
            return false; // 해당 권한 정의가 없으면 거부
        }

        Permission permission = permissionOpt.get();
        String condition = permission.getConditionExpression();

        // 2. Permission에 연결된 SpEL 조건이 없으면, 이 권한을 가졌다는 사실만으로 통과시킵니다.
        if (!StringUtils.hasText(condition)) {
            log.debug("Permission '{}' has no extra condition. Access granted by permission possession.", permissionName);
            return true;
        }

        // 3. SpEL 표현식을 평가하여 최종 결정을 내립니다.
        try {
            EvaluationContext context = new StandardEvaluationContext();
            // SpEL 표현식에서 사용할 수 있는 변수들을 설정합니다.
            context.setVariable("target", targetObject);
            context.setVariable("auth", authentication);
            context.setVariable("user", authentication.getPrincipal());

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