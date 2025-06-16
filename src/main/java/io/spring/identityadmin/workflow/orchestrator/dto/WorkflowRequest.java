package io.spring.identityadmin.workflow.orchestrator.dto;

import io.spring.identityadmin.studio.dto.InitiateGrantRequestDto;

/**
 * 전체 권한 부여 워크플로우를 시작하기 위한 요청 DTO 입니다.
 * 마법사의 모든 단계에 필요한 정보를 담을 수 있습니다.
 */
public class WorkflowRequest {
    private InitiateGrantRequestDto initialRequest;
    private String policyName;
    private String policyDescription;

    // Getters and Setters
    public InitiateGrantRequestDto getInitialRequest() {
        return initialRequest;
    }

    public void setInitialRequest(InitiateGrantRequestDto initialRequest) {
        this.initialRequest = initialRequest;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getPolicyDescription() {
        return policyDescription;
    }

    public void setPolicyDescription(String policyDescription) {
        this.policyDescription = policyDescription;
    }
}
