package io.spring.identityadmin.admin.recommendation.service;

/**
 * AI 기반으로 사용자에게 최적의 권한 설정을 추천하는 서비스입니다.
 */
public interface PermissionRecommendationService {

    /**
     * 특정 주체(사용자/그룹)에게 가장 연관성이 높거나 자주 사용되는 리소스를 추천합니다.
     * @param subjectContext 주체 정보
     * @return 추천 리소스 목록과 추천 근거
     */
    List<RecommendedResourceDto> recommendResourcesForSubject(SubjectContext subjectContext);

    /**
     * 특정 주체와 리소스 조합에 대해 가장 적절한 권한 수준(읽기, 편집 등)을 추천합니다.
     * @param subjectContext 주체 정보
     * @param resourceContext 리소스 정보
     * @return 추천 권한 수준과 추천 근거
     */
    RecommendedPermissionLevel recommendPermissionLevel(SubjectContext subjectContext, ResourceContext resourceContext);

    /**
     * 특정 주체와 유사한 다른 주체(예: 같은 부서, 같은 역할)의 권한 설정 패턴을 조회하여 제공합니다.
     * @param subjectContext 기준이 되는 주체 정보
     * @return 유사 권한 설정 패턴 목록
     */
    List<PermissionPatternDto> findSimilarPermissionPatterns(SubjectContext subjectContext);
}
