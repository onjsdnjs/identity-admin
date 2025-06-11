package io.spring.identityadmin.security.xacml.pip.risk;

import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;
import jakarta.servlet.http.HttpServletRequest; // HttpServletRequest 임포트
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
@Order(10)
public class IpRiskEvaluator implements RiskFactorEvaluator {
    private static final Set<String> TRUSTED_IPS = Set.of("127.0.0.1", "0:0:0:0:0:0:0:1");

    @Override
    public int evaluate(AuthorizationContext context) {
        final HttpServletRequest request = context.environment().request();

        if (request != null) {
            final String remoteIp = request.getRemoteAddr();
            if (!TRUSTED_IPS.contains(remoteIp)) {
                return 30; // 신뢰되지 않은 IP
            }
            return 0; // 신뢰된 IP
        } else {
            // request 객체가 없어 IP 확인이 불가능한 경우
            return 15;
        }
    }
}