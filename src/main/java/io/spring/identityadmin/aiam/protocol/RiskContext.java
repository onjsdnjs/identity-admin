package io.spring.identityadmin.aiam.protocol;

public class RiskContext extends IAMContext {
    private Map<String, Object> currentPolicies;
    private List<SecurityEvent> recentEvents;
    private ThreatIntelligence threatData;
    // 위험 분석에 필요한 컨텍스트
}