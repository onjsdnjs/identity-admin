package io.spring.identityadmin.aiam.dto;

public record ConditionValidationResponse(boolean isCompatible, String reason) {}
