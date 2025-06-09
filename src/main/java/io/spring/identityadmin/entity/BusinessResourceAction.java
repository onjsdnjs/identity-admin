package io.spring.identityadmin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import java.io.Serializable;

@Entity
@Table(name = "BUSINESS_RESOURCE_ACTION")
@Getter
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

@Embeddable
class BusinessResourceActionId implements Serializable {
    private Long businessResourceId;
    private Long businessActionId;
}
