package io.spring.identityadmin.workflow.wizard.service;

/**
 * 3단계 권한 부여 마법사의 전체 워크플로우를 관장하는 핵심 서비스입니다.
 * 사용자의 각 단계별 입력을 처리하고, 컨텍스트를 유지하며, 최종적으로 정책 생성을 오케스트레이션합니다.
 */
public interface PermissionWizardService {

    /**
     * Step 1: 권한을 부여할 주체(사용자, 그룹)를 선택하고 임시 컨텍스트를 생성합니다.
     * @param subjectSelectionDto 선택된 사용자 ID 및 그룹 ID 목록
     * @return 다음 단계로 진행하기 위한 정보와 컨텍스트 ID를 포함한 결과
     */
    WizardStepResult<ResourceSelectionContext> selectSubjects(SubjectSelectionDto subjectSelectionDto);

    /**
     * Step 2: 선택된 주체에게 부여할 리소스(애플리케이션 기능/데이터)를 선택합니다.
     * @param wizardContextId 이전 단계에서 발급된 컨텍스트 ID
     * @param resourceIds 선택된 리소스(ManagedResource 또는 BusinessResource)의 ID 목록
     * @return 다음 단계로 진행하기 위한 정보와 업데이트된 컨텍스트 ID
     */
    WizardStepResult<PermissionLevelContext> selectResources(String wizardContextId, Set<Long> resourceIds);

    /**
     * Step 3: 선택된 리소스에 대해 어떤 수준의 권한(읽기, 쓰기 등)을 부여할지 결정하고 정책 생성을 완료합니다.
     * @param wizardContextId 이전 단계에서 발급된 컨텍스트 ID
     * @param permissionLevelGrants 각 리소스에 대한 권한 수준 매핑 정보
     * @return 성공적으로 생성된 정책의 요약 정보를 포함한 최종 결과
     */
    WizardResult grantPermissions(String wizardContextId, Set<PermissionLevelGrant> permissionLevelGrants);

    /**
     * 현재 마법사 진행 상황을 기반으로 생성될 정책의 결과를 미리 보여줍니다.
     * @param wizardContextId 현재 컨텍스트 ID
     * @return "관리자 그룹에게 '사용자 관리' 기능에 대한 '모든 권한'이 부여됩니다." 와 같은 자연어 요약 DTO
     */
    PermissionPreviewDto previewPermissions(String wizardContextId);
}