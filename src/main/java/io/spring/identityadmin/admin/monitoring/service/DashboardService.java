package io.spring.identityadmin.admin.monitoring.service;

import io.spring.identityadmin.admin.monitoring.dto.*;

import java.util.List;

/**
 * [역할 확장] 관리자용 통합 대시보드에 필요한 모든 고도화된 데이터를 제공합니다.
 */
public interface DashboardService {
    List<RiskIndicatorDto> analyzeRiskIndicators();
    // Stream<ActivityEventDto> getActivityStream(ActivityFilter filter); // 실시간 기능은 별도 서비스로
    DashboardDto getDashboardData();
}