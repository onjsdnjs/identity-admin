package io.spring.iam.common.event.service;

import io.spring.iam.common.event.dto.DomainEvent;

public interface IntegrationEventBus {
    void publish(DomainEvent event);
    <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler);
}