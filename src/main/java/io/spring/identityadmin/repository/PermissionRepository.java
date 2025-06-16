package io.spring.identityadmin.repository;

import io.spring.identityadmin.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);

    /**
     * [신규 추가] 여러 개의 권한 이름(name)으로 Permission 엔티티 목록을 조회합니다.
     * StudioVisualizerService 에서 사용됩니다.
     */
    List<Permission> findAllByNameIn(Set<String> names);

    /**
     * [오류 수정 및 성능 개선] isDefined = true인 권한을 조회할 때, N+1 문제를 방지하기 위해
     * Fetch Join을 사용하여 연관된 모든 엔티티를 한번의 쿼리로 가져옵니다.
     */
    @Query("SELECT p FROM Permission p " +
            "LEFT JOIN FETCH p.managedResource mr " +
            "WHERE mr.status <> io.spring.identityadmin.domain.entity.ManagedResource.Status.NEEDS_DEFINITION " +
            "AND mr.status <> io.spring.identityadmin.domain.entity.ManagedResource.Status.EXCLUDED")
    List<Permission> findDefinedPermissionsWithDetails();
}