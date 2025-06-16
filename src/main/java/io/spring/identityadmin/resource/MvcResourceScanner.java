package io.spring.identityadmin.resource;

import io.spring.identityadmin.domain.entity.ManagedResource;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MvcResourceScanner implements ResourceScanner {

    private final RequestMappingHandlerMapping handlerMapping;

    @Override
    public List<ManagedResource> scan() {
        final List<ManagedResource> resources = new ArrayList<>();
        final Map<RequestMappingInfo, HandlerMethod> handlerMethods = this.handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            final RequestMappingInfo mappingInfo = entry.getKey();
            final HandlerMethod handlerMethod = entry.getValue();
            final Class<?> beanType = handlerMethod.getBeanType();

            // 필터링 규칙 1: io.spring.identityadmin 패키지 내의 컨트롤러로 한정
            if (!beanType.getPackageName().startsWith("io.spring.identityadmin")) {
                continue;
            }

            // 필터링 규칙 2: @Controller 또는 @RestController 어노테이션이 붙은 클래스만 대상
            if (!beanType.isAnnotationPresent(Controller.class) && !beanType.isAnnotationPresent(RestController.class)) {
                continue;
            }

            // RequestMapping 정보가 없으면 스킵 (URL 기반 스캔의 핵심)
            PathPatternsRequestCondition pathPatternsCondition = mappingInfo.getPathPatternsCondition();
            if (pathPatternsCondition == null || pathPatternsCondition.getPatterns().isEmpty()) {
                continue;
            }

            final String urlPattern = pathPatternsCondition.getPatterns().stream().findFirst().get().getPatternString();
            final String httpMethodStr = mappingInfo.getMethodsCondition().getMethods().stream()
                    .findFirst().map(Enum::name).orElse("ANY");

            ManagedResource.HttpMethod httpMethod;
            try {
                httpMethod = ManagedResource.HttpMethod.valueOf(httpMethodStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                httpMethod = ManagedResource.HttpMethod.ANY;
            }

            Operation operation = handlerMethod.getMethodAnnotation(Operation.class);
            String friendlyName;
            String description;
            boolean isDefinedByAnnotation;

            if (operation != null && !operation.summary().isEmpty()) {
                friendlyName = operation.summary();
                description = operation.description();
                isDefinedByAnnotation = true;
            } else {
                friendlyName = handlerMethod.getMethod().getName();
                description = "개발자는 코드에 @Operation 어노테이션을 추가하여 이 리소스의 비즈니스 용도를 명시해야 합니다.";
                isDefinedByAnnotation = false;
            }

            resources.add(ManagedResource.builder()
                    .resourceIdentifier(urlPattern)
                    .httpMethod(httpMethod)
                    .resourceType(ManagedResource.ResourceType.URL)
                    .friendlyName(friendlyName)
                    .description(description)
                    .serviceOwner(handlerMethod.getBeanType().getSimpleName())
                    .isManaged(false) // [의미 변경] 워크벤치에서 관리자가 명시적으로 관리하기 전까지는 false
                    .isDefined(isDefinedByAnnotation) // [의미 변경] @Operation으로 정의되었는지 여부
                    .build());
        }

        log.info("Successfully scanned and discovered {} URL resources.", resources.size());
        return resources;
    }
}