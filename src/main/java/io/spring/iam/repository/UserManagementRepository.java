package io.spring.iam.repository;

import io.spring.iam.domain.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserManagementRepository extends JpaRepository<Users, Long> { }
