package io.spring.identityadmin.iamw;

import io.spring.identityadmin.entity.ManagedResource;
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

/**
 * @Service, @Component 등 모든 Spring Bean의 Public 메서드를 스캔하여
 * 'METHOD' 타입 리소스로 등록합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
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
                    //메서드 파라미터 타입을 식별자에 포함하여 고유성을 보장
                    String params = Arrays.stream(method.getParameterTypes())
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining(","));
                    String identifier = String.format("%s.%s(%s)", beanClass.getName(), method.getName(), params);

                    Operation operation = method.getAnnotation(Operation.class);
                    String friendlyName = (operation != null && !operation.summary().isEmpty()) ?
                            operation.summary() : beanClass.getSimpleName() + "." + method.getName();

                    resources.add(ManagedResource.builder()
                            .resourceIdentifier(identifier)
                            .resourceType(ManagedResource.ResourceType.METHOD)
                            .friendlyName(friendlyName)
                            .description(operation != null ? operation.description() : "")
                            .serviceOwner(beanClass.getSimpleName())
                            .build());

                    log.debug("Discovered METHOD Resource: {}", identifier);
                }
            }
        }
        return resources;
    }
}
