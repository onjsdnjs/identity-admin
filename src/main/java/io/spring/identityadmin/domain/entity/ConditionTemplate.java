package io.spring.identityadmin.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * 일반 사용자가 인가 정책을 설정할 때 선택할 수 있는, 미리 정의된 조건 템플릿을 정의하는 엔티티.
 * (예: '업무 시간에만 허용', '사내 IP에서만 허용' 등)
 */
@Entity
@Table(name = "CONDITION_TEMPLATE")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionTemplate implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // 사용자에게 표시될 조건 이름 (예: 업무 시간 제약)

    @Column(nullable = false, length = 2048)
    private String spelTemplate; // 이 조건이 선택되었을 때 실제로 생성될 SpEL 표현식 (예: #isBusinessHours())

    @Column
    private String category; // UI에서 조건을 그룹핑하기 위한 카테고리 (예: 시간 기반, 위치 기반)

    @Column
    private int parameterCount; // 이 조건을 사용하는 데 필요한 파라미터 수 (예: '금액 비교' 조건은 1개의 파라미터 필요)

    @Column(length = 1024)
    private String description; // 조건에 대한 설명

    @Column(length = 1024)
    private String requiredTargetType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConditionTemplate that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
