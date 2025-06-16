/*
// 신규 인터페이스 제안
package io.spring.identityadmin.ai.service;

import io.spring.identityadmin.admin.recommendation.dto.SubjectContext;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;

public interface AiAuthorizationAdvisor {

    */
/**
     * 신규 리소스에 대한 최적의 권한 명칭과 설명을 추천합니다.
     * @param resource 기술적 리소스 정보
     * @return AI가 추천하는 비즈니스 이름과 설명
     *//*

    PermissionRecommendation suggestPermissionForResource(ManagedResource resource);

    */
/**
     * 특정 주체(사용자/그룹)에게 가장 적합한 역할을 추천합니다.
     * @param subjectContext 주체의 현재 정보 (소속 그룹, 직무 등)
     * @return 추천 역할 목록과 그 이유
     *//*

    List<RoleRecommendation> recommendRolesForSubject(SubjectContext subjectContext);

    */
/**
     * 주어진 인가 컨텍스트의 리스크 점수를 평가합니다.
     * @param context 인증/인가 시점의 모든 컨텍스트 정보
     * @return 0-100 사이의 리스크 점수와 평가 근거
     *//*

    RiskAssessment assessRisk(AuthorizationContext context);

    */
/**
     * 두 정책 간의 의미론적 충돌이나 중복을 분석합니다.
     * @param policyA 비교할 정책 A
     * @param policyB 비교할 정책 B
     * @return 정책 분석 리포트
     *//*

    PolicyAnalysisReport analyzePolicyConflict(Policy policyA, Policy policyB);
}*/
