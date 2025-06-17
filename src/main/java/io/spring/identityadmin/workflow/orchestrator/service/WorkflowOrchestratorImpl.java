package io.spring.identityadmin.workflow.orchestrator.service;

import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.workflow.orchestrator.dto.WorkflowRequest;
import io.spring.identityadmin.workflow.orchestrator.dto.WorkflowResult;
import io.spring.identityadmin.workflow.wizard.service.PermissionWizardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowOrchestratorImpl implements WorkflowOrchestrator {

    private final PermissionWizardService permissionWizardService;

    /**
     * [최종 구현] 실제 PermissionWizardService의 메서드를 순차적으로 호출하여
     * 전체 권한 부여 워크플로우를 조정합니다.
     * 이 메서드는 단일 트랜잭션으로 처리되어 과정의 원자성을 보장합니다.
     */
    @Override
    @Transactional
    public WorkflowResult executePermissionGrantWorkflow(WorkflowRequest request) {
        log.info("Executing permission grant workflow...");

        // 1. 마법사 워크플로우 시작 및 컨텍스트 생성
        var initiationDto = permissionWizardService.beginCreation(
                request.getInitialRequest(),
                request.getPolicyName(),
                request.getPolicyDescription()
        );
        String contextId = initiationDto.contextId();
        log.info("Workflow context created with ID: {}", contextId);

        // 2. 워크플로우의 각 단계를 순차적으로 실행
        // (실제 애플리케이션에서는 이 단계들이 별개의 API 호출로 분리될 수 있으나,
        // 여기서는 단일 요청으로 전체 프로세스를 완수하는 것을 보여줍니다.)

        // 2-1. 주체 정보 추가 (beginCreation 에서 이미 처리됨)
//        permissionWizardService.addSubjects(contextId, request.getInitialRequest().subjectIds(), request.getInitialRequest().subjectTypes());
        log.info("Subjects added to context {}", contextId);

        // 2-2. 권한 정보 추가 (beginCreation 에서 이미 처리됨)
//        permissionWizardService.addPermissions(contextId, request.getInitialRequest().permissionIds());
        log.info("Permissions added to context {}", contextId);

        // 2-3. (확장) 추가 조건 적용 단계
        // permissionWizardService.applyConditions(contextId, request.getConditions());

        // 3. 모든 정보가 반영된 컨텍스트를 기반으로 최종 정책 생성 및 저장
        Policy finalPolicy = permissionWizardService.commitPolicy(contextId);
        log.info("Workflow completed. Final policy created with ID: {}", finalPolicy.getId());

        return new WorkflowResult(contextId, "SUCCESS", finalPolicy.getId());
    }
}
