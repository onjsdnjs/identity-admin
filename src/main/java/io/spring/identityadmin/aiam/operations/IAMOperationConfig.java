package io.spring.identityadmin.aiam.operations;

import io.spring.identityadmin.aiam.protocol.request.PolicyRequest;
import io.spring.identityadmin.aiam.protocol.types.PolicyContext;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * IAM Operations 설정 클래스
 * 
 * 🎯 하드코딩 방지를 위한 외부 설정 관리
 * - application.yml에서 설정값 주입
 * - 런타임 설정 변경 가능
 */
@Component
@ConfigurationProperties(prefix = "app.iam.operations")
public class IAMOperationConfig {
    
    // ==================== 감사 설정 ====================
    private boolean auditLoggingEnabled = true;
    
    // ==================== 보안 설정 ====================
    private boolean securityValidationEnabled = true;
    
    // ==================== 정책 설정 ====================
    private boolean policyValidationEnabled = true;
    
    // ==================== 스트리밍 설정 ====================
    private int streamingThreshold = 100;
    
    // ==================== 위험 분석 설정 ====================
    private double riskThreshold = 0.7;
    private String riskAnalysisDepth = "COMPREHENSIVE";
    private long monitoringIntervalMs = 5000L;
    
    // ==================== 충돌 감지 설정 ====================
    private double conflictSensitivity = 0.8;
    
    // ==================== 추천 설정 ====================
    private int maxRecommendations = 5;
    private double minConfidenceThreshold = 0.6;
    
    // ==================== 분석 설정 ====================
    private String userAnalysisDepth = "DETAILED";
    private String optimizationLevel = "BALANCED";
    
    // ==================== 검증 설정 ====================
    private boolean strictValidationMode = false;
    
    // ==================== 로그 분석 설정 ====================
    private int logAnalysisBatchSize = 1000;
    private long logAnalysisTimeoutSeconds = 300L;
    
    // ==================== 복잡도 계산기 ====================
    private ComplexityCalculator complexityCalculator = new DefaultComplexityCalculator();
    
    // ==================== Getters and Setters ====================
    
    public boolean isAuditLoggingEnabled() {
        return auditLoggingEnabled;
    }
    
    public void setAuditLoggingEnabled(boolean auditLoggingEnabled) {
        this.auditLoggingEnabled = auditLoggingEnabled;
    }
    
    public boolean isSecurityValidationEnabled() {
        return securityValidationEnabled;
    }
    
    public void setSecurityValidationEnabled(boolean securityValidationEnabled) {
        this.securityValidationEnabled = securityValidationEnabled;
    }
    
    public boolean isPolicyValidationEnabled() {
        return policyValidationEnabled;
    }
    
    public void setPolicyValidationEnabled(boolean policyValidationEnabled) {
        this.policyValidationEnabled = policyValidationEnabled;
    }
    
    public int getStreamingThreshold() {
        return streamingThreshold;
    }
    
    public void setStreamingThreshold(int streamingThreshold) {
        this.streamingThreshold = streamingThreshold;
    }
    
    public double getRiskThreshold() {
        return riskThreshold;
    }
    
    public void setRiskThreshold(double riskThreshold) {
        this.riskThreshold = riskThreshold;
    }
    
    public String getRiskAnalysisDepth() {
        return riskAnalysisDepth;
    }
    
    public void setRiskAnalysisDepth(String riskAnalysisDepth) {
        this.riskAnalysisDepth = riskAnalysisDepth;
    }
    
    public long getMonitoringIntervalMs() {
        return monitoringIntervalMs;
    }
    
    public void setMonitoringIntervalMs(long monitoringIntervalMs) {
        this.monitoringIntervalMs = monitoringIntervalMs;
    }
    
    public double getConflictSensitivity() {
        return conflictSensitivity;
    }
    
    public void setConflictSensitivity(double conflictSensitivity) {
        this.conflictSensitivity = conflictSensitivity;
    }
    
    public int getMaxRecommendations() {
        return maxRecommendations;
    }
    
    public void setMaxRecommendations(int maxRecommendations) {
        this.maxRecommendations = maxRecommendations;
    }
    
    public double getMinConfidenceThreshold() {
        return minConfidenceThreshold;
    }
    
    public void setMinConfidenceThreshold(double minConfidenceThreshold) {
        this.minConfidenceThreshold = minConfidenceThreshold;
    }
    
    public String getUserAnalysisDepth() {
        return userAnalysisDepth;
    }
    
    public void setUserAnalysisDepth(String userAnalysisDepth) {
        this.userAnalysisDepth = userAnalysisDepth;
    }
    
    public String getOptimizationLevel() {
        return optimizationLevel;
    }
    
    public void setOptimizationLevel(String optimizationLevel) {
        this.optimizationLevel = optimizationLevel;
    }
    
    public boolean isStrictValidationMode() {
        return strictValidationMode;
    }
    
    public void setStrictValidationMode(boolean strictValidationMode) {
        this.strictValidationMode = strictValidationMode;
    }
    
    public int getLogAnalysisBatchSize() {
        return logAnalysisBatchSize;
    }
    
    public void setLogAnalysisBatchSize(int logAnalysisBatchSize) {
        this.logAnalysisBatchSize = logAnalysisBatchSize;
    }
    
    public long getLogAnalysisTimeoutSeconds() {
        return logAnalysisTimeoutSeconds;
    }
    
    public void setLogAnalysisTimeoutSeconds(long logAnalysisTimeoutSeconds) {
        this.logAnalysisTimeoutSeconds = logAnalysisTimeoutSeconds;
    }
    
    public ComplexityCalculator getComplexityCalculator() {
        return complexityCalculator;
    }
    
    public void setComplexityCalculator(ComplexityCalculator complexityCalculator) {
        this.complexityCalculator = complexityCalculator;
    }
    
    // ==================== Inner Interfaces ====================
    
    /**
     * 요청 복잡도 계산 인터페이스
     */
    public interface ComplexityCalculator {
        int calculate(PolicyRequest<PolicyContext> request);
    }
    
    /**
     * 기본 복잡도 계산기
     */
    public static class DefaultComplexityCalculator implements ComplexityCalculator {
        @Override
        public int calculate(PolicyRequest<PolicyContext> request) {
            // 기본 복잡도 계산 로직
            int complexity = 1;
            
            PolicyContext context = request.getContext();
            if (context != null) {
                // 정책 컨텍스트 기반 복잡도 계산
                complexity += context.getTargetEntities().size() * 10;
                complexity += context.getConditions().size() * 5;
                
                if (context.isRequireAdvancedAnalysis()) {
                    complexity += 50;
                }
            }
            
            return complexity;
        }
    }
} 