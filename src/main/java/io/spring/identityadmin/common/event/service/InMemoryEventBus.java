package io.spring.identityadmin.common.event.service;

import io.spring.identityadmin.common.event.dto.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class InMemoryEventBus implements IntegrationEventBus {
    private final Map<Class<? extends DomainEvent>, List<EventHandler>> subscribers = new ConcurrentHashMap<>();

    @Override
    public void publish(DomainEvent event) {
        log.info("Publishing event: {}", event.getClass().getSimpleName());
        subscribers.getOrDefault(event.getClass(), Collections.emptyList())
                .forEach(handler -> handler.handle(event));
    }

    @Override
    public <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
        log.info("Handler {} subscribed to event {}", handler.getClass().getSimpleName(), eventType.getSimpleName());
    }
}