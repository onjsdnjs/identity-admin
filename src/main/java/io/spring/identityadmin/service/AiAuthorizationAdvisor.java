package io.spring.identityadmin.service;

import io.spring.identityadmin.ai.service.dto.PolicyAnalysisReport;
import io.spring.identityadmin.ai.service.dto.RecommendedRoleDto;
import io.spring.identityadmin.ai.service.dto.ResourceNameSuggestion;
import io.spring.identityadmin.ai.service.dto.TrustAssessment;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;

import java.util.List;

/**
 * 프로메테우스 AI 코어의 모든 지능형 기능을 외부 서비스에 제공하는 퍼사드 인터페이스.
 * 모든 AI 관련 요청은 이 Advisor를 통해 이루어집니다.
 */
public interface AiAuthorizationAdvisor {

    /**
     * 자연어 질의를 받아 정책(Policy) 초안을 생성합니다.
     * @param naturalLanguageQuery "개발팀은 업무시간에만 개발서버 접근 가능"과 같은 자연어 문장
     * @return AI가 생성한 PolicyDto 객체. 관리자는 이 초안을 검토하고 저장할 수 있습니다.
     */
    PolicyDto generatePolicyFromText(String naturalLanguageQuery);

    /**
     * 주어진 인가 컨텍스트의 동적 신뢰도를 평가합니다.
     * @param context 현재 인가 결정에 사용되는 컨텍스트 객체
     * @return 0.0 ~ 1.0 사이의 신뢰도 점수(Trust Score)와 평가 근거(Risk Tags)를 담은 객체
     */
    TrustAssessment assessContext(AuthorizationContext context);

    /**
     * 특정 사용자에게 할당할 역할을 추천합니다.
     * @param userId 추천을 받을 사용자의 ID
     * @return 추천 역할 목록과 그 이유(예: "동일 그룹 동료 80% 보유"), 신뢰도를 포함한 DTO 리스트
     */
    List<RecommendedRoleDto> recommendRolesForUser(Long userId);

    /**
     * 시스템의 잠재적 위험 요소를 분석하여 보고합니다.
     * @return "SoD 위반 가능성이 있는 권한 조합", "장기 미사용 휴면 권한" 등의 분석 결과 리스트
     */
    List<PolicyAnalysisReport> analyzeSecurityPosture();

    /**
     * 새로 발견된 리소스의 기술적 정보를 바탕으로 비즈니스 친화적인 이름과 설명을 제안합니다.
     * @param technicalIdentifier 스캔된 리소스의 고유 식별자 (예: 클래스.메서드명)
     * @param serviceOwner 리소스를 포함하는 서비스 또는 모듈 이름
     * @return AI가 제안하는 이름과 설명을 담은 DTO
     */
    ResourceNameSuggestion suggestResourceName(String technicalIdentifier, String serviceOwner);
}