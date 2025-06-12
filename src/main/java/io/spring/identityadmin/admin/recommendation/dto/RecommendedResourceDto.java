package io.spring.identityadmin.admin.recommendation.dto;

public record RecommendedResourceDto(Long permissionId, String permissionName, String reason, double score) {}