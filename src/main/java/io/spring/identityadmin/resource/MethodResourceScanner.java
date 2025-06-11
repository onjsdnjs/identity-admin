package io.spring.identityadmin.resource;

import io.spring.identityadmin.domain.entity.ManagedResource;
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

            if (!beanClass.getPackageName().startsWith("io.spring.identityadmin")) {
                continue;
            }

            Method[] methods = beanClass.getDeclaredMethods();
            for (Method method : methods) {
                if (Modifier.isPublic(method.getModifiers())) {
                    String params = Arrays.stream(method.getParameterTypes())
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining(","));
                    String identifier = String.format("%s.%s(%s)", beanClass.getName(), method.getName(), params);
                    String friendlyName = convertCamelCaseToTitleCase(method.getName());
                    String returnType = method.getReturnType().getSimpleName();

                    resources.add(ManagedResource.builder()
                            .resourceIdentifier(identifier)
                            .resourceType(ManagedResource.ResourceType.METHOD)
                            .friendlyName(friendlyName)
                            .description(method.toString()) // 상세 설명으로 전체 시그니처 제공
                            .serviceOwner(beanClass.getSimpleName())
                            .parameterTypes(params)
                            .returnType(returnType)
                            .isManaged(true) // 기본은 관리 대상으로 설정
                            .build());
                }
            }
        }
        return resources;
    }

    private String convertCamelCaseToTitleCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) return "";
        String regex = "(?<=[a-z])(?=[A-Z])";
        String result = camelCase.replaceAll(regex, " ");
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }
}