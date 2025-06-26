package io.spring.identityadmin.aiam.dto;

public record ConditionValidationRequest(String resourceIdentifier, String conditionSpel) {}