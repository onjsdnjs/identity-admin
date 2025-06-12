package io.spring.identityadmin.admin.dashboard.service;

import java.util.stream.Stream;

/**
 * 관리자용 통합 대시보드에 필요한 모든 데이터를 제공하는 서비스입니다.
 */
public interface DashboardService {

    /**
     * 조직의 전반적인 보안 설정 상태를 평가하여 정량적인 점수로 반환합니다.
     * (예: MFA 설정률, 과도한 권한 보유자 수 등을 기반으로 계산)
     * @return 보안 점수와 상세 평가 항목
     */
    SecurityScoreDto getSecurityScore();

    /**
     * 즉각적인 조치가 필요할 수 있는 주요 위험 지표를 분석하여 제공합니다.
     * (예: 장기 미사용 관리자 계정, 과도한 권한을 가진 역할, MFA 미설정 관리자 등)
     * @return 위험 지표 목록과 각 지표에 대한 상세 정보
     */
    List<RiskIndicatorDto> analyzeRiskIndicators();

    /**
     * 시스템의 최신 활동 로그를 실시간 스트림으로 제공합니다. (WebSocket과 연동 가능)
     * @param filter 활동 로그 필터링 조건
     * @return 실시간 활동 이벤트 스트림
     */
    Stream<ActivityEventDto> getActivityStream(ActivityFilter filter);

    /**
     * '역할/그룹'과 '리소스' 간의 권한 관계를 매트릭스 형태로 시각화하기 위한 데이터를 조회합니다.
     * @param filter 매트릭스 필터링 조건 (특정 그룹, 리소스 타입 등)
     * @return UI에서 테이블로 렌더링할 수 있는 권한 매트릭스 데이터
     */
    PermissionMatrixDto getPermissionMatrix(MatrixFilter filter);
}

