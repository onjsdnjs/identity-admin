package io.spring.iam.common.event.service;

import io.spring.iam.common.event.dto.DomainEvent;

@FunctionalInterface
public interface EventHandler<T extends DomainEvent> {
    void handle(T event);
}
