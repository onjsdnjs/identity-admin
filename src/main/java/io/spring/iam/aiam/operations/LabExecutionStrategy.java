package io.spring.iam.aiam.operations;

import io.spring.iam.aiam.protocol.IAMContext;
import java.util.List;
import java.util.Map;

/**
 * 연구소 실행 전략
 * 
 * 🎯 마스터 브레인이 수립하는 정밀한 실행 전략
 * - 어떤 연구소를 어떤 순서로 실행할지 결정
 * - 각 연구소 간 데이터 흐름 정의
 * - 예외 상황별 대응 전략 수립
 * - 성능 최적화 전략 포함
 */
public class LabExecutionStrategy {
    
    private final String strategyId;
    private final String operationType;
    private final List<LabExecutionStep> executionSteps;
    private final Map<String, Object> strategyParameters;
    private final FallbackStrategy fallbackStrategy;
    private final QualityGate qualityGate;
    
    public LabExecutionStrategy(String strategyId,
                               String operationType,
                               List<LabExecutionStep> executionSteps,
                               Map<String, Object> strategyParameters,
                               FallbackStrategy fallbackStrategy,
                               QualityGate qualityGate) {
        this.strategyId = strategyId;
        this.operationType = operationType;
        this.executionSteps = executionSteps;
        this.strategyParameters = strategyParameters;
        this.fallbackStrategy = fallbackStrategy;
        this.qualityGate = qualityGate;
    }
    
    /**
     * 연구소 실행 단계
     */
    public static class LabExecutionStep {
        private final String stepId;
        private final String labType;
        private final Map<String, Object> stepParameters;
        private final List<String> dependencies;
        private final int timeoutSeconds;
        private final int retryCount;
        
        public LabExecutionStep(String stepId,
                               String labType,
                               Map<String, Object> stepParameters,
                               List<String> dependencies,
                               int timeoutSeconds,
                               int retryCount) {
            this.stepId = stepId;
            this.labType = labType;
            this.stepParameters = stepParameters;
            this.dependencies = dependencies;
            this.timeoutSeconds = timeoutSeconds;
            this.retryCount = retryCount;
        }
        
        // Getters
        public String getStepId() { return stepId; }
        public String getLabType() { return labType; }
        public Map<String, Object> getStepParameters() { return stepParameters; }
        public List<String> getDependencies() { return dependencies; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public int getRetryCount() { return retryCount; }
    }
    
    /**
     * 폴백 전략
     */
    public static class FallbackStrategy {
        private final FallbackType type;
        private final String fallbackLabType;
        private final Map<String, Object> fallbackParameters;
        
        public FallbackStrategy(FallbackType type,
                               String fallbackLabType,
                               Map<String, Object> fallbackParameters) {
            this.type = type;
            this.fallbackLabType = fallbackLabType;
            this.fallbackParameters = fallbackParameters;
        }
        
        public enum FallbackType {
            IMMEDIATE,      // 즉시 폴백
            GRADUAL,        // 점진적 폴백
            FULL_RECOVERY,  // 완전 복구
            EMERGENCY       // 긴급 모드
        }
        
        // Getters
        public FallbackType getType() { return type; }
        public String getFallbackLabType() { return fallbackLabType; }
        public Map<String, Object> getFallbackParameters() { return fallbackParameters; }
    }
    
    /**
     * 품질 게이트
     */
    public static class QualityGate {
        private final double minAccuracyThreshold;
        private final double maxResponseTimeMs;
        private final double minConfidenceScore;
        private final List<String> requiredValidations;
        
        public QualityGate(double minAccuracyThreshold,
                          double maxResponseTimeMs,
                          double minConfidenceScore,
                          List<String> requiredValidations) {
            this.minAccuracyThreshold = minAccuracyThreshold;
            this.maxResponseTimeMs = maxResponseTimeMs;
            this.minConfidenceScore = minConfidenceScore;
            this.requiredValidations = requiredValidations;
        }
        
        /**
         * 품질 기준을 통과하는지 검증
         */
        public boolean passesQualityGate(double accuracy, double responseTime, double confidence) {
            return accuracy >= minAccuracyThreshold &&
                   responseTime <= maxResponseTimeMs &&
                   confidence >= minConfidenceScore;
        }
        
        // Getters
        public double getMinAccuracyThreshold() { return minAccuracyThreshold; }
        public double getMaxResponseTimeMs() { return maxResponseTimeMs; }
        public double getMinConfidenceScore() { return minConfidenceScore; }
        public List<String> getRequiredValidations() { return requiredValidations; }
    }
    
    // Getters
    public String getStrategyId() { return strategyId; }
    public String getOperationType() { return operationType; }
    public List<LabExecutionStep> getExecutionSteps() { return executionSteps; }
    public Map<String, Object> getStrategyParameters() { return strategyParameters; }
    public FallbackStrategy getFallbackStrategy() { return fallbackStrategy; }
    public QualityGate getQualityGate() { return qualityGate; }
} 