package io.spring.identityadmin.security.xacml.pdp.evaluation.url;

import io.spring.identityadmin.ai.AINativeIAMAdvisor;
import io.spring.identityadmin.ai.dto.TrustAssessment;
import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;
import io.spring.identityadmin.security.xacml.pip.attribute.AttributeInformationPoint;
import io.spring.identityadmin.security.xacml.pip.risk.RiskEngine;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.expression.WebSecurityExpressionRoot;

import java.util.Map;

public class CustomWebSecurityExpressionRoot extends WebSecurityExpressionRoot {

    private final RiskEngine riskEngine;
    private final AttributeInformationPoint attributePIP;
    private final AINativeIAMAdvisor aINativeIAMAdvisor; // RiskEngine 대신 주입
    private final AuthorizationContext authorizationContext;
    private TrustAssessment cachedAssessment; // 평가 결과를 캐싱

    public CustomWebSecurityExpressionRoot(Authentication authentication, HttpServletRequest request,
                                           RiskEngine riskEngine, AttributeInformationPoint attributePIP,
                                           AINativeIAMAdvisor aINativeIAMAdvisor, // RiskEngine 대신 주입
                                           AuthorizationContext authorizationContext) {
        super(() -> authentication, request);
        this.riskEngine = riskEngine;
        this.attributePIP = attributePIP;
        this.authorizationContext = authorizationContext;
        this.aINativeIAMAdvisor = aINativeIAMAdvisor;
    }

    public int getRiskScore() {
        // [최종 수정] 자신이 가진 표준 컨텍스트 객체를 그대로 전달합니다.
        return riskEngine.calculateRiskScore(this.authorizationContext);
    }

    public double getTrustScore() {
        if (this.cachedAssessment == null) {
            this.cachedAssessment = aINativeIAMAdvisor.assessContext(this.authorizationContext);
        }
        return this.cachedAssessment.score();
    }

    /**
     * AI 신뢰도 평가를 수행하고, 그 결과를 컨텍스트에 저장합니다.
     * @return AI가 평가한 신뢰도 평가 결과 객체
     */
    public TrustAssessment assessContext() {
        if (this.cachedAssessment == null) {
            this.cachedAssessment = aINativeIAMAdvisor.assessContext(this.authorizationContext);
            // 평가 결과를 컨텍스트의 attributes 맵에 저장하여, 상위 로직(감사 로그)에서 참조할 수 있도록 함
            this.authorizationContext.attributes().put("ai_assessment", this.cachedAssessment);
        }
        return this.cachedAssessment;
    }

    public AINativeIAMAdvisor getAi() {
        return this.aINativeIAMAdvisor;
    }

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