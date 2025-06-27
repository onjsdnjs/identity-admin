package io.spring.iam.admin.studio.dto;

import java.util.List;

public record PolicyImpactDto(List<ExplorerItemDto> affectedSubjects, List<ExplorerItemDto> affectedResources) {}
