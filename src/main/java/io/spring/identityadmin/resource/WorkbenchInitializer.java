package io.spring.identityadmin.resource;

import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.resource.service.ResourceRegistryService;
import io.spring.identityadmin.resource.service.AutoConditionTemplateService;
import io.spring.identityadmin.security.xacml.pap.service.PolicyEnrichmentService;
import io.spring.identityadmin.repository.PolicyRepository;
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
    private final AutoConditionTemplateService autoConditionTemplateService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("IAM Command Center: Starting resource synchronization on application startup...");
        try {
            resourceRegistryService.refreshAndSynchronizeResources();
            log.info("IAM Command Center: Resource synchronization completed successfully.");
            
            // 🚀 개선: 애플리케이션 시작 시 ManagedResource 기반 조건 템플릿 자동 생성
            log.info("🎯 Starting ManagedResource-based condition template generation...");
            autoConditionTemplateService.generateManagedResourceBasedTemplates();
            log.info("✅ ManagedResource-based condition template generation completed.");
            
            // 🚀 개선: 애플리케이션 시작 시 Permission 기반 조건 템플릿 자동 생성
            log.info("🎯 Starting Permission-based condition template generation...");
            autoConditionTemplateService.generatePermissionBasedTemplates();
            log.info("✅ Permission-based condition template generation completed.");
            log.info("Checking for policies without friendly descriptions...");

            // 설명이 없는 정책들만 조회
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
