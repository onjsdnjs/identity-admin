package io.spring.identityadmin.workflow.orchestrator.service;

/**
 * 권한 부여, 정책 생성 등 여러 서비스에 걸친 복잡한 비즈니스 워크플로우를 조정(Orchestrate)하는 최상위 서비스입니다.
 */
public interface WorkflowOrchestrator {
    /**
     * '권한 부여 마법사' 전체 워크플로우를 시작부터 끝까지 실행합니다.
     * 내부적으로 PermissionWizardService, PolicyBuilderService 등을 순차적으로 호출합니다.
     * @param request 권한 부여 요청에 필요한 모든 초기 데이터
     * @return 워크플로우 실행 결과 (성공, 실패, 생성된 정책 ID 등)
     */
    WorkflowResult executePermissionGrantWorkflow(PermissionGrantRequest request);

    /**
     * 워크플로우의 현재 진행 상태를 조회합니다.
     * @param workflowId 조회할 워크플로우의 고유 ID
     * @return 현재 단계, 진행률 등의 상태 정보
     */
    WorkflowStatus getWorkflowStatus(String workflowId);
}

