package io.spring.identityadmin.aiam.protocol;

public class PolicyResponse extends IAMResponse {
    private PolicyDto generatedPolicy;
    private List<String> appliedConditions;
    private ConfidenceScore confidence;
}
