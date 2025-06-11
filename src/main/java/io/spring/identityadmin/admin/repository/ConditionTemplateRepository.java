package io.spring.identityadmin.admin.repository;

import io.spring.identityadmin.domain.entity.ConditionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConditionTemplateRepository extends JpaRepository<ConditionTemplate, Long> {
}
