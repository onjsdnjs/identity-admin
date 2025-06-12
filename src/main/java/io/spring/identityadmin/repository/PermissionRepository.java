package io.spring.identityadmin.repository;

import io.spring.identityadmin.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);

    /**
     * [신규 추가] 여러 개의 권한 이름(name)으로 Permission 엔티티 목록을 조회합니다.
     * StudioVisualizerService에서 사용됩니다.
     */
    List<Permission> findAllByNameIn(Set<String> names);
}