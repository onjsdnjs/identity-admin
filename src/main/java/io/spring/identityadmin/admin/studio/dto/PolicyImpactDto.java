package io.spring.identityadmin.admin.studio.dto;

import java.util.List;

public record PolicyImpactDto(List<ExplorerItemDto> affectedSubjects, List<ExplorerItemDto> affectedResources) {}
