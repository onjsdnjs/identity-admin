package io.spring.identityadmin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "MANAGED_RESOURCE",
        uniqueConstraints = @UniqueConstraint(columnNames = "resourceIdentifier"))
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ManagedResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String resourceIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType;

    @Enumerated(EnumType.STRING)
    private HttpMethod httpMethod;

    @Column(nullable = false)
    private String friendlyName;

    @Column(length = 1024)
    private String description;

    private String serviceOwner;

    private String parameterTypes;

    private String returnType;

    @Column(nullable = false)
    @Builder.Default
    private boolean isManaged = true;

    public enum ResourceType {
        URL, METHOD
    }

    public enum HttpMethod {
        GET, POST, PUT, DELETE
    }
}