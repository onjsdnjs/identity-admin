package io.spring.identityadmin.repository;

import io.spring.identityadmin.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    /**
     * [신규 추가] 특정 사용자의 최근 감사 로그 5개를 조회합니다.
     */
    List<AuditLog> findTop5ByPrincipalNameOrderByIdDesc(String principalName);
}