package io.spring.iam.repository;

import io.spring.iam.domain.entity.WizardSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WizardSessionRepository extends JpaRepository<WizardSession, String> {
}
