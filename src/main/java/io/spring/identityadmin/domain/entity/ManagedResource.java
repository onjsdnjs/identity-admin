package io.spring.identityadmin.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "MANAGED_RESOURCE",
        uniqueConstraints = @UniqueConstraint(columnNames = "resourceIdentifier"))
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ManagedResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String resourceIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType;

    @Enumerated(EnumType.STRING)
    private HttpMethod httpMethod;

    @Column(nullable = false)
    private String friendlyName;

    @Column(length = 1024)
    private String description;

    private String serviceOwner;

    // --- [기존] 상세 기술 정보 ---
    private String parameterTypes;
    private String returnType;

    // --- [신규] 관리자 판단을 위한 컨텍스트 정보 ---
    @Column(length = 2048)
    private String sourceCodeLocation; // 예: "com.example.service.OrderService.java:42"

    @Column(columnDefinition = "TEXT")
    private String javadocContent; // 수집 가능한 경우 JavaDoc 내용

    @Column(length = 2048)
    private String apiDocsUrl; // Spring REST Docs 문서의 앵커 링크

    @Column(nullable = false)
    @Builder.Default
    private boolean isManaged = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDefined = false;

    public enum ResourceType { URL, METHOD }
    public enum HttpMethod { GET, POST, PUT, DELETE, PATCH, ANY }
}