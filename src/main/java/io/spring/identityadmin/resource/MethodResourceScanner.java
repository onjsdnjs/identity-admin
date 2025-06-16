package io.spring.identityadmin.resource;

import io.spring.identityadmin.domain.entity.ManagedResource;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
            Object bean = applicationContext.getBean(beanName);
            Class<?> beanClass = AopUtils.getTargetClass(bean);

            if (!beanClass.getPackageName().startsWith("io.spring.identityadmin")) continue;

            for (Method method : beanClass.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) continue;

                // [핵심 변경] 보안 어노테이션이 있는 메서드만 필터링
                if (!isSecureMethod(method)) {
                    continue;
                }

                String params = Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(","));
                String identifier = String.format("%s.%s", beanClass.getName(), method.getName());

                Operation operation = method.getAnnotation(Operation.class);
                String friendlyName;
                String description;
                boolean isDefinedByAnnotation;

                if (operation != null && !operation.summary().isEmpty()) {
                    friendlyName = operation.summary();
                    description = operation.description();
                    isDefinedByAnnotation = true;
                } else {
                    friendlyName = method.getName();
                    description = "개발자는 코드에 @Operation 어노테이션을 추가하여 이 메서드의 비즈니스 용도를 명시해야 합니다.";
                    isDefinedByAnnotation = false;
                }

                resources.add(ManagedResource.builder()
                        .resourceIdentifier(identifier)
                        .resourceType(ManagedResource.ResourceType.METHOD)
                        .friendlyName(friendlyName)
                        .description(description)
                        .serviceOwner(beanClass.getSimpleName())
                        .parameterTypes(params)
                        .returnType(method.getReturnType().getSimpleName())
                        .isManaged(false) // 관리자가 명시적으로 관리하기 전까지는 false
                        .isDefined(isDefinedByAnnotation) // @Operation 유무에 따라 결정
                        .build());
            }
        }
        log.info("Successfully scanned and discovered {} METHOD resources.", resources.size());
        return resources;
    }

    /**
     * [신규 헬퍼 메서드]
     * 해당 메서드에 보안 관련 어노테이션이 있는지 확인합니다.
     * @param method 검사할 메서드
     * @return 보안 어노테이션이 있으면 true
     */
    private boolean isSecureMethod(Method method) {
        return method.isAnnotationPresent(PreAuthorize.class) ||
                method.isAnnotationPresent(PostAuthorize.class) ||
                method.isAnnotationPresent(PreFilter.class) ||
                method.isAnnotationPresent(PostFilter.class);
    }
}