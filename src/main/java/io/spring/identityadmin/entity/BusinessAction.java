package io.spring.identityadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * 일반 사용자가 인가 정책을 설정할 때 사용하는 비즈니스 행위를 정의하는 엔티티.
 * (예: '읽기', '쓰기', '결재' 등)
 */
@Entity
@Table(name = "BUSINESS_ACTION")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessAction implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // 사용자에게 표시될 행위 이름 (예: 조회하기)

    @Column(nullable = false)
    private String actionType; // Permission의 actionType과 매핑될 기술적 타입 (예: READ)

    @Column(nullable = false)
    private String mappedPermissionName; // 예: "PERMISSION_READ"

    @Column(length = 1024)
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BusinessAction that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
