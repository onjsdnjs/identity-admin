package io.spring.iam.aiam.protocol.request;

import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.types.UserContext;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 분석 요청 클래스
 * 사용자 권한, 행동 패턴, 위험도 등을 분석하기 위한 요청
 */
public class UserAnalysisRequest<T extends UserContext> extends IAMRequest<T> {
    
    private String targetUserId;
    private String analysisType;
    private LocalDateTime analysisStartDate;
    private LocalDateTime analysisEndDate;
    private List<String> focusAreas;
    private boolean includeRecommendations;
    private boolean includeBehaviorPatterns;
    private boolean includeHistoricalData;
    private String analysisDepth;
    
    public UserAnalysisRequest(T context) {
        super(context, "USER_ANALYSIS");
        this.includeHistoricalData = true;
        this.analysisType = "COMPREHENSIVE";
        this.analysisDepth = "DETAILED";
        this.includeRecommendations = true;
        this.includeBehaviorPatterns = true;
        this.analysisEndDate = LocalDateTime.now();
        this.analysisStartDate = analysisEndDate.minusDays(30); // 기본 30일
    }
    
    // Getters and Setters
    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    
    public String getAnalysisType() { return analysisType; }
    public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }
    
    public LocalDateTime getAnalysisStartDate() { return analysisStartDate; }
    public void setAnalysisStartDate(LocalDateTime analysisStartDate) { this.analysisStartDate = analysisStartDate; }
    
    public LocalDateTime getAnalysisEndDate() { return analysisEndDate; }
    public void setAnalysisEndDate(LocalDateTime analysisEndDate) { this.analysisEndDate = analysisEndDate; }
    
    public List<String> getFocusAreas() { return focusAreas; }
    public void setFocusAreas(List<String> focusAreas) { this.focusAreas = focusAreas; }
    
    public boolean isIncludeRecommendations() { return includeRecommendations; }
    public void setIncludeRecommendations(boolean includeRecommendations) { this.includeRecommendations = includeRecommendations; }
    
    public boolean isIncludeBehaviorPatterns() { return includeBehaviorPatterns; }
    public void setIncludeBehaviorPatterns(boolean includeBehaviorPatterns) { this.includeBehaviorPatterns = includeBehaviorPatterns; }
    
    public boolean isIncludeHistoricalData() { return includeHistoricalData; }
    public void setIncludeHistoricalData(boolean includeHistoricalData) { this.includeHistoricalData = includeHistoricalData; }
    
    public String getAnalysisDepth() { return analysisDepth; }
    public void setAnalysisDepth(String analysisDepth) { this.analysisDepth = analysisDepth; }
    
    @Override
    public String toString() {
        return String.format("UserAnalysisRequest{user='%s', type='%s', period=%s to %s, depth='%s', historical=%s}", 
                targetUserId, analysisType, analysisStartDate, analysisEndDate, analysisDepth, includeHistoricalData);
    }
} 