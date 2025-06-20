package io.spring.identityadmin.ai;
import io.spring.identityadmin.ai.dto.PolicyAnalysisReport;
import io.spring.identityadmin.ai.dto.RecommendedRoleDto;
import io.spring.identityadmin.ai.dto.ResourceNameSuggestion;
import io.spring.identityadmin.ai.dto.TrustAssessment;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.security.xacml.pip.context.AuthorizationContext;

import java.util.List;

/**
 * AI-Native IAM 플랫폼의 모든 지능형 기능을 제공하는 핵심 어드바이저(Advisor) 인터페이스.
 * 플랫폼의 다른 컴포넌트들은 이 인터페이스를 통해 AI의 분석 및 추론 능력을 활용합니다.
 */
public interface AINativeIAMAdvisor {

    /**
     * 관리자의 자연어 요구사항을 분석하여, 시스템이 실행할 수 있는 정책(Policy) 초안을 생성합니다.
     * @param naturalLanguageQuery "개발팀은 업무시간에만 개발서버 접근 가능"과 같은 자연어 문장
     * @return AI가 생성한 PolicyDto 객체
     */
    PolicyDto generatePolicyFromText(String naturalLanguageQuery);

    /**
     * 실시간으로 수집된 모든 인가 컨텍스트를 종합적으로 분석하여, 현재 접근 요청의 신뢰도를 평가하고 최종 판단을 내립니다.
     * @param context 현재 인가 결정에 사용되는 컨텍스트 객체
     * @return 신뢰도 점수(Trust Score)와 평가 근거(Risk Tags)를 담은 객체
     */
    TrustAssessment assessContext(AuthorizationContext context);

    /**
     * 특정 사용자의 프로필과 동료 그룹의 권한 패턴을 분석하여, 가장 적합한 역할을 추천합니다.
     * @param userId 추천을 받을 사용자의 ID
     * @return 추천 역할 목록과 그 이유, 신뢰도를 포함한 DTO 리스트
     */
    List<RecommendedRoleDto> recommendRolesForUser(Long userId);

    /**
     * 시스템의 전체 보안 정책 및 권한 상태를 분석하여, 잠재적 위험을 식별하고 개선안을 보고합니다.
     * @return SoD 위반, 과잉 권한 등 분석 결과 리스트
     */
    List<PolicyAnalysisReport> analyzeSecurityPosture();

    /**
     * 새로 발견된 기술적 리소스 정보를 바탕으로, 비즈니스 사용자가 이해할 수 있는 이름과 설명을 제안합니다.
     * @param technicalIdentifier 스캔된 리소스의 고유 식별자
     * @param serviceOwner 리소스를 포함하는 서비스 또는 모듈 이름
     * @return AI가 제안하는 이름과 설명을 담은 DTO
     */
    ResourceNameSuggestion suggestResourceName(String technicalIdentifier, String serviceOwner);
}