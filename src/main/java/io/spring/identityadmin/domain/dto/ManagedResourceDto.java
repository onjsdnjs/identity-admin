package io.spring.identityadmin.domain.dto;

import io.spring.identityadmin.domain.entity.ManagedResource;
import lombok.Data;

@Data
public class ManagedResourceDto {
    private Long id;
    private String resourceIdentifier;
    private ManagedResource.ResourceType resourceType;
    private String friendlyName;
    private String description;
    private String serviceOwner;
    private String parameterTypes;
    private String returnType;
    private boolean isManaged;
}
