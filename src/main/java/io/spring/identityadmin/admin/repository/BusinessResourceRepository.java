package io.spring.identityadmin.admin.repository;

import io.spring.identityadmin.entity.BusinessResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessResourceRepository extends JpaRepository<BusinessResource, Long> {
}