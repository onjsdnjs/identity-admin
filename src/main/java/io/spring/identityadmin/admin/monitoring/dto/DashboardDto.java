package io.spring.identityadmin.admin.monitoring.dto;

import io.spring.identityadmin.admin.support.context.dto.RecentActivityDto;
import java.util.List;

/**
 * [최종 구현] 대시보드에 필요한 모든 데이터를 담는 최종 DTO.
 * ui-ux.html 디자인에 맞춰 모든 필드를 포함합니다.
 */
public record DashboardDto(
        long totalUserCount,
        long activeSessionCount,
        long inactiveUserCount,
        long mfaMissingAdminCount,
        List<RecentActivityDto> recentActivities,
        List<RiskIndicatorDto> riskIndicators,
        SecurityScoreDto securityScore,
        PermissionMatrixDto permissionMatrix
) {}