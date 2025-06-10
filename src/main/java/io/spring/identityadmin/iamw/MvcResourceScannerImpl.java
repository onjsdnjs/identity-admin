package io.spring.identityadmin.iamw;

// package io.spring.identityadmin.workbench.infra;

import io.spring.identityadmin.annotation.SecuredResource;
import io.spring.identityadmin.entity.ManagedResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MvcResourceScannerImpl implements ResourceScanner {

    private final RequestMappingHandlerMapping handlerMapping;

    @Override
    public List<ManagedResource> scan() {
        List<ManagedResource> resources = new ArrayList<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();
            SecuredResource securedResource = handlerMethod.getMethodAnnotation(SecuredResource.class);

            if (securedResource != null) {
                // URL 패턴 문자열을 가져옴 (첫 번째 패턴만 사용, 필요시 확장)
                String urlPattern = entry.getKey().getPatternsCondition().getPatterns().iterator().next();

                resources.add(ManagedResource.builder()
                        .resourceIdentifier(urlPattern)
                        .resourceType(ManagedResource.ResourceType.URL)
                        .friendlyName(securedResource.name())
                        .description(securedResource.description())
                        .serviceOwner(handlerMethod.getBeanType().getSimpleName())
                        .build());
            }
        }
        return resources;
    }
}
