package io.spring.iam.aiam.protocol.request;

import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.types.RiskContext;

/**
 * 리스크 관련 요청 클래스
 * 위험 분석, 모니터링, 위협 인텔리전스 등의 작업에 사용
 */
public class RiskRequest<T extends RiskContext> extends IAMRequest<T> {
    
    private String analysisType;
    private boolean realTimeMonitoring;
    private int analysisDepth;
    private double riskThreshold;
    
    public RiskRequest(T context, String operation) {
        super(context, operation);
    }
    
    public RiskRequest(T context, String operation, String analysisType) {
        super(context, operation);
        this.analysisType = analysisType;
    }
    
    // Getters and Setters
    public String getAnalysisType() { return analysisType; }
    public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }
    
    public boolean isRealTimeMonitoring() { return realTimeMonitoring; }
    public void setRealTimeMonitoring(boolean realTimeMonitoring) { this.realTimeMonitoring = realTimeMonitoring; }
    
    public int getAnalysisDepth() { return analysisDepth; }
    public void setAnalysisDepth(int analysisDepth) { this.analysisDepth = analysisDepth; }
    
    public void setAnalysisDepth(String analysisDepthStr) {
        // String을 int로 변환 (BASIC=1, DETAILED=2, COMPREHENSIVE=3)
        switch (analysisDepthStr.toUpperCase()) {
            case "BASIC": this.analysisDepth = 1; break;
            case "DETAILED": this.analysisDepth = 2; break;
            case "COMPREHENSIVE": this.analysisDepth = 3; break;
            default: this.analysisDepth = 2; // 기본값
        }
    }
    
    public double getRiskThreshold() { return riskThreshold; }
    public void setRiskThreshold(double riskThreshold) { this.riskThreshold = riskThreshold; }
    
    @Override
    public String toString() {
        return String.format("RiskRequest{operation='%s', analysisType='%s', realTime=%s}", 
                getOperation(), analysisType, realTimeMonitoring);
    }
} 