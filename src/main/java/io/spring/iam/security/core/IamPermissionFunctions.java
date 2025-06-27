package io.spring.iam.security.core;

import io.spring.iam.admin.iam.service.impl.DocumentService;
import io.spring.iam.domain.entity.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("iamFunctions") // SpEL 에서 @iamFunctions 이름으로 이 Bean을 참조합니다.
@RequiredArgsConstructor
public class IamPermissionFunctions {

    private final DocumentService documentService;

    /**
     * 대상 객체의 소유자가 현재 인증된 사용자인지 확인하는 SpEL 함수.
     * @param target SpEL의 #targetObject 변수. @PostAuthorize에서 사용됩니다.
     * @param authentication SpEL의 #authentication 변수.
     * @return 소유자 여부
     */
    public boolean isOwner(Object target, Authentication authentication) {
        if (target instanceof Document document) {
            return document.getOwnerUsername().equals(authentication.getName());
        }
        // ... 다른 도메인 객체에 대한 소유자 확인 로직 ...
        return false;
    }
}