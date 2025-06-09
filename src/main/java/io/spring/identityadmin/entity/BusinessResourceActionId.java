package io.spring.identityadmin.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * BusinessResourceAction 엔티티의 복합 키를 위한 ID 클래스.
 * public으로 선언되어 다른 패키지에서도 접근 가능해야 한다.
 */
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class BusinessResourceActionId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long businessResourceId;
    private Long businessActionId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BusinessResourceActionId that = (BusinessResourceActionId) o;
        return Objects.equals(businessResourceId, that.businessResourceId) && Objects.equals(businessActionId, that.businessActionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessResourceId, businessActionId);
    }
}