package io.spring.identityadmin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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
    @MapsId("businessResourceId") // 복합 키의 businessResourceId 필드에 매핑
    @JoinColumn(name = "business_resource_id")
    private BusinessResource businessResource;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("businessActionId") // 복합 키의 businessActionId 필드에 매핑
    @JoinColumn(name = "business_action_id")
    private BusinessAction businessAction;

    @Column(name = "mapped_permission_name", nullable = false)
    private String mappedPermissionName;
}
