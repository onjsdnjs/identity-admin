package io.spring.identityadmin.iamw;

import io.spring.identityadmin.entity.ManagedResource;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
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

            // [핵심 수정] getPatternsCondition()이 null을 반환하는 경우를 방어합니다.
            PathPatternsRequestCondition pathPatternsCondition = mappingInfo.getPathPatternsCondition();
            if (pathPatternsCondition == null) {
                continue; // URL 패턴이 없는 매핑은 건너뜁니다.
            }

            Set<PathPattern> urlPatterns = pathPatternsCondition.getPatterns();
            if (urlPatterns.isEmpty()) {
                continue;
            }

            // 프로젝트 내부의 컨트롤러만 대상으로 필터링
            if (!handlerMethod.getBeanType().getPackageName().startsWith("io.spring.identityadmin")) {
                continue;
            }

            // 대표 URL 패턴 하나만 사용
            final String urlPattern = urlPatterns.stream().findFirst().get().getPatternString();

            // HTTP 메서드 추출
            final String httpMethod = mappingInfo.getMethodsCondition().getMethods().stream()
                    .findFirst().map(Enum::name).orElse("ANY");

            final String resourceIdentifier = httpMethod + ":" + urlPattern;
            final String methodName = handlerMethod.getMethod().getName();
            final String friendlyName = convertCamelCaseToTitleCase(methodName);
            final String description = "Mapped to: " + handlerMethod.toString();

            resources.add(ManagedResource.builder()
                    .resourceIdentifier(resourceIdentifier)
                    .resourceType(ManagedResource.ResourceType.URL)
                    .friendlyName(friendlyName)
                    .description(description)
                    .serviceOwner(handlerMethod.getBeanType().getSimpleName())
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