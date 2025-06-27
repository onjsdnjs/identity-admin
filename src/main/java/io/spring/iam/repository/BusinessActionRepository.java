package io.spring.iam.repository;

import io.spring.iam.domain.entity.business.BusinessAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessActionRepository extends JpaRepository<BusinessAction, Long> {
}
