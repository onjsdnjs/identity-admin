package io.spring.identityadmin.security.xacml.pip.attribute;

import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.repository.UserRepository;
import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DatabaseAttributePIP implements AttributeInformationPoint {

    private final UserRepository userRepository;

    @Override
    public Map<String, Object> getAttributes(AuthorizationContext context) {
        Map<String, Object> attributes = new HashMap<>();

        if (context.subject() != null) {
            userRepository.findByUsernameWithGroupsRolesAndPermissions(((UserDto)(context.subject().getPrincipal())).getUsername()).ifPresent(user -> {
                // SpEL 에서 #userAge로 접근 가능하도록 속성 추가
                attributes.put("username", user.getUsername());
                // 필요시 다른 사용자 속성(부서, 직책 등) 추가
            });
        }
        return attributes;
    }
}
