package io.spring.identityadmin.aiam.protocol;

public class PolicyContext extends IAMContext {
    private List<String> availableRoles;
    private List<String> availablePermissions;
    private List<String> availableConditions;
    // 정책 생성에 필요한 컨텍스트
}