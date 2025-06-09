package io.spring.identityadmin.admin.service;

import io.spring.identityadmin.entity.policy.Policy;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public interface PolicyRetrievalPoint {

    @Cacheable(value = "urlPolicies", key = "'allUrlPolicies'")
    List<Policy> findUrlPolicies();

    @CacheEvict(value = "urlPolicies", allEntries = true)
    void clearUrlPoliciesCache();

    /**
     * 특정 메서드에 적용될 정책들을 조회합니다.
     * @param methodIdentifier 조회할 메서드 식별자
     * @return 적용 가능한 정책 목록 (우선순위에 따라 정렬됨)
     */
    @Cacheable(value = "methodPolicies", key = "#methodIdentifier")
    List<Policy> findMethodPolicies(String methodIdentifier);

    /**
     * 메서드 정책 캐시를 모두 무효화합니다.
     */
    @CacheEvict(value = "methodPolicies", allEntries = true)
    void clearMethodPoliciesCache();
}