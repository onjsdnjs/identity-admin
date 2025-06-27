package io.spring.iam.aiam.dto;

import java.util.List;

public record TrustAssessment(
        double score, // 0.0 ~ 1.0
        List<String> riskTags, // 예: ["NEW_IP", "OFF_HOURS"]
        String summary
) {}