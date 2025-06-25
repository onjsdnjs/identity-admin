package io.spring.identityadmin.resource.scanner;

import io.spring.identityadmin.domain.entity.ConditionTemplate;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.resource.MethodPatternAnalyzer;
import io.spring.identityadmin.resource.MethodPatternAnalyzer.MethodAnalysisResult;
import io.spring.identityadmin.resource.service.AutoConditionTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 🔄 기존 시스템과 통합된 자동 조건 템플릿 스캐너
 * 
 * ResourceScanner 인터페이스를 구현하여 기존 ResourceRegistryServiceImpl의
 * scanners 리스트에 자동으로 등록되고, refreshAndSynchronizeResources() 호출 시
 * 다른 스캐너들과 함께 실행됩니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoConditionTemplateScanner implements ResourceScanner {

    private final ApplicationContext applicationContext;
    private final MethodPatternAnalyzer methodPatternAnalyzer;
    private final AutoConditionTemplateService autoConditionService;

    @Override
    public List<ManagedResource> scan() {
        log.info("🔍 자동 조건 템플릿 스캔 시작");
        
        try {
            // 1. 기존 MethodResourceScanner와 동일한 방식으로 컨트롤러 메서드 스캔
            List<Method> allMethods = scanControllerMethods();
            log.info("📊 스캔된 컨트롤러 메서드 수: {}", allMethods.size());
            
            // 2. 메서드 패턴 분석
            List<MethodAnalysisResult> analysisResults = methodPatternAnalyzer
                .analyzeMethods(allMethods, "auto_condition_scan");
            
            // 3. 자동 조건 템플릿 생성 (비동기로 실행하여 스캔 성능에 영향 없음)
            generateTemplatesAsync(analysisResults);
            
            // 4. ResourceScanner 계약을 위해 빈 리스트 반환
            // (조건 템플릿은 ManagedResource가 아니므로 별도 관리)
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("❌ 자동 조건 템플릿 스캔 실패", e);
            return new ArrayList<>();
        }
    }

    /**
     * 컨트롤러 메서드들을 스캔합니다 (MethodResourceScanner 로직 재사용)
     */
    private List<Method> scanControllerMethods() {
        List<Method> allMethods = new ArrayList<>();
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
                Class<?> targetClass = org.springframework.aop.support.AopUtils.getTargetClass(bean);

                // 애플리케이션 패키지 내의 컨트롤러만 대상
                if (!targetClass.getPackageName().startsWith("io.spring.identityadmin")) {
                    continue;
                }

                // 컨트롤러 클래스만 대상
                if (!isController(targetClass)) {
                    continue;
                }

                // public 메서드들 수집
                for (Method method : targetClass.getDeclaredMethods()) {
                    if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                        allMethods.add(method);
                    }
                }
            } catch (Exception e) {
                log.trace("빈 스캔 중 오류: {}", beanName);
            }
        }

        return allMethods;
    }

    /**
     * 클래스가 컨트롤러인지 확인
     */
    private boolean isController(Class<?> targetClass) {
        return org.springframework.core.annotation.AnnotationUtils
            .findAnnotation(targetClass, org.springframework.stereotype.Controller.class) != null ||
            org.springframework.core.annotation.AnnotationUtils
            .findAnnotation(targetClass, org.springframework.web.bind.annotation.RestController.class) != null;
    }

    /**
     * 조건 템플릿 생성을 비동기로 실행
     */
    private void generateTemplatesAsync(List<MethodAnalysisResult> analysisResults) {
        // 별도 스레드에서 실행하여 리소스 스캔 성능에 영향 주지 않음
        new Thread(() -> {
            try {
                List<ConditionTemplate> generatedTemplates = autoConditionService
                    .generateTemplatesFromAnalysis(analysisResults);
                
                log.info("✅ 자동 조건 템플릿 생성 완료: {} 개", generatedTemplates.size());
                
                // 패턴별 통계 로그
                logPatternStatistics(analysisResults);
                
            } catch (Exception e) {
                log.error("❌ 자동 조건 템플릿 생성 실패", e);
            }
        }, "AutoConditionTemplateGenerator").start();
    }

    /**
     * 패턴별 통계 로그
     */
    private void logPatternStatistics(List<MethodAnalysisResult> analysisResults) {
        var patternStats = analysisResults.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                r -> r.getDetectedPattern().toString(),
                java.util.stream.Collectors.counting()));
        
        log.info("📈 메서드 패턴 통계: {}", patternStats);
    }
} 