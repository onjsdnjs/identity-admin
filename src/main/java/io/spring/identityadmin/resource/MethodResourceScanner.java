package io.spring.identityadmin.resource;

import io.spring.identityadmin.domain.entity.ManagedResource;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
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

                String params = Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(","));
                String identifier = String.format("%s.%s", beanClass.getName(), method.getName());

                Operation operation = method.getAnnotation(Operation.class);
                String friendlyName;
                String description;
                boolean isDefined;

                // [최종 수정] @Operation 존재 여부로 isDefined 필드를 명확하게 설정
                if (operation != null && !operation.summary().isEmpty()) {
                    friendlyName = operation.summary();
                    description = operation.description();
                    isDefined = true;
                } else {
                    friendlyName = method.getName();
                    description = "개발자는 코드에 @Operation 어노테이션을 추가하여 이 메서드의 비즈니스 용도를 명시해야 합니다.";
                    isDefined = false;
                }

                resources.add(ManagedResource.builder()
                        .resourceIdentifier(identifier)
                        .resourceType(ManagedResource.ResourceType.METHOD)
                        .friendlyName(friendlyName)
                        .description(description)
                        .serviceOwner(beanClass.getSimpleName())
                        .parameterTypes(params)
                        .returnType(method.getReturnType().getSimpleName())
                        .isManaged(operation != null)
                        .isDefined(isDefined)
                        .build());
            }
        }
        log.info("Successfully scanned and discovered {} METHOD resources.", resources.size());
        return resources;
    }

    private String convertCamelCaseToTitleCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) return "";
        String regex = "(?<=[a-z])(?=[A-Z])";
        String result = camelCase.replaceAll(regex, " ");
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }
}