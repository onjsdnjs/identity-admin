package io.spring.identityadmin.security.xacml.pdp.evaluation.method;

import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.Serializable;

@Component("customPermissionEvaluator")
@Slf4j
@RequiredArgsConstructor
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final ApplicationContext context; // Spring 컨텍스트 직접 주입
    private final PermissionRepository permissionRepository;
    private final SpelExpressionParser expressionParser = new SpelExpressionParser();

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permissionAction) {
        String action = ((String) permissionAction).toUpperCase();
        String permissionName = String.format("%s_%s", targetType.toUpperCase(), action);

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new AccessDeniedException("Permission not defined: " + permissionName));

        String spel = permission.getConditionExpression();
        if (!StringUtils.hasText(spel)) return true; // 조건이 없으면 통과

        Object targetObject = findDomainObject(targetType, targetId);
        if (targetObject == null) return false;

        EvaluationContext evalContext = new StandardEvaluationContext();
        evalContext.setVariable("target", targetObject);
        evalContext.setVariable("auth", authentication);
        evalContext.setVariable("user", authentication.getPrincipal());

        return expressionParser.parseExpression(spel).getValue(evalContext, Boolean.class);
    }

    private Object findDomainObject(String targetType, Serializable targetId) {
        String repositoryName = targetType.toLowerCase() + "Repository";
        JpaRepository<?, Serializable> repository = (JpaRepository<?, Serializable>) context.getBean(repositoryName);
        return repository.findById(targetId).orElse(null);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (targetDomainObject == null) return false;
        Serializable id = ObjectIdExtractor.extractId(targetDomainObject);
        String type = targetDomainObject.getClass().getSimpleName();
        return hasPermission(authentication, id, type, permission);
    }
}