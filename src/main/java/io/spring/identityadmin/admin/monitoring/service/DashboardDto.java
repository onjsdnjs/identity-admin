package io.spring.identityadmin.admin.monitoring.service;

import lombok.Builder;
import java.util.List;

/**
 * 대시보드에 표시될 모든 데이터를 담는 DTO
 */
@Builder
public record DashboardDto(
        long userCount,
        long groupCount,
        long roleCount,
        long policyCount,
        List<ActivityLog> recentActivities,
        List<SecurityAlert> securityAlerts
) {
    /**
     * 최근 활동 로그를 표현하는 내부 레코드
     */
    public record ActivityLog(String timestamp, String activity) {}

    /**
     * 보안 알림을 표현하는 내부 레코드
     */
    public record SecurityAlert(String level, String message, String link) {}
}
