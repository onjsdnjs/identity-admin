package io.spring.identityadmin.studio.service.impl;

import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.security.xacml.pap.service.PolicyBuilderService;
import io.spring.identityadmin.studio.dto.InitiateGrantRequestDto;
import io.spring.identityadmin.studio.dto.SimulationRequestDto;
import io.spring.identityadmin.studio.dto.SimulationResultDto;
import io.spring.identityadmin.studio.dto.WizardInitiationDto;
import io.spring.identityadmin.studio.service.StudioActionService;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import io.spring.identityadmin.workflow.wizard.service.PermissionWizardService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudioActionServiceImpl implements StudioActionService {

     private final PolicyBuilderService policyBuilderService; // 주입 필요
     private final PermissionWizardService permissionWizardService; // 주입 필요
    private final ModelMapper modelMapper;

    @Override
    public SimulationResultDto runPolicySimulation(SimulationRequestDto simulationRequest) {
        // PolicyDto를 Policy 엔티티로 변환
        Policy policyDraft = modelMapper.map(simulationRequest.policyDraft(), Policy.class);

        // 주입된 PolicyBuilderService의 시뮬레이션 메서드 호출 (현재는 Mock)
        // return policyBuilderService.simulatePolicy(policyDraft);

        // Mock 구현
        return new SimulationResultDto("시뮬레이션 결과: 1개 권한 획득, 0개 권한 상실", List.of(
                new SimulationResultDto.ImpactDetail(
                        "김철수", "USER", "사용자 정보 조회",
                        SimulationResultDto.ImpactType.PERMISSION_GAINED,
                        simulationRequest.policyDraft().getName())
        ));
    }

    @Override
    public WizardInitiationDto initiateGrantWorkflow(InitiateGrantRequestDto grantRequest) {
        String contextId = UUID.randomUUID().toString();
        WizardContext initialContext = new WizardContext(
                contextId,
                "Studio에서 시작된 정책",
                "Authorization Studio를 통해 생성된 정책입니다.",
                grantRequest.subjectIds(),
                grantRequest.subjectTypes(),
                grantRequest.permissionIds(),
                null
        );

        // 주입된 PermissionWizardService의 워크플로우 시작 메서드 호출 (현재는 Mock)
         permissionWizardService.beginPolicyCreation(initialContext);

        // 마법사 페이지 URL과 컨텍스트 ID 반환
        return new WizardInitiationDto(contextId, "/admin/policy-wizard/" + contextId);
    }
}