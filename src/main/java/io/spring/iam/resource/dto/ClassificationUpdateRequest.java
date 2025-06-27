package io.spring.iam.resource.dto;

import io.spring.iam.domain.entity.ConditionTemplate;

/**
 * 분류 업데이트 요청 DTO
 * ✅ SRP 준수: 분류 업데이트 요청 정보만 담당
 */
public class ClassificationUpdateRequest {
    public ConditionTemplate.ConditionClassification classification;
    public ConditionTemplate.RiskLevel riskLevel;
    public Boolean approvalRequired;
    public Boolean contextDependent;
    public Integer complexityScore;
} 