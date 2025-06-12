package io.spring.identityadmin.admin.monitoring.service;

/**
 * 시스템의 활동을 실시간으로 감시하고 보안 위협을 탐지하는 서비스입니다.
 */
public interface RealtimeMonitoringService {
    /**
     * 정의된 규칙(예: 짧은 시간 내에 과도한 로그인 실패, 중요 권한 변경)에 따라 이상 행동을 감지합니다.
     * @param event 모니터링할 실시간 활동 이벤트
     */
    void detectAnomalies(ActivityEventDto event);

    /**
     * 감지된 이상 행동이나 보안 정책 위반 시 관리자에게 알림을 발송합니다.
     * @param alert 발송할 알림의 상세 내용
     */
    void sendAlert(SecurityAlertDto alert);
}