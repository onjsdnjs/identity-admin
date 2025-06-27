package io.spring.iam.aiam.labs;

import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;

import java.util.List;
import java.util.Map;

/**
 * 🔬 IAM 연구소 협업 전략
 * 
 * 여러 연구소가 협력하여 복잡한 작업을 수행하는 전략을 정의
 */
public class CollaborationStrategy {
    
    private final String strategyId;
    private final String strategyName;
    private final CollaborationType type;
    private final List<AbstractIAMLab<?>> participatingLabs;
    private final Map<String, Object> strategyParameters;
    private final WorkDistributionPlan distributionPlan;
    private final ResultSynthesisMethod synthesisMethod;
    
    public CollaborationStrategy(String strategyId, String strategyName, CollaborationType type,
                               List<AbstractIAMLab<?>> participatingLabs,
                               Map<String, Object> strategyParameters,
                               WorkDistributionPlan distributionPlan,
                               ResultSynthesisMethod synthesisMethod) {
        this.strategyId = strategyId;
        this.strategyName = strategyName;
        this.type = type;
        this.participatingLabs = participatingLabs;
        this.strategyParameters = strategyParameters;
        this.distributionPlan = distributionPlan;
        this.synthesisMethod = synthesisMethod;
    }
    
    /**
     * 협업 타입
     */
    public enum CollaborationType {
        PARALLEL("Parallel", "병렬 처리", "각 연구소가 독립적으로 작업 수행"),
        SEQUENTIAL("Sequential", "순차 처리", "연구소들이 순서대로 작업 수행"),
        HIERARCHICAL("Hierarchical", "계층적 처리", "주 연구소와 보조 연구소로 구분"),
        CONSENSUS("Consensus", "합의 기반", "모든 연구소의 합의를 통한 결과 도출"),
        COMPETITIVE("Competitive", "경쟁 기반", "여러 연구소가 경쟁하여 최고 결과 선택"),
        HYBRID("Hybrid", "하이브리드", "여러 협업 방식을 조합");
        
        private final String displayName;
        private final String description;
        private final String explanation;
        
        CollaborationType(String displayName, String description, String explanation) {
            this.displayName = displayName;
            this.description = description;
            this.explanation = explanation;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getExplanation() { return explanation; }
    }
    
    /**
     * 작업 분배 계획
     */
    public static class WorkDistributionPlan {
        private final Map<AbstractIAMLab<?>, WorkAssignment> assignments;
        private final List<WorkDependency> dependencies;
        private final long estimatedTotalTime;
        
        public WorkDistributionPlan(Map<AbstractIAMLab<?>, WorkAssignment> assignments,
                                  List<WorkDependency> dependencies,
                                  long estimatedTotalTime) {
            this.assignments = assignments;
            this.dependencies = dependencies;
            this.estimatedTotalTime = estimatedTotalTime;
        }
        
        public Map<AbstractIAMLab<?>, WorkAssignment> getAssignments() { return assignments; }
        public List<WorkDependency> getDependencies() { return dependencies; }
        public long getEstimatedTotalTime() { return estimatedTotalTime; }
    }
    
    /**
     * 작업 할당
     */
    public static class WorkAssignment {
        private final String taskId;
        private final String taskDescription;
        private final Map<String, Object> taskParameters;
        private final int priority;
        private final long estimatedTime;
        
        public WorkAssignment(String taskId, String taskDescription,
                            Map<String, Object> taskParameters,
                            int priority, long estimatedTime) {
            this.taskId = taskId;
            this.taskDescription = taskDescription;
            this.taskParameters = taskParameters;
            this.priority = priority;
            this.estimatedTime = estimatedTime;
        }
        
        public String getTaskId() { return taskId; }
        public String getTaskDescription() { return taskDescription; }
        public Map<String, Object> getTaskParameters() { return taskParameters; }
        public int getPriority() { return priority; }
        public long getEstimatedTime() { return estimatedTime; }
    }
    
    /**
     * 작업 의존성
     */
    public static class WorkDependency {
        private final String prerequisiteTaskId;
        private final String dependentTaskId;
        private final DependencyType type;
        
        public WorkDependency(String prerequisiteTaskId, String dependentTaskId, DependencyType type) {
            this.prerequisiteTaskId = prerequisiteTaskId;
            this.dependentTaskId = dependentTaskId;
            this.type = type;
        }
        
        public enum DependencyType {
            HARD("Hard", "필수 의존성"),
            SOFT("Soft", "선택적 의존성"),
            DATA_FLOW("Data Flow", "데이터 흐름 의존성");
            
            private final String displayName;
            private final String description;
            
            DependencyType(String displayName, String description) {
                this.displayName = displayName;
                this.description = description;
            }
            
            public String getDisplayName() { return displayName; }
            public String getDescription() { return description; }
        }
        
        public String getPrerequisiteTaskId() { return prerequisiteTaskId; }
        public String getDependentTaskId() { return dependentTaskId; }
        public DependencyType getType() { return type; }
    }
    
    /**
     * 결과 통합 방법
     */
    public enum ResultSynthesisMethod {
        MERGE("Merge", "결과 병합", "모든 결과를 통합하여 하나의 결과 생성"),
        VOTE("Vote", "다수결", "가장 많은 연구소가 지지하는 결과 선택"),
        WEIGHTED_AVERAGE("Weighted Average", "가중 평균", "연구소별 가중치를 적용한 평균"),
        BEST_RESULT("Best Result", "최고 결과", "성능이 가장 좋은 결과 선택"),
        CONSENSUS("Consensus", "합의", "모든 연구소가 합의한 결과"),
        CUSTOM("Custom", "사용자 정의", "특별한 통합 로직 적용");
        
        private final String displayName;
        private final String description;
        private final String explanation;
        
        ResultSynthesisMethod(String displayName, String description, String explanation) {
            this.displayName = displayName;
            this.description = description;
            this.explanation = explanation;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getExplanation() { return explanation; }
    }
    
    /**
     * 기본 협업 전략 생성
     */
    public static CollaborationStrategy createDefault(List<AbstractIAMLab<?>> collaborators, 
                                                    IAMRequest<?> request) {
        String strategyId = "collab-" + System.currentTimeMillis();
        String strategyName = "Default Collaboration Strategy";
        
        // 기본적으로 병렬 처리 방식 사용
        CollaborationType type = CollaborationType.PARALLEL;
        
        // 작업 분배 계획 생성
        WorkDistributionPlan distributionPlan = createDefaultDistributionPlan(collaborators);
        
        // 기본 결과 통합 방법
        ResultSynthesisMethod synthesisMethod = ResultSynthesisMethod.BEST_RESULT;
        
        return new CollaborationStrategy(
            strategyId, strategyName, type, collaborators,
            Map.of("requestType", request.getClass().getSimpleName()),
            distributionPlan, synthesisMethod
        );
    }
    
    /**
     * 작업 분배
     */
    public <T extends IAMContext> Map<AbstractIAMLab<T>, IAMRequest<T>> distributeWork(
            List<AbstractIAMLab<T>> collaborators, IAMRequest<T> request) {
        
        // 기본 구현: 모든 연구소에 동일한 요청 분배
        Map<AbstractIAMLab<T>, IAMRequest<T>> distribution = new java.util.HashMap<>();
        
        for (AbstractIAMLab<T> lab : collaborators) {
            distribution.put(lab, request);
        }
        
        return distribution;
    }
    
    /**
     * 협업 효율성 평가
     */
    public double evaluateEfficiency() {
        // 참여 연구소 수, 작업 복잡도, 예상 시간 등을 고려한 효율성 점수
        double labCountScore = Math.min(1.0, participatingLabs.size() / 5.0); // 5개 연구소 기준
        double typeScore = getTypeEfficiencyScore();
        double distributionScore = evaluateDistributionEfficiency();
        
        return (labCountScore * 0.3) + (typeScore * 0.4) + (distributionScore * 0.3);
    }
    
    private double getTypeEfficiencyScore() {
        return switch (type) {
            case PARALLEL -> 0.9;
            case COMPETITIVE -> 0.8;
            case CONSENSUS -> 0.7;
            case HIERARCHICAL -> 0.75;
            case SEQUENTIAL -> 0.6;
            case HYBRID -> 0.85;
        };
    }
    
    private double evaluateDistributionEfficiency() {
        // 작업 분배의 균형성과 의존성을 고려한 효율성 평가
        return 0.8; // 기본값
    }
    
    private static WorkDistributionPlan createDefaultDistributionPlan(List<AbstractIAMLab<?>> collaborators) {
        Map<AbstractIAMLab<?>, WorkAssignment> assignments = new java.util.HashMap<>();
        
        for (int i = 0; i < collaborators.size(); i++) {
            AbstractIAMLab<?> lab = collaborators.get(i);
            WorkAssignment assignment = new WorkAssignment(
                "task-" + i,
                "Research task for " + lab.getLabName(),
                Map.of("labId", lab.getLabId()),
                1, // priority
                5000 // estimated time in ms
            );
            assignments.put(lab, assignment);
        }
        
        return new WorkDistributionPlan(assignments, List.of(), 5000);
    }
    
    // ==================== Getters ====================
    
    public String getStrategyId() { return strategyId; }
    public String getStrategyName() { return strategyName; }
    public CollaborationType getType() { return type; }
    public List<AbstractIAMLab<?>> getParticipatingLabs() { return participatingLabs; }
    public Map<String, Object> getStrategyParameters() { return strategyParameters; }
    public WorkDistributionPlan getDistributionPlan() { return distributionPlan; }
    public ResultSynthesisMethod getSynthesisMethod() { return synthesisMethod; }
} 