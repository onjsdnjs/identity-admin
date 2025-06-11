package io.spring.identityadmin.repository;

import io.spring.identityadmin.domain.entity.business.BusinessResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessResourceRepository extends JpaRepository<BusinessResource, Long> {
}