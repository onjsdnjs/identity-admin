package io.spring.identityadmin.resource;

import io.spring.identityadmin.domain.entity.ManagedResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [오류 수정 및 로직 개선]
 * 사유: 1. 존재하지 않는 isDefined() 빌더 메서드 호출 오류를 제거했습니다.
 *      2. @Protectable 어노테이션이 붙은 리소스는 개발자가 이미 '정의'한 것으로 간주하여,
 *         초기 상태를 'NEEDS_DEFINITION'이 아닌 'PERMISSION_CREATED'로 설정합니다.
 *         이를 통해 관리자는 별도의 '정의' 과정 없이 바로 권한을 역할에 할당할 수 있어
 *         워크플로우가 크게 단축됩니다.
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
                log.trace("Skipping bean '{}' during scan due to initialization issue: {}", beanName, e.getMessage());
                continue;
            }

            Class<?> beanClass = AopUtils.getTargetClass(bean);

            if (!beanClass.getPackageName().startsWith("io.spring.identityadmin")) continue;
            if (beanClass.isAnnotationPresent(Controller.class) || beanClass.isAnnotationPresent(RestController.class)) continue;

            for (Method method : beanClass.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) continue;

                Protectable protectableAnnotation = method.getAnnotation(Protectable.class);
                if (protectableAnnotation == null) {
                    continue;
                }

                String params = Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(","));
                String identifier = String.format("%s.%s(%s)", beanClass.getName(), method.getName(), params);

                String friendlyName = protectableAnnotation.name();
                String description = protectableAnnotation.description();
                String sourceCodeLocation = String.format("%s.java", beanClass.getName().replace('.', '/'));

                resources.add(ManagedResource.builder()
                        .resourceIdentifier(identifier)
                        .resourceType(ManagedResource.ResourceType.METHOD)
                        .friendlyName(friendlyName)
                        .description(description)
                        .serviceOwner(beanClass.getSimpleName())
                        .parameterTypes(params)
                        .returnType(method.getReturnType().getSimpleName())
                        .sourceCodeLocation(sourceCodeLocation)
                        // [로직 개선] @Protectable 리소스는 즉시 권한 할당이 가능한 'PERMISSION_CREATED' 상태로 시작
                        .status(ManagedResource.Status.PERMISSION_CREATED)
                        .build());
            }
        }
        log.info("Successfully scanned and discovered {} protectable METHOD resources.", resources.size());
        return resources;
    }
}