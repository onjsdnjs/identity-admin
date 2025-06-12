package io.spring.identityadmin.common.event.service;

import io.spring.identityadmin.common.event.dto.DomainEvent;

@FunctionalInterface
public interface EventHandler<T extends DomainEvent> {
    void handle(T event);
}
