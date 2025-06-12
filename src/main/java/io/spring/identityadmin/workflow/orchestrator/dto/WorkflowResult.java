package io.spring.identityadmin.workflow.orchestrator.dto;

/**
 * 워크플로우 실행 결과를 담는 DTO 입니다.
 */
public record WorkflowResult(
        String workflowId,
        String status, // "SUCCESS", "FAILURE"
        Long createdPolicyId
) {}
