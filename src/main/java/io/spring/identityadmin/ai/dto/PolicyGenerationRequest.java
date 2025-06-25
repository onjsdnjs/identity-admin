package io.spring.identityadmin.ai.dto;

import java.util.List;
import java.util.Map;

public record PolicyGenerationRequest(
    String naturalLanguageQuery,
    AvailableItems availableItems
) {
    
    public record AvailableItems(
        List<RoleItem> roles,
        List<PermissionItem> permissions,
        List<ConditionItem> conditions
    ) {}
    
    public record RoleItem(
        Long id,
        String name,
        String description
    ) {}
    
    public record PermissionItem(
        Long id,
        String name,
        String description
    ) {}
    
    public record ConditionItem(
        Long id,
        String name,
        String description,
        Boolean isCompatible
    ) {}
} 