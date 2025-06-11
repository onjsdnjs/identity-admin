package io.spring.identityadmin.repository;

import io.spring.identityadmin.domain.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserManagementRepository extends JpaRepository<Users, Long> { }
