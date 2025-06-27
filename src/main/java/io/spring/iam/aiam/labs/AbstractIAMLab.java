package io.spring.iam.aiam.labs;

import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🔬 IAM 전문 연구소 추상 기본 클래스
 * 
 * 🏛️ 세계 최고 수준의 AI-Native IAM 연구소 기반:
 * - 각 연구소는 특정 IAM 도메인의 전문가
 * - 독립적인 AI 모델과 알고리즘 보유
 * - 실시간 성능 모니터링 및 최적화
 * - 다른 연구소와의 협업 지원
 * 
 * @param <T> IAM 컨텍스트 타입
 */
@Slf4j
public abstract class AbstractIAMLab<T extends IAMContext> {
    
    // ==================== 🏛️ 연구소 기본 정보 ====================
    private final String labId;
    private final String labName;
    private final String labVersion;
    private final LabSpecialization specialization;
    private final LabCapabilities capabilities;
    
    // ==================== 📊 성능 메트릭 추적 ====================
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    
    // ==================== 🔧 연구소 상태 관리 ====================
    private LabStatus currentStatus = LabStatus.INITIALIZING;
    private double currentLoad = 0.0;
    private long lastHealthCheck = System.currentTimeMillis();
    
    protected AbstractIAMLab(String labName, String labVersion, 
                           LabSpecialization specialization, 
                           LabCapabilities capabilities) {
        this.labId = generateLabId();
        this.labName = labName;
        this.labVersion = labVersion;
        this.specialization = specialization;
        this.capabilities = capabilities;
        
        log.info("🔬 IAM Lab initialized: {} v{} - Specialization: {}", 
                labName, labVersion, specialization.name());
    }
    
    // ==================== 🎯 핵심 연구소 인터페이스 ====================
    
    /**
     * 연구소의 주요 연구 작업을 수행합니다
     * 각 연구소는 이 메서드를 구현하여 전문 분야의 AI 작업을 처리
     */
    public abstract <R extends IAMResponse> R conductResearch(IAMRequest<T> request, Class<R> responseType);
    
    /**
     * 연구소가 지원하는 작업 목록을 반환합니다
     */
    public abstract Set<String> getSupportedOperations();
    
    /**
     * 연구소의 전문 분야를 반환합니다
     */
    public abstract String getSpecializationDescription();
    
    /**
     * 연구소의 현재 연구 역량을 평가합니다
     */
    public abstract LabCapabilityAssessment assessCapabilities();
    
    // ==================== 🔍 연구소 운영 메서드 ====================
    
    /**
     * 연구 작업을 실행하고 성능을 추적합니다
     */
    public final <R extends IAMResponse> R executeResearch(IAMRequest<T> request, Class<R> responseType) {
        long startTime = System.currentTimeMillis();
        totalRequests.incrementAndGet();
        
        try {
            log.info("🔬 {} Lab: Research initiated for {}", labName, request.getClass().getSimpleName());
            
            // 1. 전처리 및 검증
            validateRequest(request);
            updateLoad(1.0);
            
            // 2. 실제 연구 수행
            R result = conductResearch(request, responseType);
            
            // 3. 후처리 및 검증
            validateResult(result);
            
            // 4. 성공 메트릭 업데이트
            long executionTime = System.currentTimeMillis() - startTime;
            recordSuccess(executionTime);
            
            log.info("✅ {} Lab: Research completed in {}ms", labName, executionTime);
            return result;
            
        } catch (Exception e) {
            // 5. 실패 처리
            long executionTime = System.currentTimeMillis() - startTime;
            recordFailure(executionTime, e);
            
            log.error("❌ {} Lab: Research failed after {}ms - {}", labName, executionTime, e.getMessage());
            throw new LabExecutionException("Lab research failed: " + e.getMessage(), e);
            
        } finally {
            updateLoad(-1.0);
        }
    }
    
    /**
     * 특정 작업을 지원하는지 확인합니다
     */
    public final boolean supportsOperation(String operation) {
        return getSupportedOperations().contains(operation);
    }
    
    /**
     * 연구소 상태를 확인합니다
     */
    public final boolean isHealthy() {
        return currentStatus == LabStatus.OPERATIONAL && 
               currentLoad < capabilities.getMaxLoad() &&
               (System.currentTimeMillis() - lastHealthCheck) < 30000; // 30초 이내
    }
    
    /**
     * 연구소 헬스체크를 수행합니다
     */
    public final LabHealthStatus performHealthCheck() {
        try {
            lastHealthCheck = System.currentTimeMillis();
            
            // 기본 헬스체크
            boolean isOperational = currentStatus == LabStatus.OPERATIONAL;
            boolean isLoadAcceptable = currentLoad < capabilities.getMaxLoad();
            boolean hasResources = checkResourceAvailability();
            
            // 전문 헬스체크 (각 연구소별 구현)
            boolean isSpecializedHealthy = performSpecializedHealthCheck();
            
            if (isOperational && isLoadAcceptable && hasResources && isSpecializedHealthy) {
                currentStatus = LabStatus.OPERATIONAL;
                return LabHealthStatus.healthy(labId, labName);
            } else {
                currentStatus = LabStatus.DEGRADED;
                return LabHealthStatus.degraded(labId, labName, "Performance issues detected");
            }
            
        } catch (Exception e) {
            currentStatus = LabStatus.FAILED;
            log.error("❌ {} Lab: Health check failed", labName, e);
            return LabHealthStatus.failed(labId, labName, e.getMessage());
        }
    }
    
    /**
     * 연구소 성능 메트릭을 반환합니다
     */
    public final LabMetrics getMetrics() {
        long total = totalRequests.get();
        double successRate = total > 0 ? (double) successfulRequests.get() / total * 100.0 : 0.0;
        double averageResponseTime = total > 0 ? (double) totalExecutionTime.get() / total : 0.0;
        double throughput = calculateThroughput();
        
        return new LabMetrics(
            labId, labName, specialization,
            total, successfulRequests.get(), failedRequests.get(),
            successRate, averageResponseTime, throughput,
            currentLoad, currentStatus,
            System.currentTimeMillis()
        );
    }
    
    /**
     * 연구소를 다른 연구소와 협업시킵니다
     */
    public final <R extends IAMResponse> R collaborateWith(List<AbstractIAMLab<T>> collaborators,
                                                          IAMRequest<T> request,
                                                          Class<R> responseType) {
        log.info("🤝 {} Lab: Starting collaboration with {} labs", labName, collaborators.size());
        
        try {
            // 1. 협업 전략 수립
            CollaborationStrategy strategy = planCollaboration(collaborators, request);
            
            // 2. 각 연구소별 작업 분할
            Map<AbstractIAMLab<T>, IAMRequest<T>> workDistribution = distributeWork(collaborators, request, strategy);
            
            // 3. 병렬 연구 수행
            Map<AbstractIAMLab<T>, IAMResponse> results = executeCollaborativeResearch(workDistribution, responseType);
            
            // 4. 결과 통합
            R finalResult = synthesizeResults(results, responseType);
            
            log.info("✅ {} Lab: Collaboration completed successfully", labName);
            return finalResult;
            
        } catch (Exception e) {
            log.error("❌ {} Lab: Collaboration failed", labName, e);
            throw new LabExecutionException("Collaborative research failed: " + e.getMessage(), e);
        }
    }
    
    // ==================== 🔧 보호된 헬퍼 메서드 ====================
    
    /**
     * 각 연구소별 전문 헬스체크 (서브클래스에서 구현)
     */
    protected abstract boolean performSpecializedHealthCheck();
    
    /**
     * 협업 전략 수립 (서브클래스에서 오버라이드 가능)
     */
    protected CollaborationStrategy planCollaboration(List<AbstractIAMLab<T>> collaborators, IAMRequest<T> request) {
        return CollaborationStrategy.createDefault(collaborators, request);
    }
    
    /**
     * 작업 분할 (서브클래스에서 오버라이드 가능)
     */
    protected Map<AbstractIAMLab<T>, IAMRequest<T>> distributeWork(List<AbstractIAMLab<T>> collaborators, 
                                                                 IAMRequest<T> request,
                                                                 CollaborationStrategy strategy) {
        return strategy.distributeWork(collaborators, request);
    }
    
    /**
     * 결과 통합 (서브클래스에서 구현)
     */
    protected abstract <R extends IAMResponse> R synthesizeResults(Map<AbstractIAMLab<T>, IAMResponse> results, 
                                                                  Class<R> responseType);
    
    // ==================== 🔧 Private 헬퍼 메서드 ====================
    
    private void validateRequest(IAMRequest<T> request) {
        if (request == null) {
            throw new IllegalArgumentException("Research request cannot be null");
        }
        if (request.getContext() == null) {
            throw new IllegalArgumentException("IAM context cannot be null");
        }
    }
    
    private void validateResult(IAMResponse result) {
        if (result == null) {
            throw new LabExecutionException("Research result cannot be null");
        }
    }
    
    private void recordSuccess(long executionTime) {
        successfulRequests.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime);
    }
    
    private void recordFailure(long executionTime, Exception error) {
        failedRequests.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime);
        
        // 실패 패턴 분석을 위한 로깅
        log.warn("🔍 {} Lab: Failure analysis - Error: {}, ExecutionTime: {}ms", 
                labName, error.getClass().getSimpleName(), executionTime);
    }
    
    private void updateLoad(double delta) {
        currentLoad = Math.max(0.0, Math.min(100.0, currentLoad + delta));
    }
    
    private boolean checkResourceAvailability() {
        // 기본 리소스 체크 (메모리, CPU 등)
        return Runtime.getRuntime().freeMemory() > (1024 * 1024 * 100); // 100MB 이상
    }
    
    private double calculateThroughput() {
        // 간단한 처리량 계산 (실제로는 더 정교한 계산 필요)
        long total = totalRequests.get();
        return total > 0 ? total / 60.0 : 0.0; // requests per minute
    }
    
    private Map<AbstractIAMLab<T>, IAMResponse> executeCollaborativeResearch(
            Map<AbstractIAMLab<T>, IAMRequest<T>> workDistribution, 
            Class<? extends IAMResponse> responseType) {
        // 협업 연구 실행 로직 (현재는 단순 구현)
        return Map.of();
    }
    
    private String generateLabId() {
        return "lab-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    // ==================== 🔧 Getter 메서드 ====================
    
    public final String getLabId() { return labId; }
    public final String getLabName() { return labName; }
    public final String getLabVersion() { return labVersion; }
    public final LabSpecialization getSpecialization() { return specialization; }
    public final LabCapabilities getCapabilities() { return capabilities; }
    public final LabStatus getCurrentStatus() { return currentStatus; }
    public final double getCurrentLoad() { return currentLoad; }
} 