package io.spring.identityadmin.admin.repository;

import io.springsecurity.springsecurity6x.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserManagementRepository extends JpaRepository<Users, Long> { }
