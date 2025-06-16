package io.spring.identityadmin.resource;

import io.spring.identityadmin.domain.entity.ManagedResource;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        final List<ManagedResource> resources = new ArrayList<>();
        final Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            final RequestMappingInfo mappingInfo = entry.getKey();
            final HandlerMethod handlerMethod = entry.getValue();

            PathPatternsRequestCondition pathPatternsCondition = mappingInfo.getPathPatternsCondition();
            if (pathPatternsCondition == null || pathPatternsCondition.getPatterns().isEmpty()) continue;
            if (!handlerMethod.getBeanType().getPackageName().startsWith("io.spring.identityadmin")) continue;

            final String urlPattern = pathPatternsCondition.getPatterns().stream().findFirst().get().getPatternString();
            final String httpMethod = mappingInfo.getMethodsCondition().getMethods().stream()
                    .findFirst().map(Enum::name).orElse("ANY");

            Operation operation = handlerMethod.getMethodAnnotation(Operation.class);
            String friendlyName;
            String description;
            boolean isDefined;

            if (operation != null && !operation.summary().isEmpty()) {
                friendlyName = operation.summary();
                description = operation.description();
                isDefined = true;
            } else {
                friendlyName = handlerMethod.getMethod().getName(); // 어노테이션 없으면 그냥 메서드 이름 사용
                description = "개발자는 코드에 @Operation 어노테이션을 추가하여 이 리소스의 비즈니스 용도를 명시해야 합니다.";
                isDefined = false;
            }

            resources.add(ManagedResource.builder()
                    .resourceIdentifier(urlPattern)
                    .httpMethod(ManagedResource.HttpMethod.valueOf(httpMethod.toUpperCase()))
                    .resourceType(ManagedResource.ResourceType.URL)
                    .friendlyName(friendlyName)
                    .description(description)
                    .serviceOwner(handlerMethod.getBeanType().getSimpleName())
                    .isManaged(operation != null)
                    .isDefined(isDefined)
                    .build());
        }

        log.info("Successfully scanned and discovered {} URL resources.", resources.size());
        return resources;
    }

    /**
     * camelCase 문자열을 Title Case (단어마다 대문자)로 변환하는 헬퍼 메서드.
     * 예: "getUserById" -> "Get User By Id"
     * @param camelCase 변환할 camelCase 문자열
     * @return 변환된 Title Case 문자열
     */
    private String convertCamelCaseToTitleCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return "";
        }
        // 대문자 앞에 공백을 추가하는 정규식
        String regex = "(?<=[a-z])(?=[A-Z])";
        String result = camelCase.replaceAll(regex, " ");
        // 첫 글자를 대문자로 변경
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }
}