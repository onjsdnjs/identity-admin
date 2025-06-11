package io.spring.identityadmin.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "PERMISSION")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Permission implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long id;

    @Column(name = "permission_name", unique = true, nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "condition_expression", length = 2048)
    private String conditionExpression;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "PERMISSION_FUNCTIONS",
            joinColumns = @JoinColumn(name = "permission_id"),
            inverseJoinColumns = @JoinColumn(name = "function_catalog_id")
    )
    @Builder.Default
    private Set<FunctionCatalog> functions = new HashSet<>();
}