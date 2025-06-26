package io.spring.identityadmin.aiam.protocol.request;

import io.spring.identityadmin.aiam.protocol.IAMRequest;
import io.spring.identityadmin.aiam.protocol.types.UserContext;
import io.spring.identityadmin.aiam.protocol.enums.UserAnalysisType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 분석 요청 클래스
 * 사용자 행동, 권한, 위험도 분석을 위한 요청
 */
public class UserAnalysisRequest<T extends UserContext> extends IAMRequest<T> {
    
    private String targetUserId;
    private UserAnalysisType analysisType;
    private LocalDateTime analysisStartDate;
    private LocalDateTime analysisEndDate;
    private List<String> focusAreas;
    private boolean includeRecommendations;
    private boolean includeBehaviorPatterns;
    
    public UserAnalysisRequest(T context) {
        super(context, "USER_ANALYSIS");
        this.analysisType = UserAnalysisType.COMPREHENSIVE_ANALYSIS;
        this.includeRecommendations = true;
        this.includeBehaviorPatterns = true;
        this.analysisEndDate = LocalDateTime.now();
        this.analysisStartDate = analysisEndDate.minusDays(30); // 기본 30일
    }
    
    // Getters and Setters
    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    
    public UserAnalysisType getAnalysisType() { return analysisType; }
    public void setAnalysisType(UserAnalysisType analysisType) { this.analysisType = analysisType; }
    
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
    
    @Override
    public String toString() {
        return String.format("UserAnalysisRequest{user='%s', type=%s, period=%s to %s}", 
                targetUserId, analysisType, analysisStartDate, analysisEndDate);
    }
} 