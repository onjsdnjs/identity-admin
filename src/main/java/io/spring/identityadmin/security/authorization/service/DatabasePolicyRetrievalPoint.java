package io.spring.identityadmin.security.authorization.service;

import io.spring.identityadmin.admin.repository.PolicyRepository;
import io.spring.identityadmin.entity.policy.Policy;
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
        log.info("URL policies cache will be evicted by annotation.");
    }

    @Override
    public List<Policy> findMethodPolicies(String methodIdentifier) {
        log.debug("Fetching method policies for identifier: {}", methodIdentifier);
        return policyRepository.findByMethodIdentifier(methodIdentifier);
    }

    @Override
    public void clearMethodPoliciesCache() {
        log.info("Method policies cache will be evicted by annotation.");
    }
}
