package io.spring.identityadmin.security.xacml.pdp.evaluation.method;
import io.spring.identityadmin.security.xacml.pip.attribute.AttributeInformationPoint;
import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;
import io.spring.identityadmin.security.xacml.pip.risk.RiskEngine;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.Map;

/**
 * [최종 수정] Spring Security의 표준 `SecurityExpressionRoot`를 상속하고,
 * `MethodSecurityExpressionOperations`를 구현하여 메서드 보안 SpEL의 커스텀 루트를 올바르게 정의합니다.
 */
public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private Object filterObject;
    private Object returnObject;
    private Object target;

    private final RiskEngine riskEngine;
    private final AttributeInformationPoint attributePIP;
    private final AuthorizationContext authorizationContext;

    public CustomMethodSecurityExpressionRoot(Authentication authentication,
                                              RiskEngine riskEngine, AttributeInformationPoint attributePIP,
                                              AuthorizationContext authorizationContext) {
        super(authentication);
        this.riskEngine = riskEngine;
        this.attributePIP = attributePIP;
        this.authorizationContext = authorizationContext;
    }

    /**
     * SpEL 표현식에서 #riskScore로 접근 가능한 커스텀 메서드.
     */
    public int getRiskScore() {
        // [최종 수정] 자신이 가진 표준 컨텍스트 객체를 그대로 전달합니다.
        return riskEngine.calculateRiskScore(this.authorizationContext);
    }

    /**
     * SpEL 표현식에서 #getAttribute('key') 형태로 동적 속성을 조회하는 커스텀 메서드.
     */
    public Object getAttribute(String key) {
        if (authorizationContext.attributes().containsKey(key)) {
            return authorizationContext.attributes().get(key);
        }
        Map<String, Object> fetchedAttributes = attributePIP.getAttributes(authorizationContext);
        authorizationContext.attributes().putAll(fetchedAttributes);
        return authorizationContext.attributes().get(key);
    }

    // --- MethodSecurityExpressionOperations 인터페이스 구현 ---
    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return this.filterObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return this.returnObject;
    }

    void setThis(Object target) {
        this.target = target;
    }

    @Override
    public Object getThis() {
        return this.target;
    }
}