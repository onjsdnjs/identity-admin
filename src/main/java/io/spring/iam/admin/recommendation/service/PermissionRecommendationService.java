package io.spring.iam.admin.recommendation.service;

import io.spring.iam.admin.recommendation.dto.*;
import java.util.List;

/**
 * AI/규칙 기반으로 사용자에게 최적의 권한 설정을 추천하는 서비스입니다.
 */
public interface PermissionRecommendationService {
    /**
     * 특정 주체에게 가장 연관성이 높은 권한을 추천합니다.
     */
    List<RecommendedResourceDto> recommendPermissionsForSubject(SubjectContext subjectContext);
}