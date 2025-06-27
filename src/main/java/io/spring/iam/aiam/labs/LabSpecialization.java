package io.spring.iam.aiam.labs;

/**
 * 🔬 IAM 연구소 전문 분야 정의
 * 
 * 각 연구소의 전문 영역과 핵심 역량을 명확히 구분
 */
public enum LabSpecialization {
    
    /**
     * 🏭 정책 생성 및 관리 전문 연구소
     * - AI 기반 정책 자동 생성
     * - 정책 템플릿 최적화
     * - 정책 충돌 감지 및 해결
     */
    POLICY_GENERATION("Policy Generation & Management", 
                     "Advanced AI-driven policy creation and optimization"),
    
    /**
     * ⚠️ 위험 평가 및 분석 전문 연구소
     * - 실시간 위험 탐지
     * - 위험 패턴 분석
     * - 예측적 위험 평가
     */
    RISK_ASSESSMENT("Risk Assessment & Analysis", 
                   "Comprehensive risk evaluation and predictive analysis"),
    
    /**
     * 👤 사용자 행동 분석 전문 연구소
     * - 사용자 패턴 분석
     * - 이상 행동 탐지
     * - 개인화된 보안 추천
     */
    USER_BEHAVIOR_ANALYSIS("User Behavior Analysis", 
                          "Deep user pattern analysis and anomaly detection"),
    
    /**
     * 🔐 접근 제어 최적화 전문 연구소
     * - 동적 접근 제어
     * - 권한 최적화
     * - 제로 트러스트 구현
     */
    ACCESS_CONTROL_OPTIMIZATION("Access Control Optimization", 
                               "Dynamic access control and zero-trust implementation"),
    
    /**
     * 🔍 감사 및 컴플라이언스 전문 연구소
     * - 자동 감사 로그 분석
     * - 컴플라이언스 검증
     * - 규정 준수 모니터링
     */
    AUDIT_COMPLIANCE("Audit & Compliance", 
                    "Automated audit analysis and compliance verification"),
    
    /**
     * 🤖 AI 모델 통합 및 최적화 전문 연구소
     * - AI 모델 성능 튜닝
     * - 모델 간 협업 최적화
     * - 실시간 모델 업데이트
     */
    AI_MODEL_OPTIMIZATION("AI Model Integration & Optimization", 
                         "Advanced AI model tuning and collaborative optimization"),
    
    /**
     * 🛡️ 보안 인텔리전스 전문 연구소
     * - 위협 인텔리전스 분석
     * - 보안 이벤트 상관관계 분석
     * - 사이버 위협 예측
     */
    SECURITY_INTELLIGENCE("Security Intelligence", 
                         "Threat intelligence and cyber security prediction"),
    
    /**
     * 💡 추천 시스템 전문 연구소
     * - 개인화된 보안 추천
     * - 정책 추천 엔진
     * - 최적 구성 제안
     */
    RECOMMENDATION_SYSTEM("Recommendation System", 
                         "Personalized security and policy recommendations"),
    
    /**
     * 🔄 워크플로우 자동화 전문 연구소
     * - 자동화 워크플로우 설계
     * - 프로세스 최적화
     * - 통합 오케스트레이션
     */
    WORKFLOW_AUTOMATION("Workflow Automation", 
                       "Intelligent workflow design and process optimization"),
    
    /**
     * 📊 데이터 분석 및 인사이트 전문 연구소
     * - 빅데이터 분석
     * - 패턴 인식
     * - 예측 분석
     */
    DATA_ANALYTICS("Data Analytics & Insights", 
                  "Advanced data analysis and predictive insights");
    
    private final String displayName;
    private final String description;
    
    LabSpecialization(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * 전문 분야의 표시 이름을 반환합니다
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 전문 분야의 상세 설명을 반환합니다
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 전문 분야의 우선순위를 반환합니다 (낮을수록 높은 우선순위)
     */
    public int getPriority() {
        return switch (this) {
            case SECURITY_INTELLIGENCE -> 1;
            case RISK_ASSESSMENT -> 2;
            case ACCESS_CONTROL_OPTIMIZATION -> 3;
            case POLICY_GENERATION -> 4;
            case USER_BEHAVIOR_ANALYSIS -> 5;
            case AUDIT_COMPLIANCE -> 6;
            case AI_MODEL_OPTIMIZATION -> 7;
            case RECOMMENDATION_SYSTEM -> 8;
            case WORKFLOW_AUTOMATION -> 9;
            case DATA_ANALYTICS -> 10;
        };
    }
    
    /**
     * 다른 전문 분야와의 협업 친화도를 반환합니다 (0.0 ~ 1.0)
     */
    public double getCollaborationAffinity(LabSpecialization other) {
        return switch (this) {
            case POLICY_GENERATION -> switch (other) {
                case RISK_ASSESSMENT -> 0.9;
                case ACCESS_CONTROL_OPTIMIZATION -> 0.8;
                case AUDIT_COMPLIANCE -> 0.7;
                case RECOMMENDATION_SYSTEM -> 0.8;
                default -> 0.5;
            };
            case RISK_ASSESSMENT -> switch (other) {
                case POLICY_GENERATION -> 0.9;
                case SECURITY_INTELLIGENCE -> 0.9;
                case USER_BEHAVIOR_ANALYSIS -> 0.8;
                case DATA_ANALYTICS -> 0.7;
                default -> 0.5;
            };
            case USER_BEHAVIOR_ANALYSIS -> switch (other) {
                case RISK_ASSESSMENT -> 0.8;
                case SECURITY_INTELLIGENCE -> 0.8;
                case RECOMMENDATION_SYSTEM -> 0.9;
                case DATA_ANALYTICS -> 0.8;
                default -> 0.5;
            };
            case ACCESS_CONTROL_OPTIMIZATION -> switch (other) {
                case POLICY_GENERATION -> 0.8;
                case SECURITY_INTELLIGENCE -> 0.7;
                case WORKFLOW_AUTOMATION -> 0.8;
                default -> 0.5;
            };
            case AUDIT_COMPLIANCE -> switch (other) {
                case POLICY_GENERATION -> 0.7;
                case RISK_ASSESSMENT -> 0.6;
                case DATA_ANALYTICS -> 0.8;
                default -> 0.4;
            };
            case AI_MODEL_OPTIMIZATION -> switch (other) {
                case RECOMMENDATION_SYSTEM -> 0.9;
                case DATA_ANALYTICS -> 0.8;
                case USER_BEHAVIOR_ANALYSIS -> 0.7;
                default -> 0.6;
            };
            case SECURITY_INTELLIGENCE -> switch (other) {
                case RISK_ASSESSMENT -> 0.9;
                case USER_BEHAVIOR_ANALYSIS -> 0.8;
                case DATA_ANALYTICS -> 0.8;
                default -> 0.6;
            };
            case RECOMMENDATION_SYSTEM -> switch (other) {
                case USER_BEHAVIOR_ANALYSIS -> 0.9;
                case AI_MODEL_OPTIMIZATION -> 0.9;
                case POLICY_GENERATION -> 0.8;
                default -> 0.6;
            };
            case WORKFLOW_AUTOMATION -> switch (other) {
                case ACCESS_CONTROL_OPTIMIZATION -> 0.8;
                case POLICY_GENERATION -> 0.7;
                default -> 0.5;
            };
            case DATA_ANALYTICS -> switch (other) {
                case USER_BEHAVIOR_ANALYSIS -> 0.8;
                case RISK_ASSESSMENT -> 0.7;
                case SECURITY_INTELLIGENCE -> 0.8;
                case AI_MODEL_OPTIMIZATION -> 0.8;
                case AUDIT_COMPLIANCE -> 0.8;
                default -> 0.6;
            };
        };
    }
    
    /**
     * 전문 분야가 특정 작업을 처리할 수 있는지 확인합니다
     */
    public boolean canHandle(String operation) {
        return switch (this) {
            case POLICY_GENERATION -> operation.contains("policy") || 
                                    operation.contains("generation") ||
                                    operation.contains("create");
            case RISK_ASSESSMENT -> operation.contains("risk") || 
                                  operation.contains("assess") ||
                                  operation.contains("threat");
            case USER_BEHAVIOR_ANALYSIS -> operation.contains("user") || 
                                         operation.contains("behavior") ||
                                         operation.contains("analysis");
            case ACCESS_CONTROL_OPTIMIZATION -> operation.contains("access") || 
                                              operation.contains("control") ||
                                              operation.contains("permission");
            case AUDIT_COMPLIANCE -> operation.contains("audit") || 
                                   operation.contains("compliance") ||
                                   operation.contains("log");
            case AI_MODEL_OPTIMIZATION -> operation.contains("model") || 
                                        operation.contains("optimization") ||
                                        operation.contains("tuning");
            case SECURITY_INTELLIGENCE -> operation.contains("security") || 
                                        operation.contains("intelligence") ||
                                        operation.contains("threat");
            case RECOMMENDATION_SYSTEM -> operation.contains("recommend") || 
                                        operation.contains("suggest") ||
                                        operation.contains("advice");
            case WORKFLOW_AUTOMATION -> operation.contains("workflow") || 
                                      operation.contains("automation") ||
                                      operation.contains("process");
            case DATA_ANALYTICS -> operation.contains("data") || 
                                 operation.contains("analytics") ||
                                 operation.contains("insight");
        };
    }
} 