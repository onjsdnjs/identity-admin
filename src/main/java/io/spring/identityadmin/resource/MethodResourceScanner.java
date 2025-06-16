package io.spring.identityadmin.resource;

import io.spring.identityadmin.domain.entity.ManagedResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [오류 수정 및 최종 완성]
 * 사유: 이전 수정에서 잘못된 필터링 로직으로 인해 @Service, @Component 등이 붙은
 *      핵심 비즈니스 Bean들을 스캔에서 누락하는 치명적인 오류가 있었습니다.
 *      모든 Bean을 대상으로 하되, 우리가 관리하고자 하는 패키지 내의 Bean만 스캔하도록
 *      로직을 단순화하고 명확하게 수정하여 메서드 리소스가 정상적으로 DB에 저장되도록 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MethodResourceScanner implements ResourceScanner {

    private final ApplicationContext applicationContext;

    @Override
    public List<ManagedResource> scan() {
        List<ManagedResource> resources = new ArrayList<>();
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception e) {
                log.trace("Skipping bean '{}' during scan: {}", beanName, e.getMessage());
                continue;
            }

            // AOP 프록시 객체가 아닌 실제 타겟 클래스를 가져옴
            Class<?> targetClass = AopUtils.getTargetClass(bean);

            // [핵심 수정] 스캔 대상을 우리 애플리케이션 패키지로 한정하는 것 외에 다른 필터링은 제거
            if (!targetClass.getPackageName().startsWith("io.spring.identityadmin")) {
                continue;
            }

            // [핵심 수정] 웹 컨트롤러는 MvcResourceScanner가 담당하므로 명시적으로 제외
            if (AnnotationUtils.findAnnotation(targetClass, Controller.class) != null ||
                    AnnotationUtils.findAnnotation(targetClass, RestController.class) != null) {
                continue;
            }

            try {
                for (Method method : targetClass.getDeclaredMethods()) {
                    // public 메서드만 스캔
                    if (!Modifier.isPublic(method.getModifiers())) {
                        continue;
                    }

                    // @Protectable 어노테이션이 붙은 메서드만 스캔 대상
                    Protectable protectableAnnotation = AnnotationUtils.findAnnotation(method, Protectable.class);
                    if (protectableAnnotation == null) {
                        continue;
                    }

                    String params = Arrays.stream(method.getParameterTypes())
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining(","));
                    String identifier = String.format("%s.%s(%s)", targetClass.getName(), method.getName(), params);

                    String friendlyName = protectableAnnotation.name();
                    String description = protectableAnnotation.description();
                    String sourceCodeLocation = String.format("%s.java", targetClass.getName().replace('.', '/'));

                    resources.add(ManagedResource.builder()
                            .resourceIdentifier(identifier)
                            .resourceType(ManagedResource.ResourceType.METHOD)
                            .friendlyName(friendlyName)
                            .description(description)
                            .serviceOwner(targetClass.getSimpleName())
                            .parameterTypes(params)
                            .returnType(method.getReturnType().getSimpleName())
                            .sourceCodeLocation(sourceCodeLocation)
                            .status(ManagedResource.Status.PERMISSION_CREATED)
                            .build());
                }
            } catch (Exception e) {
                log.warn("Could not scan methods on bean '{}' of type {}: {}", beanName, targetClass.getSimpleName(), e.getMessage());
            }
        }
        log.info("Scanned and discovered {} protectable METHOD resources.", resources.size());
        return resources;
    }
}