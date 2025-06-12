package io.spring.identityadmin.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "POLICY_TEMPLATE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String templateId; // "new-hire-template"

    @Column(nullable = false)
    private String name; // "신입사원 기본 권한 세트"

    @Column(length = 1024)
    private String description;

    @Column
    private String category; // "HR", "Finance", "Development"

    @Lob
    @Column(nullable = false, name = "policy_draft_json")
    private String policyDraftJson; // PolicyDto를 JSON으로 직렬화하여 저장
}
