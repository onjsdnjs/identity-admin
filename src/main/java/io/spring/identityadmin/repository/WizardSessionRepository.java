package io.spring.identityadmin.repository;

import io.spring.identityadmin.domain.entity.WizardSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WizardSessionRepository extends JpaRepository<WizardSession, String> {
}
