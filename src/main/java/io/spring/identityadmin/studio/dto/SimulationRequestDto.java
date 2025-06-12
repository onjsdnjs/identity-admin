package io.spring.identityadmin.studio.dto;

import io.spring.identityadmin.domain.entity.policy.Policy;

public record SimulationRequestDto(String action, Policy policyDraft) {} // action: "CREATE", "UPDATE", "DELETE"