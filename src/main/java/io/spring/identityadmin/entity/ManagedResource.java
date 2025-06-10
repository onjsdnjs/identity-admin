package io.spring.identityadmin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "MANAGED_RESOURCE")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ManagedResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String resourceIdentifier; // 기술적 식별자 (e.g., /api/users, com.example.service.UserService.getUsers)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType; // URL, METHOD 등

    @Column(nullable = false)
    private String friendlyName; // 사용자 친화적 이름 (e.g., "사용자 목록 조회")

    @Column(length = 1024)
    private String description; // 리소스에 대한 설명

    private String serviceOwner; // 이 리소스를 소유한 서비스 또는 모듈 (e.g., "회원 관리 서비스")

    @Column(length = 1024)
    private String defaultRule; // @PreAuthorize 등에서 스캔된 기본 규칙 SpEL

    @Column(nullable = false)
    @Builder.Default
    private boolean isManaged = true;

    public enum ResourceType {
        URL, METHOD
    }
}
