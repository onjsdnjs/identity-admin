package io.spring.identityadmin.common.event.service;

/**
 * 시스템 내의 주요 도메인 이벤트(예: 정책 변경, 사용자 생성)를 발행하고 구독하는 이벤트 버스입니다.
 * 캐시 무효화, 실시간 알림 등 비동기 처리에 사용됩니다.
 */
public interface IntegrationEventBus {
    /**
     * 시스템에 새로운 이벤트를 발행합니다.
     * @param event 발행할 도메인 이벤트
     */
    void publish(DomainEvent event);

    /**
     * 특정 타입의 이벤트를 구독하여 처리할 핸들러를 등록합니다.
     * @param eventType 구독할 이벤트 타입
     * @param handler 이벤트를 처리할 핸들러
     */
    void subscribe(String eventType, EventHandler handler);
}
