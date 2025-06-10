package io.spring.identityadmin.iamw;

import io.spring.identityadmin.entity.ManagedResource;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * @Service, @Component 등 모든 Spring Bean의 Public 메서드를 스캔하여
 * 'METHOD' 타입 리소스로 등록합니다.
 */
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
            Class<?> beanClass = AopUtils.getTargetClass(bean); // 프록시 객체인 경우 원본 클래스 추출

            // 프로젝트 패키지 내의 빈만 대상으로 함
            if (!beanClass.getPackageName().startsWith("io.spring.identityadmin")) {
                continue;
            }

            Method[] methods = beanClass.getDeclaredMethods();
            for (Method method : methods) {
                // Public 메서드만 스캔 대상으로 함
                if (Modifier.isPublic(method.getModifiers())) {
                    Operation operation = method.getAnnotation(Operation.class);
                    String friendlyName = (operation != null && !operation.summary().isEmpty()) ?
                            operation.summary() : method.getName();
                    String identifier = beanClass.getName() + "." + method.getName();

                    resources.add(ManagedResource.builder()
                            .resourceIdentifier(identifier)
                            .resourceType(ManagedResource.ResourceType.METHOD)
                            .friendlyName(friendlyName)
                            .serviceOwner(beanClass.getSimpleName())
                            .build());
                }
            }
        }
        return resources;
    }
}
