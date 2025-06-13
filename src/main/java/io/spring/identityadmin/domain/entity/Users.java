package io.spring.identityadmin.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username; // 일반적으로 이메일 주소 사용

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true) // UserGroup 엔티티의 'user' 필드에 매핑
    @Builder.Default
    @ToString.Exclude
    private Set<UserGroup> userGroups = new HashSet<>(); // 사용자가 속한 그룹들

    @Column(nullable = false)
    private boolean mfaEnabled; // 사용자가 MFA를 활성화했는지 여부

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date lastMfaUsedAt;

    /**
     * [신규] 사용자가 가진 모든 역할의 이름을 중복 없이 정렬하여 반환합니다.
     */
    @Transient
    public List<String> getRoleNames() {
        if (userGroups == null || userGroups.isEmpty()) {
            return Collections.emptyList();
        }
        return this.userGroups.stream()
                .map(UserGroup::getGroup)
                .filter(Objects::nonNull)
                .flatMap(group -> group.getGroupRoles().stream())
                .map(GroupRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getRoleName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    /**
     * [신규] 사용자가 가진 모든 권한의 이름을 중복 없이 정렬하여 반환합니다.
     */
    @Transient
    public List<String> getPermissionNames() {
        if (userGroups == null || userGroups.isEmpty()) {
            return Collections.emptyList();
        }
        return this.userGroups.stream()
                .map(UserGroup::getGroup)
                .filter(Objects::nonNull)
                .flatMap(group -> group.getGroupRoles().stream())
                .map(GroupRole::getRole)
                .filter(Objects::nonNull)
                .flatMap(role -> role.getRolePermissions().stream())
                .map(RolePermission::getPermission)
                .filter(Objects::nonNull)
                .map(Permission::getName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // 프록시 객체 등을 고려하여 o.getClass() 대신 instanceof 사용
        if (!(o instanceof Users users)) return false;
        // id가 null이 아닌(영속화된) 객체들만 ID를 기준으로 비교
        return id != null && Objects.equals(id, users.id);
    }

    @Override
    public int hashCode() {
        // 모든 인스턴스가 고유한 해시코드를 갖도록 하거나,
        // 비즈니스 키 또는 ID 기반으로 구현. 여기서는 클래스 해시코드를 사용.
        return getClass().hashCode();
    }
}