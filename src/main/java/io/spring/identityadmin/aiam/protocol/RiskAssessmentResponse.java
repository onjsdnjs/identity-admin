package io.spring.identityadmin.aiam.protocol;

public class RiskAssessmentResponse extends IAMResponse {
    private RiskLevel riskLevel;
    private List<RiskFactor> identifiedRisks;
    private List<String> recommendations;
}
