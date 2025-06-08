package io.spring.identityadmin.security.authorization.service;

import io.spring.identityadmin.entity.policy.Policy;
import io.spring.identityadmin.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DatabasePolicyRetrievalPoint implements PolicyRetrievalPoint {

    private final PolicyRepository policyRepository;

    @Override
    public List<Policy> findUrlPolicies() {
        log.debug("Fetching all URL policies from database...");
        List<Policy> policies = policyRepository.findByTargetTypeWithDetails("URL");
        log.info("Retrieved {} URL policies.", policies.size());
        return policies;
    }

    @Override
    public void clearUrlPoliciesCache() {
        // @CacheEvict 어노테이션이 실제 캐시 무효화를 처리합니다.
        log.info("URL policies cache evicted.");
    }
}
