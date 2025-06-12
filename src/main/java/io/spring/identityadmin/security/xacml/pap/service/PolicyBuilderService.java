package io.spring.identityadmin.security.xacml.pap.service;

/**
 * 비전문가 사용자를 위한 시각적 정책 빌더 및 시뮬레이션 기능을 제공하는 서비스입니다.
 */
public interface PolicyBuilderService {
    /**
     * 일반적인 시나리오(예: '신입사원 기본 권한', '특정 프로젝트 접근')에 맞는 사전 정의된 정책 템플릿 목록을 제공합니다.
     * @param context 현재 정책 생성 컨텍스트 (예: 사용자 타입, 조직 부서)
     * @return 사용 가능한 정책 템플릿 목록
     */
    List<PolicyTemplateDto> getAvailableTemplates(PolicyContext context);

    /**
     * 시각적 빌더 UI로부터 전달받은 구성 요소(주체, 리소스, 조건 등)를 실제 Policy 객체로 변환합니다.
     * @param visualPolicyDto UI에서 구성된 시각적 정책 데이터
     * @return 생성된 Policy 객체
     */
    Policy buildPolicyFromVisualComponents(VisualPolicyDto visualPolicyDto);

    /**
     * 새로 생성하거나 수정한 정책이 실제 운영 환경에 어떤 영향을 미칠지 미리 시뮬레이션합니다.
     * @param policyToSimulate 시뮬레이션할 Policy 객체
     * @param simulationContext 시뮬레이션 컨텍스트 (특정 사용자, 특정 리소스 등)
     * @return "사용자 A는 이 정책으로 인해 X 리소스에 접근 가능해집니다." 와 같은 시뮬레이션 결과
     */
    SimulationResultDto simulatePolicy(Policy policyToSimulate, SimulationContext simulationContext);

    /**
     * 새로운 정책이 기존 정책들과 충돌(예: 동일 대상에 대해 허용과 거부가 동시에 존재)하는지 감지합니다.
     * @param newPolicy 검사할 신규 정책
     * @return 발견된 정책 충돌 목록
     */
    List<PolicyConflictDto> detectConflicts(Policy newPolicy);
}
