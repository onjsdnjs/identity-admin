package io.spring.identityadmin.iamw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션이 완전히 시작된 후, '통합 워크벤치'에 필요한 초기화 작업을 수행합니다.
 * 이 클래스는 리소스 자동 스캔 및 DB 등록을 보장하는 핵심적인 역할을 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkbenchInitializer implements ApplicationRunner {

    private final ResourceRegistryService resourceRegistryService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("IAM Command Center: Starting resource synchronization on application startup...");
        try {
            resourceRegistryService.refreshResources();
            log.info("IAM Command Center: Resource synchronization completed successfully.");
        } catch (Exception e) {
            log.error("IAM Command Center: Failed to initialize resources on startup.", e);
        }
    }
}
