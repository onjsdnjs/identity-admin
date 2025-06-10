package io.spring.identityadmin.repository;

import io.spring.identityadmin.entity.Users;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {

    /**
     * username 으로 Users 엔티티를 조회하면서,
     * 연결된 Group, GroupRole, Role, RolePermission, Permission 엔티티를 모두 FETCH JOIN합니다.
     * 이를 통해 N+1 쿼리 문제를 방지하고 CustomUserDetails에서 모든 권한 정보를 효율적으로 로드합니다.
     */
    @Cacheable(value = "usersWithAuthorities", key = "#username")
    @Query("SELECT u FROM Users u " +
            "LEFT JOIN FETCH u.userGroups ug " +      // <<< LEFT JOIN FETCH로 변경 (사용자가 그룹에 속하지 않은 경우도 고려)
            "LEFT JOIN FETCH ug.group g " +
            "LEFT JOIN FETCH g.groupRoles gr " +
            "LEFT JOIN FETCH gr.role r " +
            "LEFT JOIN FETCH r.rolePermissions rp " +
            "LEFT JOIN FETCH rp.permission p " +
            "WHERE u.username = :username")
    Optional<Users> findByUsernameWithGroupsRolesAndPermissions(String username);

    @Cacheable(value = "usersWithAuthorities", key = "#id")
    @Query("SELECT u FROM Users u " +
            "LEFT JOIN FETCH u.userGroups ug " +      // <<< LEFT JOIN FETCH로 변경
            "LEFT JOIN FETCH ug.group g " +
            "LEFT JOIN FETCH g.groupRoles gr " +
            "LEFT JOIN FETCH gr.role r " +
            "LEFT JOIN FETCH r.rolePermissions rp " +
            "LEFT JOIN FETCH rp.permission p " +
            "WHERE u.id = :id")
    Optional<Users> findByIdWithGroupsRolesAndPermissions(Long id);

    // UserManagementService 에서 사용할 N+1 해결 쿼리
    @Query("SELECT DISTINCT u FROM Users u " +
            "LEFT JOIN FETCH u.userGroups ug " +
            "LEFT JOIN FETCH ug.group g " +
            "LEFT JOIN FETCH g.groupRoles gr " +
            "LEFT JOIN FETCH gr.role r " +
            "LEFT JOIN FETCH r.rolePermissions rp " +
            "LEFT JOIN FETCH rp.permission p")
    List<Users> findAllWithDetails();

    /**
     * [쿼리 최종 수정] JOIN을 사용하여 'ADMIN' 역할을 가지면서 MFA가 비활성화된 사용자를 최적화된 방식으로 조회합니다.
     * @return MFA가 비활성화된 관리자 계정 목록
     */
    @Query("SELECT DISTINCT u FROM Users u " +
            "JOIN u.userGroups ug " +
            "JOIN ug.group g " +
            "JOIN g.groupRoles gr " +
            "JOIN gr.role r " +
            "WHERE u.mfaEnabled = false AND r.roleName = 'ADMIN'")
    List<Users> findAdminsWithMfaDisabled();
}

