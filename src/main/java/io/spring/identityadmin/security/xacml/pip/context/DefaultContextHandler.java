package io.spring.identityadmin.security.xacml.pip.context;

import io.spring.identityadmin.domain.entity.Users;
import io.spring.identityadmin.repository.UserRepository;
import io.spring.identityadmin.security.core.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInvocation; // MethodInvocation 임포트
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DefaultContextHandler implements ContextHandler {

    private final UserRepository userRepository;

    @Override
    public AuthorizationContext buildContext(HttpServletRequest request, Object resource) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails = (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails)
                ? (CustomUserDetails) authentication.getPrincipal() : null;

        Users subject = (userDetails != null) ? userDetails.getUsers() : null;

        // [수정] subject가 null이 아닐 경우, DB 에서 최신 역할/그룹 정보를 조회하여 컨텍스트에 추가
        if (subject != null) {
            Users userWithDetails = userRepository.findByIdWithGroupsAndRoles(subject.getId())
                    .orElse(subject);

            // 역할과 그룹 정보를 attributes 맵에 추가
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("userRoles", userWithDetails.getRoleNames());
            attributes.put("userGroups", userWithDetails.getUserGroups());

            // TODO: 리소스 민감도 등급 등 추가 정보 로드 로직
            // attributes.put("resourceSensitivity", getResourceSensitivity(resource));

            return new AuthorizationContext(authentication, subject,
                    new ResourceDetails(resource.toString()),
                    new EnvironmentDetails(request.getRemoteAddr()),
                    attributes);
        }

        // 인증되지 않은 사용자의 경우
        return new AuthorizationContext(authentication, null,
                new ResourceDetails(resource.toString()),
                new EnvironmentDetails(request.getRemoteAddr()),
                new HashMap<>());
    }

    @Override
    public AuthorizationContext create(Authentication authentication, HttpServletRequest request) {
        // 기존 URL 기반 컨텍스트 생성 로직은 그대로 유지
        ResourceDetails resource = new ResourceDetails("URL", request.getRequestURI());
        EnvironmentDetails environment = new EnvironmentDetails(request.getRemoteAddr(), LocalDateTime.now(), request);

        return new AuthorizationContext(authentication, resource, request.getMethod(), environment);
    }

    /**
     * [신규] 메서드 기반 보안을 위한 컨텍스트 생성 로직 구현
     */
    @Override
    public AuthorizationContext create(Authentication authentication, MethodInvocation invocation) {
        Method method = invocation.getMethod();
        String resourceIdentifier = method.getDeclaringClass().getName() + "." + method.getName();

        ResourceDetails resource = new ResourceDetails("METHOD", resourceIdentifier);

        // 메서드 호출 시점에는 HttpServletRequest가 없으므로 IP, 원본 요청 등은 null
        EnvironmentDetails environment = new EnvironmentDetails(null, LocalDateTime.now(), null);

        // 메서드 호출은 'INVOKE'라는 행동으로 정의
        return new AuthorizationContext(authentication, resource, "INVOKE", environment);
    }
}
