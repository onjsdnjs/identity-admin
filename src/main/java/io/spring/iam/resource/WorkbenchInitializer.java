package io.spring.iam.resource;

import io.spring.iam.domain.entity.policy.Policy;
import io.spring.iam.repository.PermissionRepository;
import io.spring.iam.repository.PolicyRepository;
import io.spring.iam.resource.service.ResourceRegistryService;
import io.spring.iam.security.xacml.pap.service.PolicyEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 애플리케이션이 완전히 시작된 후, '통합 워크벤치'에 필요한 초기화 작업을 수행합니다.
 * 이 클래스는 리소스 자동 스캔 및 DB 등록을 보장하는 핵심적인 역할을 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkbenchInitializer implements ApplicationRunner {

    private final ResourceRegistryService resourceRegistryService;
    private final PolicyRepository policyRepository;
    private final PolicyEnrichmentService policyEnrichmentService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("IAM Command Center: Starting resource synchronization on application startup...");
        try {
            // 1. 리소스 동기화 시작 (비동기)
            resourceRegistryService.refreshAndSynchronizeResources();
            log.info("IAM Command Center: Resource synchronization started (async).");

            // 2. 정책 설명 강화
            List<Policy> policiesToUpdate = policyRepository.findByFriendlyDescriptionIsNull();

            if (policiesToUpdate.isEmpty()) {
                log.info("All policies have friendly descriptions. No updates needed.");
                return;
            }

            log.info("Found {} policies to enrich. Starting process...", policiesToUpdate.size());
            for (Policy policy : policiesToUpdate) {
                policyEnrichmentService.enrichPolicyWithFriendlyDescription(policy);
                policyRepository.save(policy);
            }
            log.info("Policy enrichment process completed.");
        } catch (Exception e) {
            log.error("IAM Command Center: Failed to initialize resources on startup.", e);
        }
    }
}
