package io.spring.identityadmin.repository;

import io.spring.identityadmin.domain.entity.policy.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByName(String name);

    @Query("SELECT DISTINCT p FROM Policy p " +
            "LEFT JOIN FETCH p.targets t " +
            "LEFT JOIN FETCH p.rules r " +
            "LEFT JOIN FETCH r.conditions c")
    List<Policy> findAllWithDetails();

    @Query("SELECT DISTINCT p FROM Policy p " +
            "LEFT JOIN FETCH p.targets t " +
            "LEFT JOIN FETCH p.rules r " +
            "LEFT JOIN FETCH r.conditions c " +
            "WHERE t.targetType = :targetType " +
            "ORDER BY p.priority ASC")
    List<Policy> findByTargetTypeWithDetails(@Param("targetType") String targetType);

    @Query("SELECT p FROM Policy p JOIN p.targets t " +
            "LEFT JOIN FETCH p.rules r " +
            "LEFT JOIN FETCH r.conditions c " +
            "WHERE t.targetType = 'METHOD' AND t.targetIdentifier = :methodIdentifier " +
            "ORDER BY p.priority ASC")
    List<Policy> findByMethodIdentifier(@Param("methodIdentifier") String methodIdentifier);

    @Query("SELECT p FROM Policy p " +
            "LEFT JOIN FETCH p.targets t " +
            "LEFT JOIN FETCH p.rules r " +
            "LEFT JOIN FETCH r.conditions c " +
            "WHERE p.id = :id")
    Optional<Policy> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT p FROM Policy p JOIN FETCH p.targets t WHERE t.targetType = 'URL'")
    List<Policy> findAllUrlPoliciesWithDetails();

    /**
     * [신규] ID를 기준으로 내림차순 정렬하여 최근 5개의 정책을 조회합니다.
     * 감사 로그(Audit Log)가 구현되기 전까지 대시보드의 '최근 활동'에 사용됩니다.
     * @return 최근 생성된 Policy 5개 리스트
     */
    @Query("SELECT DISTINCT p FROM Policy p " +
            "LEFT JOIN FETCH p.rules r " +
            "LEFT JOIN FETCH r.conditions " +
            "ORDER BY p.id DESC")
    List<Policy> findTop5ByOrderByIdDesc();

    @Query("SELECT DISTINCT p FROM Policy p " +
            "LEFT JOIN FETCH p.rules r " +
            "LEFT JOIN FETCH r.conditions " +
            "WHERE p.friendlyDescription IS NULL")
    List<Policy> findByFriendlyDescriptionIsNull();

    /**
     * Ant-style 경로 매칭을 지원하는 편의 메서드.
     * DB에서 모든 URL 정책을 가져온 후, 메모리에서 AntPathMatcher를 사용해 필터링합니다.
     * @param requestUrl 매칭할 요청 URL (e.g., /admin/users/1)
     * @return 매칭되는 모든 정책 리스트
     */
    default List<Policy> findPoliciesMatchingUrl(String requestUrl) {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        return findAllUrlPoliciesWithDetails().stream()
                .filter(policy -> policy.getTargets().stream()
                        .anyMatch(target -> pathMatcher.match(target.getTargetIdentifier(), requestUrl)))
                .toList();
    }
}