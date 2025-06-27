package io.spring.iam.aiam.protocol.enums;

/**
 * 🎯 AI 진단 타입 열거형
 * 
 * AINativeIAMSynapseArbiterFromOllama의 모든 AI 진단 기능을 타입으로 정의
 * 
 * 🔥 주요 진단 타입들:
 * - POLICY_GENERATION: 정책 생성 (generatePolicyFromTextStream, generatePolicyFromTextByAi)
 * - CONDITION_TEMPLATE: 조건 템플릿 생성 (generateUniversalConditionTemplates, generateSpecificConditionTemplates)
 * - TRUST_ASSESSMENT: 신뢰도 평가 (assessContext)
 * - RESOURCE_NAMING: 리소스 이름 제안 (suggestResourceName, suggestResourceNamesInBatch)
 * - ROLE_RECOMMENDATION: 역할 추천 (recommendRolesForUser)
 * - SECURITY_POSTURE: 보안 상태 분석 (analyzeSecurityPosture)
 */
public enum DiagnosisType {
    
    /**
     * 🔥 정책 생성 진단
     * - generatePolicyFromTextStream
     * - generatePolicyFromTextByAi
     */
    POLICY_GENERATION("정책 생성", "자연어 요구사항을 분석하여 IAM 정책을 생성합니다"),
    
    /**
     * 🔬 조건 템플릿 생성 진단  
     * - generateUniversalConditionTemplates
     * - generateSpecificConditionTemplates
     */
    CONDITION_TEMPLATE("조건 템플릿 생성", "범용 및 특화 조건 템플릿을 AI로 생성합니다"),
    
    /**
     * 🛡️ 신뢰도 평가 진단
     * - assessContext
     */
    TRUST_ASSESSMENT("신뢰도 평가", "인증 컨텍스트를 분석하여 신뢰도를 평가합니다"),
    
    /**
     * 🏷️ 리소스 이름 제안 진단
     * - suggestResourceName
     * - suggestResourceNamesInBatch
     */
    RESOURCE_NAMING("리소스 이름 제안", "기술적 식별자를 사용자 친화적 이름으로 변환합니다"),
    
    /**
     * 👤 역할 추천 진단
     * - recommendRolesForUser
     */
    ROLE_RECOMMENDATION("역할 추천", "사용자에게 적합한 역할을 AI로 추천합니다"),
    
    /**
     * 🔍 보안 상태 분석 진단
     * - analyzeSecurityPosture
     */
    SECURITY_POSTURE("보안 상태 분석", "전체 시스템의 보안 상태를 분석하고 개선점을 제안합니다");
    
    private final String displayName;
    private final String description;
    
    DiagnosisType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 문자열로부터 DiagnosisType을 찾습니다
     */
    public static DiagnosisType fromString(String type) {
        for (DiagnosisType diagnosisType : values()) {
            if (diagnosisType.name().equalsIgnoreCase(type) || 
                diagnosisType.displayName.equalsIgnoreCase(type)) {
                return diagnosisType;
            }
        }
        throw new IllegalArgumentException("Unknown diagnosis type: " + type);
    }
} 