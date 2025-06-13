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
     * [신규 추가] isDefined = true 인, 즉 @Operation으로 정의된 리소스와 연결된
     * 사용자 친화적인 권한들만 조회하는 쿼리입니다.
     */
    @Query("SELECT DISTINCT p FROM Permission p " +
            "JOIN FETCH p.functions f " +
            "JOIN FETCH f.managedResource mr " +
            "WHERE mr.isDefined = true")
    List<Permission> findDefinedPermissions();
}