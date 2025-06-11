package io.spring.identityadmin.repository;

import io.spring.identityadmin.domain.entity.FunctionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FunctionGroupRepository extends JpaRepository<FunctionGroup, Long> {}
