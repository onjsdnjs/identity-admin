package io.spring.iam.repository;

import io.spring.iam.domain.entity.FunctionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FunctionGroupRepository extends JpaRepository<FunctionGroup, Long> { }
