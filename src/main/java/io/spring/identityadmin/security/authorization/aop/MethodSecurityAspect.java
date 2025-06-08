package io.spring.identityadmin.security.authorization.aop;

import io.springsecurity.springsecurity6x.security.authorization.expression.CustomWebSecurityExpressionHandler;
import io.springsecurity.springsecurity6x.security.authorization.service.MethodResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class MethodSecurityAspect {

    private final MethodResourceService methodResourceService;
    private final CustomWebSecurityExpressionHandler expressionHandler;
    private final SpelExpressionParser expressionParser = new SpelExpressionParser();

    @Around("execution(public * io.springsecurity.springsecurity6x.admin.service..*.*(..))")
    public Object checkMethodAccess(ProceedingJoinPoint joinPoint) throws Throwable {

        String fullMethodName = getFullMethodName(joinPoint);
        String expression = methodResourceService.getMethodExpression(fullMethodName);

        if (!StringUtils.hasText(expression)) {
            return joinPoint.proceed();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // URL 보안과 100% 동일한 평가 컨텍스트를 생성 ('뿌리' 공유)
        StandardEvaluationContext context = (StandardEvaluationContext) expressionHandler.createEvaluationContext(authentication, null);

        String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        boolean isGranted = Boolean.TRUE.equals(expressionParser.parseExpression(expression).getValue(context, Boolean.class));

        if (isGranted) {
            log.debug("Method access GRANTED for '{}'", fullMethodName);
            return joinPoint.proceed();
        } else {
            log.warn("Method access DENIED for '{}'", fullMethodName);
            throw new AccessDeniedException("Access Denied for method: " + fullMethodName);
        }
    }

    private String getFullMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringTypeName() + "." + signature.getName();
    }
}