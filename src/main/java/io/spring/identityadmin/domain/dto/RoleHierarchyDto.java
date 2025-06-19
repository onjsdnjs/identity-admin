package io.spring.identityadmin.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleHierarchyDto {
    private Long id;
    private String hierarchyString;
    private String description;
    private Boolean isActive;
    private List<HierarchyPair> hierarchyPairs = new ArrayList<>();

    public record HierarchyPair(String parentRole, String childRole) {}
}