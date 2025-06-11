package io.spring.identityadmin.security.xacml.pdp.evaluation.url;

import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;
import io.spring.identityadmin.security.xacml.pip.attribute.AttributeInformationPoint;
import io.spring.identityadmin.security.xacml.pip.risk.RiskEngine;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.WebSecurityExpressionRoot;

import java.util.Map;

public class CustomWebSecurityExpressionRoot extends WebSecurityExpressionRoot {

    private final RiskEngine riskEngine;
    private final AttributeInformationPoint attributePIP;
    private final AuthorizationContext authorizationContext;

    public CustomWebSecurityExpressionRoot(Authentication authentication, HttpServletRequest request,
                                           RiskEngine riskEngine, AttributeInformationPoint attributePIP,
                                           AuthorizationContext authorizationContext) {
        super(() -> authentication, request);
        this.riskEngine = riskEngine;
        this.attributePIP = attributePIP;
        this.authorizationContext = authorizationContext;
    }

    public int getRiskScore() {
        // [최종 수정] 자신이 가진 표준 컨텍스트 객체를 그대로 전달합니다.
        return riskEngine.calculateRiskScore(this.authorizationContext);
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