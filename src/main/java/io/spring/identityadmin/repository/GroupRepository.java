package io.spring.identityadmin.repository;

import io.spring.identityadmin.domain.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByName(String name);

    /**
     * [쿼리 수정] 올바른 Fetch Join으로 N+1 문제를 해결합니다.
     */
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.groupRoles gr LEFT JOIN FETCH gr.role r WHERE g.id = :id")
    Optional<Group> findByIdWithRoles(Long id);

    @Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.groupRoles gr LEFT JOIN FETCH gr.role LEFT JOIN FETCH g.userGroups ug ORDER BY g.name ASC")
    List<Group> findAllWithRolesAndUsers();
}