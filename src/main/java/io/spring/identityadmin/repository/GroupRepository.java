package io.spring.identityadmin.repository;

import io.spring.identityadmin.domain.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByName(String name);

    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.groupRoles gr LEFT JOIN FETCH gr.role WHERE g.id = :id")
    Optional<Group> findByIdWithRoles(Long id);

    /**
     * N+1 문제 해결을 위해 모든 Group과 연관된 Role 및 UserGroup 정보를 함께 조회합니다.
     * @return Group 리스트
     */
    @Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.groupRoles gr LEFT JOIN FETCH gr.role LEFT JOIN FETCH g.userGroups ug ORDER BY g.name ASC")
    List<Group> findAllWithRolesAndUsers();
}
