package io.spring.identityadmin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "BUSINESS_RESOURCE_ACTION")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessResourceAction {

    @EmbeddedId
    private BusinessResourceActionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("businessResourceId")
    private BusinessResource businessResource;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("businessActionId")
    private BusinessAction businessAction;

    @Column(name = "mapped_permission_name", nullable = false)
    private String mappedPermissionName; // 예: "DOCUMENT_READ"
}
