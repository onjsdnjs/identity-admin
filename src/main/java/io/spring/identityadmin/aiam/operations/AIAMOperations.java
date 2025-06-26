package io.spring.identityadmin.aiam.operations;

import io.spring.aicore.operations.AICoreOperations;
import io.spring.identityadmin.aiam.protocol.IAMContext;
import io.spring.identityadmin.aiam.protocol.IAMRequest;
import io.spring.identityadmin.aiam.protocol.IAMResponse;
import io.spring.identityadmin.aiam.protocol.response.ConflictDetectionResponse;
import io.spring.identityadmin.aiam.protocol.response.PolicyDraftResponse;
import io.spring.identityadmin.aiam.protocol.types.PolicyContext;
import io.spring.identityadmin.aiam.protocol.types.RiskContext;
import io.spring.identityadmin.aiam.protocol.types.UserContext;

// 별도 패키지의 요청/응답 클래스들 import
import io.spring.identityadmin.aiam.protocol.request.*;
import io.spring.identityadmin.aiam.protocol.response.*;
import org.springframework.security.core.context.SecurityContext;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * IAM AI 운영 인터페이스
 * 
 * 🎯 단일 책임: IAM 도메인 AI 작업의 계약만 정의
 * 📋 SRP 준수: 오직 메서드 시그니처만 포함
 * 🔒 OCP 준수: 확장시 인터페이스 수정 불필요
 * ⚡ 동기식 우선: 필요한 곳에만 비동기 적용
 * 
 * @param <T> IAM 컨텍스트 타입
 */
public interface AIAMOperations<T extends IAMContext> extends AICoreOperations<T> {
    
    // ==================== Core IAM Operations ====================
    
    /**
     * 감사 로깅과 함께 AI 요청을 실행합니다
     * 동기식: 대부분의 요청은 즉시 처리 가능
     */
    <R extends IAMResponse> R executeWithAudit(IAMRequest<T> request, Class<R> responseType);
    
    /**
     * 보안 검증과 함께 AI 요청을 실행합니다
     * 동기식: 보안 검증은 즉시 완료되어야 함
     */
    <R extends IAMResponse> R executeWithSecurity(IAMRequest<T> request, 
                                                 SecurityContext securityContext,
                                                 Class<R> responseType);
    
    // ==================== Domain-Specific Operations ====================
    
    /**
     * 정책을 생성합니다
     * 동기식: 일반적인 정책 생성은 즉시 완료
     */
    PolicyResponse generatePolicy(PolicyRequest<PolicyContext> request);
    
    /**
     * 정책을 스트리밍 방식으로 생성합니다
     * 비동기식: 대용량 정책 생성시에만 스트림 필요
     */
    Stream<PolicyDraftResponse> generatePolicyStream(PolicyRequest<PolicyContext> request);
    
    /**
     * 위험도를 분석합니다
     * 동기식: 위험도 분석은 즉시 결과 필요
     */
    RiskAssessmentResponse assessRisk(RiskRequest<RiskContext> request);
    
    /**
     * 실시간 위험 모니터링을 시작합니다
     * 비동기식: 실시간 모니터링은 장시간 실행
     */
    CompletableFuture<Void> startRiskMonitoring(RiskRequest<RiskContext> request, 
                                               RiskEventCallback callback);
    
    /**
     * 정책 충돌을 감지합니다
     *
     * 동기식: 충돌 감지는 즉시 완료
     */
    ConflictDetectionResponse detectConflicts(ConflictDetectionRequest<PolicyContext> request);
    
    /**
     * 스마트 추천을 제공합니다
     * 동기식: 추천은 즉시 제공
     */
    <C extends IAMContext> RecommendationResponse<C> recommend(RecommendationRequest<C> request);
    
    /**
     * 사용자 권한을 분석합니다
     * 동기식: 사용자 분석은 즉시 완료
     */
    UserAnalysisResponse analyzeUser(UserAnalysisRequest<UserContext> request);
    
    /**
     * 정책을 최적화합니다
     * 동기식: 정책 최적화는 즉시 완료
     */
    OptimizationResponse optimizePolicy(OptimizationRequest<PolicyContext> request);
    
    /**
     * 정책 유효성을 검증합니다
     * 동기식: 검증은 즉시 완료되어야 함
     */
    ValidationResponse validatePolicy(ValidationRequest<PolicyContext> request);
    
    /**
     * IAM 감사 로그를 분석합니다
     * 비동기식: 대용량 로그 분석은 시간이 소요될 수 있음
     */
    CompletableFuture<AuditAnalysisResponse> analyzeAuditLogs(AuditAnalysisRequest<T> request);
    
    // ==================== Callback Interfaces ====================
    
    /**
     * 위험 이벤트 콜백 인터페이스
     * 적절한 내부 인터페이스 사용 예시
     */
    interface RiskEventCallback {
        void onRiskDetected(RiskEvent event);
        void onError(Exception error);
    }
    
    /**
     * 위험 이벤트 데이터 클래스
     * 간단한 데이터 홀더로 내부 클래스 적절한 사용
     */
    class RiskEvent {
        private final String riskType;
        private final String severity;
        private final long timestamp;
        
        public RiskEvent(String riskType, String severity) {
            this.riskType = riskType;
            this.severity = severity;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public String getRiskType() { return riskType; }
        public String getSeverity() { return severity; }
        public long getTimestamp() { return timestamp; }
    }
}