package io.spring.identityadmin.aiam.labs.risk;

public class IntelligentRiskAssessmentLab extends AbstractIAMLab<RiskContext, RiskAssessmentResponse> {
    @Override
    public Mono<RiskAssessmentResponse> execute(IAMRequest<RiskContext> request, Class<RiskAssessmentResponse> responseType) {
        return aiamOperations.assessRisk((RiskRequest<RiskContext>) request);
    }
}
