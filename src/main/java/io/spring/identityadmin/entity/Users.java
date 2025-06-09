package io.spring.identityadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    // --- MFA 관련 필드 ---
    @Column(nullable = false)
    private boolean mfaEnabled; // 사용자가 MFA를 활성화했는지 여부

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date lastMfaUsedAt;
}