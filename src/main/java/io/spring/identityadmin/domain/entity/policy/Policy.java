package io.spring.identityadmin.domain.entity.policy;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Table(name = "POLICY")
public class Policy implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Effect effect;

    @Column(nullable = false)
    private int priority;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PolicyTarget> targets = new HashSet<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PolicyRule> rules = new HashSet<>();

    @Column(length = 2048)
    private String friendlyDescription;

    public enum Effect { ALLOW, DENY }

    // [신규] 양방향 관계 설정을 위한 편의 메서드 추가
    public void addTarget(PolicyTarget target) {
        this.targets.add(target);
        target.setPolicy(this);
    }

    // [신규] 양방향 관계 설정을 위한 편의 메서드 추가
    public void addRule(PolicyRule rule) {
        this.rules.add(rule);
        rule.setPolicy(this);
    }
}