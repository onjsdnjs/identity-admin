package io.spring.aicore.adaptation;

import io.spring.aicore.protocol.DomainContext;

import java.util.Optional;

public interface DomainAdapterRegistry<T extends DomainContext> {
    <D> Optional<DomainAdapter<T, D>> getAdapter(Class<D> domainType);
    <D> void registerAdapter(Class<D> domainType, DomainAdapter<T, D> adapter);
}