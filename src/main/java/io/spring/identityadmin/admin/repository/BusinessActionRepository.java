package io.spring.identityadmin.admin.repository;

import io.spring.identityadmin.entity.BusinessAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessActionRepository extends JpaRepository<BusinessAction, Long> {
}
