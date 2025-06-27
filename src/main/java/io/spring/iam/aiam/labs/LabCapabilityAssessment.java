package io.spring.iam.aiam.labs;

import java.util.List;
import java.util.Map;

/**
 * 🔬 IAM 연구소 역량 평가
 * 
 * 연구소의 현재 역량을 종합적으로 평가
 */
public class LabCapabilityAssessment {
    
    private final String labId;
    private final String labName;
    private final LabSpecialization specialization;
    private final double overallScore; // 전체 점수 (0.0 ~ 100.0)
    private final Map<AssessmentCategory, CategoryScore> categoryScores;
    private final List<String> strengths; // 강점 목록
    private final List<String> weaknesses; // 약점 목록
    private final List<String> recommendations; // 개선 권장사항
    private final AssessmentLevel assessmentLevel;
    private final long assessmentTime;
    
    public LabCapabilityAssessment(String labId, String labName, LabSpecialization specialization,
                                 double overallScore, Map<AssessmentCategory, CategoryScore> categoryScores,
                                 List<String> strengths, List<String> weaknesses,
                                 List<String> recommendations, AssessmentLevel assessmentLevel) {
        this.labId = labId;
        this.labName = labName;
        this.specialization = specialization;
        this.overallScore = overallScore;
        this.categoryScores = categoryScores;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.recommendations = recommendations;
        this.assessmentLevel = assessmentLevel;
        this.assessmentTime = System.currentTimeMillis();
    }
    
    /**
     * 평가 카테고리
     */
    public enum AssessmentCategory {
        TECHNICAL_CAPABILITY("Technical Capability", "기술적 역량", 25),
        PERFORMANCE("Performance", "성능", 20),
        RELIABILITY("Reliability", "신뢰성", 20),
        SCALABILITY("Scalability", "확장성", 15),
        SECURITY("Security", "보안", 10),
        MAINTAINABILITY("Maintainability", "유지보수성", 10);
        
        private final String displayName;
        private final String description;
        private final double weight; // 가중치 (%)
        
        AssessmentCategory(String displayName, String description, double weight) {
            this.displayName = displayName;
            this.description = description;
            this.weight = weight;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public double getWeight() { return weight; }
    }
    
    /**
     * 카테고리별 점수
     */
    public static class CategoryScore {
        private final double score; // 점수 (0.0 ~ 100.0)
        private final String comment; // 평가 코멘트
        private final List<String> details; // 상세 내용
        
        public CategoryScore(double score, String comment, List<String> details) {
            this.score = score;
            this.comment = comment;
            this.details = details;
        }
        
        public double getScore() { return score; }
        public String getComment() { return comment; }
        public List<String> getDetails() { return details; }
        
        public ScoreLevel getScoreLevel() {
            if (score >= 90) return ScoreLevel.EXCELLENT;
            if (score >= 80) return ScoreLevel.GOOD;
            if (score >= 70) return ScoreLevel.AVERAGE;
            if (score >= 60) return ScoreLevel.BELOW_AVERAGE;
            return ScoreLevel.POOR;
        }
    }
    
    /**
     * 점수 레벨
     */
    public enum ScoreLevel {
        EXCELLENT(90, "Excellent", "🟢"),
        GOOD(80, "Good", "🟡"),
        AVERAGE(70, "Average", "🟠"),
        BELOW_AVERAGE(60, "Below Average", "🔴"),
        POOR(0, "Poor", "⚫");
        
        private final double threshold;
        private final String displayName;
        private final String emoji;
        
        ScoreLevel(double threshold, String displayName, String emoji) {
            this.threshold = threshold;
            this.displayName = displayName;
            this.emoji = emoji;
        }
        
        public double getThreshold() { return threshold; }
        public String getDisplayName() { return displayName; }
        public String getEmoji() { return emoji; }
    }
    
    /**
     * 평가 레벨
     */
    public enum AssessmentLevel {
        WORLD_CLASS("World Class", "세계 최고 수준", 95),
        EXCELLENT("Excellent", "우수", 85),
        GOOD("Good", "양호", 75),
        SATISFACTORY("Satisfactory", "보통", 65),
        NEEDS_IMPROVEMENT("Needs Improvement", "개선 필요", 50),
        CRITICAL("Critical", "심각한 문제", 0);
        
        private final String displayName;
        private final String description;
        private final double minScore;
        
        AssessmentLevel(String displayName, String description, double minScore) {
            this.displayName = displayName;
            this.description = description;
            this.minScore = minScore;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public double getMinScore() { return minScore; }
        
        public static AssessmentLevel fromScore(double score) {
            if (score >= WORLD_CLASS.minScore) return WORLD_CLASS;
            if (score >= EXCELLENT.minScore) return EXCELLENT;
            if (score >= GOOD.minScore) return GOOD;
            if (score >= SATISFACTORY.minScore) return SATISFACTORY;
            if (score >= NEEDS_IMPROVEMENT.minScore) return NEEDS_IMPROVEMENT;
            return CRITICAL;
        }
    }
    
    /**
     * 특정 카테고리의 점수를 반환합니다
     */
    public CategoryScore getCategoryScore(AssessmentCategory category) {
        return categoryScores.get(category);
    }
    
    /**
     * 가장 강한 카테고리를 반환합니다
     */
    public AssessmentCategory getStrongestCategory() {
        return categoryScores.entrySet().stream()
                .max(Map.Entry.comparingByValue((cs1, cs2) -> Double.compare(cs1.getScore(), cs2.getScore())))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    /**
     * 가장 약한 카테고리를 반환합니다
     */
    public AssessmentCategory getWeakestCategory() {
        return categoryScores.entrySet().stream()
                .min(Map.Entry.comparingByValue((cs1, cs2) -> Double.compare(cs1.getScore(), cs2.getScore())))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    /**
     * 연구소가 특정 작업에 적합한지 평가합니다
     */
    public boolean isSuitableFor(String operation, double requiredScore) {
        return overallScore >= requiredScore && 
               specialization.canHandle(operation);
    }
    
    /**
     * 개선이 필요한 영역을 반환합니다
     */
    public List<AssessmentCategory> getImprovementAreas() {
        return categoryScores.entrySet().stream()
                .filter(entry -> entry.getValue().getScore() < 70.0)
                .map(Map.Entry::getKey)
                .toList();
    }
    
    /**
     * 평가 요약을 포맷합니다
     */
    public String formatSummary() {
        return String.format(
            "🔬 %s Lab Capability Assessment\n" +
            "Overall Score: %.1f/100 (%s)\n" +
            "Specialization: %s\n" +
            "\n📊 Category Scores:\n%s\n" +
            "\n💪 Strengths:\n%s\n" +
            "\n⚠️ Weaknesses:\n%s\n" +
            "\n💡 Recommendations:\n%s",
            labName, overallScore, assessmentLevel.getDisplayName(),
            specialization.getDisplayName(),
            formatCategoryScores(),
            formatList(strengths, "• "),
            formatList(weaknesses, "• "),
            formatList(recommendations, "• ")
        );
    }
    
    private String formatCategoryScores() {
        return categoryScores.entrySet().stream()
                .map(entry -> String.format("  %s %s: %.1f/100",
                        entry.getValue().getScoreLevel().getEmoji(),
                        entry.getKey().getDisplayName(),
                        entry.getValue().getScore()))
                .reduce((s1, s2) -> s1 + "\n" + s2)
                .orElse("No category scores available");
    }
    
    private String formatList(List<String> items, String prefix) {
        return items.stream()
                .map(item -> prefix + item)
                .reduce((s1, s2) -> s1 + "\n" + s2)
                .orElse("None");
    }
    
    /**
     * JSON 형태로 변환합니다
     */
    public String toJson() {
        return String.format("""
            {
                "labId": "%s",
                "labName": "%s",
                "specialization": "%s",
                "overallScore": %.2f,
                "assessmentLevel": "%s",
                "assessmentTime": %d,
                "categoryScores": {%s},
                "strengths": [%s],
                "weaknesses": [%s],
                "recommendations": [%s],
                "strongestCategory": "%s",
                "weakestCategory": "%s"
            }""",
            labId, labName, specialization.name(),
            overallScore, assessmentLevel.name(), assessmentTime,
            formatCategoryScoresJson(),
            formatListJson(strengths),
            formatListJson(weaknesses),
            formatListJson(recommendations),
            getStrongestCategory() != null ? getStrongestCategory().name() : "null",
            getWeakestCategory() != null ? getWeakestCategory().name() : "null"
        );
    }
    
    private String formatCategoryScoresJson() {
        return categoryScores.entrySet().stream()
                .map(entry -> String.format("\"%s\": %.2f", 
                        entry.getKey().name(), entry.getValue().getScore()))
                .reduce((s1, s2) -> s1 + ", " + s2)
                .orElse("");
    }
    
    private String formatListJson(List<String> items) {
        return items.stream()
                .map(item -> "\"" + item + "\"")
                .reduce((s1, s2) -> s1 + ", " + s2)
                .orElse("");
    }
    
    // ==================== Getters ====================
    
    public String getLabId() { return labId; }
    public String getLabName() { return labName; }
    public LabSpecialization getSpecialization() { return specialization; }
    public double getOverallScore() { return overallScore; }
    public Map<AssessmentCategory, CategoryScore> getCategoryScores() { return categoryScores; }
    public List<String> getStrengths() { return strengths; }
    public List<String> getWeaknesses() { return weaknesses; }
    public List<String> getRecommendations() { return recommendations; }
    public AssessmentLevel getAssessmentLevel() { return assessmentLevel; }
    public long getAssessmentTime() { return assessmentTime; }
} 