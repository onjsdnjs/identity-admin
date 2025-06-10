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
            // 프록시 객체인 경우 원본 클래스를 추출합니다.
            Class<?> beanClass = AopUtils.getTargetClass(bean);

            // 프로젝트 내부의 패키지만을 대상으로 하여 불필요한 Spring 내부 Bean 스캔을 방지합니다.
            if (!beanClass.getPackageName().startsWith("io.spring.identityadmin")) {
                continue;
            }

            // 해당 클래스의 모든 public 메서드를 스캔합니다.
            Method[] methods = beanClass.getDeclaredMethods();
            for (Method method : methods) {
                if (Modifier.isPublic(method.getModifiers())) {

                    // [핵심 수정] 메서드 파라미터 타입을 포함하여 고유 식별자 생성
                    String params = Arrays.stream(method.getParameterTypes())
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining(","));
                    String identifier = String.format("%s.%s(%s)", beanClass.getName(), method.getName(), params);

                    // [핵심 수정] Java 메서드 이름 자체를 기반으로 사용자 친화적 이름 생성
                    String friendlyName = convertCamelCaseToTitleCase(method.getName());

                    // 설명은 전체 메서드 시그니처를 사용하여 명확성을 제공
                    String description = method.toString();

                    resources.add(ManagedResource.builder()
                            .resourceIdentifier(identifier)
                            .resourceType(ManagedResource.ResourceType.METHOD)
                            .friendlyName(friendlyName)
                            .description(description)
                            .serviceOwner(beanClass.getSimpleName())
                            .build());
                }
            }
        }
        log.info("Successfully scanned and discovered {} METHOD resources.", resources.size());
        return resources;
    }

    /**
     * camelCase 문자열을 Title Case (단어마다 대문자)로 변환하는 헬퍼 메서드.
     * 예: "getUserById" -> "Get User By Id"
     */
    private String convertCamelCaseToTitleCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return "";
        }
        String regex = "(?<=[a-z])(?=[A-Z])";
        String result = camelCase.replaceAll(regex, " ");
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }
}
