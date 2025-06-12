package io.spring.identityadmin.studio.service;

import io.spring.identityadmin.studio.dto.*;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;

/**
 * Authorization Studio의 'Inspector' 패널에서 발생하는 사용자 액션을 처리합니다.
 * 정책 시뮬레이션을 실행하고, 권한 부여 마법사를 시작하는 등 다른 서비스와의 연동을 담당합니다.
 */
public interface StudioActionService {

    /**
     * "What-if" 시나리오를 실행합니다.
     * 제안된 정책 변경(생성/수정/삭제)이 시스템에 미칠 영향을 미리 분석하여 결과를 반환합니다.
     * 내부적으로 PolicyBuilderService의 시뮬레이션 기능을 호출합니다.
     * @param simulationRequest 시뮬레이션할 정책 변경 내용
     * @return 영향을 받는 사용자, 변경되는 권한 등 상세 분석 결과를 담은 DTO
     */
    SimulationResultDto runPolicySimulation(SimulationRequestDto simulationRequest);

    /**
     * 권한 부여 마법사 워크플로우를 시작합니다.
     * Studio에서 선택된 주체와 리소스 정보를 포함하여 마법사를 pre-populate 상태로 초기화합니다.
     * @param subjectId 마법사에 미리 채워둘 주체의 ID
     * @param resourceId 마법사에 미리 채워둘 리소스의 ID
     * @return 권한 부여 마법사를 시작하기 위한 초기 컨텍스트 정보
     */
    WizardContext initiateGrantWorkflow(Long subjectId, Long resourceId);
}