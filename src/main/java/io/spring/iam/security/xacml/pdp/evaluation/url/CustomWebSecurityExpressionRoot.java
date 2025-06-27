package io.spring.iam.security.xacml.pdp.evaluation.url;

import io.spring.iam.aiam.AINativeIAMAdvisor;
import io.spring.iam.aiam.dto.TrustAssessment;
import io.spring.iam.security.xacml.pip.context.AuthorizationContext;
import io.spring.iam.security.xacml.pip.attribute.AttributeInformationPoint;
import io.spring.iam.security.xacml.pip.risk.RiskEngine;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.expression.WebSecurityExpressionRoot;

import java.util.Map;

@Slf4j
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

    /**
     * [최종 코어 메서드] AI 신뢰도 평가를 수행하고, 그 결과를 컨텍스트와 내부 캐시에 저장합니다.
     * SpEL에서 #ai.assessContext() 와 같이 호출하여 전체 평가 결과에 접근할 때 사용됩니다.
     * @return AI가 평가한 신뢰도 평가 결과 객체 (점수, 위험 태그, 요약 포함)
     */
    public TrustAssessment assessContext() {
        if (this.cachedAssessment == null) {
            log.debug("AI context assessment cache is empty. Requesting new assessment...");
            this.cachedAssessment = aINativeIAMAdvisor.assessContext(this.authorizationContext);
            // 평가 결과를 컨텍스트의 attributes 맵에 저장하여, 감사 로그 등 상위 로직에서 참조할 수 있도록 함
            this.authorizationContext.attributes().put("ai_assessment", this.cachedAssessment);
        }
        return this.cachedAssessment;
    }

    /**
     * [편의 메서드] 신뢰도 '점수'만 간단히 확인하고 싶을 때 사용합니다.
     * SpEL 에서 #ai.getTrustScore() 와 같이 사용됩니다.
     * @return 신뢰도 점수 (0.0 ~ 1.0)
     */
    public double getTrustScore() {
        return assessContext().score();
    }

    /**
     * [편의 메서드] 신뢰도를 0-100점 스케일의 '위험도'로 변환하여 확인하고 싶을 때 사용합니다.
     * SpEL 에서 #ai.getRiskScore() 와 같이 사용됩니다.
     * @return 위험도 점수 (0 ~ 100)
     */
    public int getRiskScore() {
        // 예: 신뢰도 0.8 -> 위험도 20점
        return (int) Math.round((1.0 - getTrustScore()) * 100);
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