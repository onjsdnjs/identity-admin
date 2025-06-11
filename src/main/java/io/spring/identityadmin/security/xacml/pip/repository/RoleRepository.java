package io.spring.identityadmin.security.xacml.pip.repository;

import io.spring.identityadmin.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(String name);

    @Override
    void delete(Role role);

    @Query("select r from Role r where r.isExpression = 'N'")
    List<Role> findAllRolesWithoutExpression();

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.rolePermissions p WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(Long id);

    /**
     * N+1 문제 해결을 위해 모든 Role과 연관된 Permission을 함께 조회합니다.
     * @return Role 리스트
     */
    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.rolePermissions")
    List<Role> findAllWithPermissions();
}