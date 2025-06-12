package io.spring.identityadmin.admin.support.context.service;

/**
 * 여러 페이지에 걸친 사용자의 작업(예: 권한 부여 마법사) 컨텍스트를 유지하고 개인화된 경험을 제공합니다.
 */
public interface UserContextService {
    /**
     * 권한 부여 마법사의 진행 단계를 세션 또는 임시 저장소에 저장하여 사용자가 나중에 이어서 작업할 수 있도록 합니다.
     * @param userSessionId 사용자 세션 식별자
     * @param wizardContext 저장할 마법사 컨텍스트
     */
    void saveWizardProgress(String userSessionId, WizardContext wizardContext);

    /**
     * 특정 사용자의 최근 관리 활동(예: 정책 생성, 사용자 추가) 기록을 조회합니다.
     * @param userId 사용자 ID
     * @return 최근 활동 DTO 목록
     */
    List<RecentActivityDto> getRecentActivities(Long userId);
}