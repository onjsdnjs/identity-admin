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
            if (beanClass.isAnnotationPresent(Controller.class) || beanClass.isAnnotationPresent(RestController.class)) continue;

            for (Method method : beanClass.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) continue;

                Protectable protectableAnnotation = method.getAnnotation(Protectable.class);
                if (protectableAnnotation == null) {
                    continue;
                }

                String params = Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(","));
                String identifier = String.format("%s.%s", beanClass.getName(), method.getName());

                // [핵심 변경] @Protectable 어노테이션의 값을 초기 정보로 사용
                String friendlyName = protectableAnnotation.name();
                String description = protectableAnnotation.description();

                String sourceCodeLocation = String.format("%s.java", beanClass.getName().replace('.', '/'));

                resources.add(ManagedResource.builder()
                        .resourceIdentifier(identifier)
                        .resourceType(ManagedResource.ResourceType.METHOD)
                        .friendlyName(friendlyName) // @Protectable의 name
                        .description(description)     // @Protectable의 description
                        .serviceOwner(beanClass.getSimpleName())
                        .parameterTypes(params)
                        .returnType(method.getReturnType().getSimpleName())
                        .sourceCodeLocation(sourceCodeLocation)
                        .isManaged(false)
                        .isDefined(true) // @Protectable이 붙은 순간, 개발자에 의해 1차 정의된 것으로 간주
                        .build());
            }
        }
        log.info("Successfully scanned and discovered {} protectable METHOD resources.", resources.size());
        return resources;
    }
}