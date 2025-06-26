package io.spring.aicore.operations;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.aicore.protocol.DomainContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * AI Core 시스템의 핵심 운영 인터페이스
 * 제네릭을 활용하여 다양한 도메인 컨텍스트에 대해 타입 안전한 AI 작업을 제공
 * 
 * @param <T> 도메인 컨텍스트 타입
 */
public interface AICoreOperations<T extends DomainContext> {
    
    /**
     * AI 요청을 실행하고 응답을 반환합니다
     * @param request AI 요청
     * @param responseType 응답 타입
     * @return AI 응답
     */
    <R extends AIResponse> Mono<R> execute(AIRequest<T> request, Class<R> responseType);
    
    /**
     * AI 요청을 스트리밍 방식으로 실행합니다
     * @param request AI 요청
     * @return 스트리밍 응답
     */
    Flux<String> executeStream(AIRequest<T> request);
    
    /**
     * AI 요청을 스트리밍 방식으로 실행하고 타입 안전한 응답을 반환합니다
     * @param request AI 요청
     * @param responseType 응답 타입
     * @return 타입화된 스트리밍 응답
     */
    <R extends AIResponse> Flux<R> executeStreamTyped(AIRequest<T> request, Class<R> responseType);
    
    /**
     * 배치 요청을 처리합니다
     * @param requests 요청 목록
     * @param responseType 응답 타입
     * @return 응답 목록
     */
    <R extends AIResponse> Mono<List<R>> executeBatch(List<AIRequest<T>> requests, Class<R> responseType);
    
    /**
     * 다중 도메인 컨텍스트 요청을 처리합니다
     * @param requests1 첫 번째 타입의 요청 목록
     * @param requests2 두 번째 타입의 요청 목록
     * @return 혼합 응답
     */
    <T1 extends DomainContext, T2 extends DomainContext> 
    Mono<AIResponse> executeMixed(List<AIRequest<T1>> requests1, List<AIRequest<T2>> requests2);
    
    /**
     * 시스템 상태를 확인합니다
     * @return 시스템 상태
     */
    Mono<HealthStatus> checkHealth();
    
    /**
     * 지원하는 기능 목록을 반환합니다
     * @return 지원 기능 집합
     */
    Set<AICapability> getSupportedCapabilities();
    
    /**
     * 특정 작업을 지원하는지 확인합니다
     * @param operation 작업명
     * @return 지원 여부
     */
    boolean supportsOperation(String operation);
    
    /**
     * 시스템 메트릭을 조회합니다
     * @return 시스템 메트릭
     */
    Mono<SystemMetrics> getMetrics();
    
    /**
     * 시스템 상태 열거형
     */
    enum HealthStatus {
        HEALTHY,            // 정상
        DEGRADED,           // 성능 저하
        UNHEALTHY,          // 비정상
        MAINTENANCE         // 유지보수 중
    }
    
    /**
     * AI 기능 열거형
     */
    enum AICapability {
        TEXT_GENERATION,        // 텍스트 생성
        TEXT_ANALYSIS,          // 텍스트 분석
        STREAMING,              // 스트리밍
        BATCH_PROCESSING,       // 배치 처리
        MULTI_DOMAIN,           // 다중 도메인
        REAL_TIME,              // 실시간 처리
        ASYNC_PROCESSING,       // 비동기 처리
        CONTEXT_AWARENESS,      // 컨텍스트 인식
        VALIDATION,             // 검증
        OPTIMIZATION            // 최적화
    }
    
    /**
     * 시스템 메트릭 정보
     */
    class SystemMetrics {
        private final long totalRequests;
        private final long successfulRequests;
        private final long failedRequests;
        private final double averageResponseTime;
        private final double throughput;
        private final long activeConnections;
        
        public SystemMetrics(long totalRequests, long successfulRequests, long failedRequests,
                           double averageResponseTime, double throughput, long activeConnections) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.averageResponseTime = averageResponseTime;
            this.throughput = throughput;
            this.activeConnections = activeConnections;
        }
        
        // Getters
        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public long getFailedRequests() { return failedRequests; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public double getThroughput() { return throughput; }
        public long getActiveConnections() { return activeConnections; }
        
        public double getSuccessRate() {
            return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("SystemMetrics{total=%d, success=%d, failed=%d, avgTime=%.2fms, throughput=%.2f/s}", 
                    totalRequests, successfulRequests, failedRequests, averageResponseTime, throughput);
        }
    }
}