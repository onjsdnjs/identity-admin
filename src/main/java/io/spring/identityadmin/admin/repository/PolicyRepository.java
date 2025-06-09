package io.spring.identityadmin.admin.repository;

import io.spring.identityadmin.entity.policy.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    @Query("SELECT DISTINCT p FROM Policy p " +
            "LEFT JOIN FETCH p.targets t " +
            "LEFT JOIN FETCH p.rules r " +
            "LEFT JOIN FETCH r.conditions c " +
            "WHERE t.targetType = :targetType " +
            "ORDER BY p.priority ASC")
    List<Policy> findByTargetTypeWithDetails(@Param("targetType") String targetType);

    /**
     * 특정 메서드 식별자에 대한 정책을 우선순위 순으로 조회합니다.
     * @param methodIdentifier 조회할 메서드 식별자 (예: com.example.service.UserService.deleteUser)
     * @return 일치하는 정책 목록
     */
    @Query("SELECT p FROM Policy p JOIN p.targets t " +
            "WHERE t.targetType = 'METHOD' AND t.targetIdentifier = :methodIdentifier " +
            "ORDER BY p.priority ASC")
    List<Policy> findByMethodIdentifier(@Param("methodIdentifier") String methodIdentifier);
}