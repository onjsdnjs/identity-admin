package io.spring.iam.aiam.labs;

import io.spring.iam.aiam.labs.policy.AdvancedPolicyGenerationLab;
import io.spring.iam.aiam.labs.risk.ComprehensiveRiskAssessmentLab;
import io.spring.iam.aiam.protocol.IAMContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;

/**
 * IAM 전문 연구소 레지스트리
 * 
 * 🏛️ 세계 최고 수준의 AI-Native IAM 연구소들을 통합 관리
 * - 각 도메인별 전문 연구소 등록/조회
 * - 연구소 간 협업 조정
 * - 연구소 성능 모니터링
 * - 동적 연구소 할당
 * 
 * @param <T> IAM 컨텍스트 타입
 */
@Slf4j
@Component
public class IAMLabRegistry<T extends IAMContext> {
    
    private final Map<Class<? extends AbstractIAMLab<?>>, AbstractIAMLab<?>> labs = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends AbstractIAMLab<?>>> labsByName = new ConcurrentHashMap<>();
    
    // ==================== 🏭 전문 연구소 인스턴스들 ====================
    private final AdvancedPolicyGenerationLab policyGenerationLab;
    private final ComprehensiveRiskAssessmentLab riskAssessmentLab;
    
    @Autowired
    public IAMLabRegistry(AdvancedPolicyGenerationLab policyGenerationLab,
                         ComprehensiveRiskAssessmentLab riskAssessmentLab) {
        this.policyGenerationLab = policyGenerationLab;
        this.riskAssessmentLab = riskAssessmentLab;
    }
    
    /**
     * 스프링 초기화 후 모든 연구소를 자동 등록합니다
     */
    @PostConstruct
    public void initializeLabs() {
        log.info("🔬 Initializing IAM Labs Registry...");
        
        // 전문 연구소들 등록
        registerLab((AbstractIAMLab<T>) policyGenerationLab);
        registerLab((AbstractIAMLab<T>) riskAssessmentLab);
        
        log.info("✅ IAM Labs Registry initialized with {} labs", labs.size());
        labs.values().forEach(lab -> 
            log.info("  📋 Registered: {} [{}]", lab.getLabName(), lab.getSpecialization().getDisplayName())
        );
    }
    
    /**
     * 연구소를 등록합니다
     * @param lab 연구소 인스턴스
     */
    public <L extends AbstractIAMLab<T>> void registerLab(L lab) {
        labs.put((Class<? extends AbstractIAMLab<?>>) lab.getClass(), lab);
        labsByName.put(lab.getLabName(), (Class<? extends AbstractIAMLab<?>>) lab.getClass());
    }
    
    /**
     * 타입으로 연구소를 조회합니다
     * @param labType 연구소 타입
     * @return 연구소 인스턴스
     */
    @SuppressWarnings("unchecked")
    public <L extends AbstractIAMLab<T>> Optional<L> getLab(Class<L> labType) {
        AbstractIAMLab<?> lab = labs.get(labType);
        return lab != null ? Optional.of((L) lab) : Optional.empty();
    }
    
    /**
     * 이름으로 연구소를 조회합니다
     * @param labName 연구소 이름
     * @return 연구소 인스턴스
     */
    @SuppressWarnings("unchecked")
    public <L extends AbstractIAMLab<T>> Optional<L> getLabByName(String labName) {
        Class<? extends AbstractIAMLab<?>> labType = labsByName.get(labName);
        if (labType != null) {
            AbstractIAMLab<?> lab = labs.get(labType);
            return lab != null ? Optional.of((L) lab) : Optional.empty();
        }
        return Optional.empty();
    }
    
    /**
     * 특정 작업을 수행할 수 있는 연구소들을 조회합니다
     * @param operation 작업명
     * @return 지원 가능한 연구소 목록
     */
    public List<AbstractIAMLab<T>> getLabsForOperation(String operation) {
        return labs.values().stream()
                .filter(lab -> lab.supportsOperation(operation))
                .map(lab -> (AbstractIAMLab<T>) lab)
                .toList();
    }
    
    /**
     * 모든 등록된 연구소를 조회합니다
     * @return 연구소 목록
     */
    public List<AbstractIAMLab<T>> getAllLabs() {
        return labs.values().stream()
                .map(lab -> (AbstractIAMLab<T>) lab)
                .toList();
    }
    
    /**
     * 연구소 등록 상태를 확인합니다
     * @param labType 연구소 타입
     * @return 등록 여부
     */
    public boolean isLabRegistered(Class<? extends AbstractIAMLab<T>> labType) {
        return labs.containsKey(labType);
    }
    
    /**
     * 연구소 성능 통계를 조회합니다
     * @return 연구소별 성능 통계
     */
    public Map<String, LabMetrics> getLabMetrics() {
        Map<String, LabMetrics> metrics = new ConcurrentHashMap<>();
        
        labs.values().forEach(lab -> {
            LabMetrics labMetrics = lab.getMetrics();
            metrics.put(lab.getLabName(), labMetrics);
        });
        
        return metrics;
    }
    
    /**
     * 가장 성능이 좋은 연구소를 조회합니다
     * @param operation 작업명
     * @return 최적 연구소
     */
    public Optional<AbstractIAMLab<T>> getBestPerformingLab(String operation) {
        return getLabsForOperation(operation).stream()
                .max((lab1, lab2) -> {
                    double score1 = calculatePerformanceScore(lab1);
                    double score2 = calculatePerformanceScore(lab2);
                    return Double.compare(score1, score2);
                });
    }
    
    /**
     * 연구소 성능 점수를 계산합니다
     * @param lab 연구소
     * @return 성능 점수 (0.0 ~ 1.0)
     */
    private double calculatePerformanceScore(AbstractIAMLab<T> lab) {
        LabMetrics metrics = lab.getMetrics();
        
        // 성공률 (40%) + 평균 응답시간 (30%) + 처리량 (30%)
        double successRate = metrics.getSuccessRate();
        double responseTimeScore = Math.max(0, 1.0 - (metrics.getAverageResponseTime() / 10000.0)); // 10초 기준
        double throughputScore = Math.min(1.0, metrics.getThroughput() / 100.0); // 100 req/s 기준
        
        return (successRate * 0.4) + (responseTimeScore * 0.3) + (throughputScore * 0.3);
    }
    
    /**
     * 등록된 연구소 수를 조회합니다
     * @return 연구소 수
     */
    public int getLabCount() {
        return labs.size();
    }
} 