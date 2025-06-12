package io.spring.identityadmin.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "WIZARD_SESSION")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WizardSession {

    @Id
    @Column(name = "session_id", length = 36)
    private String id;

    @Lob
    @Column(name = "context_data", nullable = false, columnDefinition = "TEXT")
    private String contextData; // WizardContext 객체를 JSON으로 직렬화하여 저장

    @Column(nullable = false)
    private Long ownerUserId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public static WizardSession create(String id, String contextData, Long ownerUserId, int expirationMinutes) {
        WizardSession session = new WizardSession();
        session.setId(id);
        session.setContextData(contextData);
        session.setOwnerUserId(ownerUserId);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));
        return session;
    }
}
