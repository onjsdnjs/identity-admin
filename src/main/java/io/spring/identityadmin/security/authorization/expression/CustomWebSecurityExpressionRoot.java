package io.spring.identityadmin.security.authorization.expression;

import io.spring.identityadmin.security.authorization.context.AuthorizationContext;
import io.spring.identityadmin.security.authorization.pip.AttributeInformationPoint;
import io.spring.identityadmin.security.authorization.risk.RiskEngine;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.WebSecurityExpressionRoot;

import java.util.Map;

public class CustomWebSecurityExpressionRoot extends WebSecurityExpressionRoot {

    private final RiskEngine riskEngine;
    private final AttributeInformationPoint attributePIP;
    private final AuthorizationContext authorizationContext;

    public CustomWebSecurityExpressionRoot(Authentication authentication, FilterInvocation fi,
                                           RiskEngine riskEngine, AttributeInformationPoint attributePIP,
                                           AuthorizationContext authorizationContext) {
        super(authentication, fi);
        this.riskEngine = riskEngine;
        this.attributePIP = attributePIP;
        this.authorizationContext = authorizationContext;
    }

    public int getRiskScore() {
        return riskEngine.calculateRiskScore(getAuthentication(), this.request);
    }

    /**
     * SpEL 표현식에서 #root.getAttribute('key') 형태로 동적 속성을 조회하는 메서드.
     */
    public Object getAttribute(String key) {
        // 1. 이미 컨텍스트에 로드된 속성이면 바로 반환
        if (authorizationContext.attributes().containsKey(key)) {
            return authorizationContext.attributes().get(key);
        }

        // 2. 없다면 PIP를 통해 조회하고 컨텍스트에 저장 후 반환 (Lazy Loading)
        Map<String, Object> fetchedAttributes = attributePIP.getAttributes(authorizationContext);
        authorizationContext.attributes().putAll(fetchedAttributes);

        return authorizationContext.attributes().get(key);
    }
}