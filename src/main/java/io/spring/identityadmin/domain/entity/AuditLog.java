package io.spring.identityadmin.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class) // 생성일자 자동 기록을 위해 추가
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    @CreatedDate // 엔티티 생성 시각 자동 저장
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String principalName; // 요청 주체 (사용자 이름)

    @Column(nullable = false, length = 512)
    private String resourceIdentifier; // 접근 대상 자원 (URL 또는 메서드명)

    private String action; // 요청 행위 (HTTP Method 등)

    @Column(nullable = false)
    private String decision; // 인가 결정 (ALLOW / DENY)

    @Column(length = 1024)
    private String reason; // 결정 근거 (예: Policy ID: 1, 에러 메시지)

    private String clientIp; // 요청 클라이언트 IP
}
