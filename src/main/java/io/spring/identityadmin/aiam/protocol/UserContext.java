package io.spring.identityadmin.aiam.protocol;

public class UserContext extends IAMContext {
    private UserProfile userProfile;
    private List<String> currentRoles;
    private AccessHistory accessHistory;
    // 사용자 관련 컨텍스트
}
