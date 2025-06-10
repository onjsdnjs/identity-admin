package io.spring.identityadmin.iamw;

import io.spring.identityadmin.entity.ManagedResource;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * [최종 수정] @SecuredResource 의존성을 완전히 제거하고, 모든 @RequestMapping 계열 엔드포인트를 스캔합니다.
 * 리소스의 이름과 설명은 API 문서 표준인 @Operation 어노테이션을 활용합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MvcResourceScanner implements ResourceScanner {

    private final RequestMappingHandlerMapping handlerMapping;

    @Override
    public List<ManagedResource> scan() {
        List<ManagedResource> resources = new ArrayList<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            // 프로젝트 내부의 컨트롤러만 대상으로 필터링
            if (!handlerMethod.getBeanType().getPackageName().startsWith("io.spring.identityadmin")) {
                continue;
            }

            // URL 패턴 추출
            String urlPattern = mappingInfo.getPatternsCondition().getPatterns().stream()
                    .findFirst().orElse(null);
            if (urlPattern == null) {
                continue;
            }

            // HTTP 메서드 추출
            String httpMethod = mappingInfo.getMethodsCondition().getMethods().stream()
                    .findFirst().map(Enum::name).orElse("ANY");

            // 기술적 식별자 생성 (HTTP 메서드와 URL 조합으로 고유성 확보)
            String resourceIdentifier = httpMethod + ":" + urlPattern;

            // @Operation 어노테이션에서 이름과 설명 추출
            Operation operation = handlerMethod.getMethodAnnotation(Operation.class);
            String friendlyName;
            String description;

            if (operation != null && !operation.summary().isEmpty()) {
                friendlyName = operation.summary();
                description = operation.description();
            } else {
                // @Operation이 없는 경우, 기본값 생성
                friendlyName = httpMethod + " " + urlPattern;
                description = "Class: " + handlerMethod.getBeanType().getSimpleName() + ", Method: " + handlerMethod.getMethod().getName();
            }

            resources.add(ManagedResource.builder()
                    .resourceIdentifier(resourceIdentifier)
                    .resourceType(ManagedResource.ResourceType.URL)
                    .friendlyName(friendlyName)
                    .description(description)
                    .serviceOwner(handlerMethod.getBeanType().getSimpleName())
                    .build());

            log.debug("Discovered URL Resource: {}", resourceIdentifier);
        }
        return resources;
    }
}