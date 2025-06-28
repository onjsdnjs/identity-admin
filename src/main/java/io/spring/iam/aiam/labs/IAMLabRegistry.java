package io.spring.iam.aiam.labs;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ IAM 전문 연구소 레지스트리 (순환 의존성 완전 해결)
 * 
 * ✅ 완전 독립적 설계:
 * - 다른 어떤 비즈니스 컴포넌트에도 의존하지 않음
 * - 오직 Lab 등록/조회/관리만 담당
 * - 순수한 레지스트리 역할만 수행
 * 
 * 🎯 Pipeline 기반 AI-Native IAM 연구소들을 동적 통합 관리:
 * - 모든 Lab을 List로 자동 주입받아 동적 등록
 * - 클래스 이름 기반 자동 식별
 * - Lab 추가/제거시 코드 수정 불필요
 * - Pipeline 기반 표준화된 AI 처리
 */
@Slf4j
@Component
public class IAMLabRegistry {
    
    private final Map<String, Object> labs = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> labsByType = new ConcurrentHashMap<>();
    
    // ==================== 🏭 모든 Lab을 동적으로 주입받음 ====================
    private final List<Object> allLabs;
    
    @Autowired
    public IAMLabRegistry(List<Object> allLabs) {
        this.allLabs = allLabs != null ? allLabs : new ArrayList<>();
        log.info("🔬 IAMLabRegistry created with {} potential labs", this.allLabs.size());
    }
    
    /**
     * 스프링 초기화 후 모든 Lab을 자동 등록합니다
     */
    @PostConstruct
    public void initializeLabs() {
        log.info("🔬 Initializing dynamic Pipeline-based IAM Labs Registry...");
        
        // 모든 주입된 객체 중에서 Lab 으로 판단되는 것들만 필터링하여 등록
        allLabs.stream()
            .filter(this::isLabComponent)
            .forEach(this::registerLabDynamically);
        
        log.info("✅ IAM Labs Registry initialized with {} labs", labs.size());
        labs.forEach((name, lab) -> 
            log.info("  📋 Registered: {} [{}]", name, lab.getClass().getSimpleName())
        );
        
        // 등록된 Lab이 없는 경우 경고
        if (labs.isEmpty()) {
            log.warn("⚠️ No Labs were registered! Please check if Lab components are properly annotated with @Component");
        }
    }
    
    /**
     * 객체가 Lab 컴포넌트인지 판단합니다
     * @param obj 검사할 객체
     * @return Lab 여부
     */
    private boolean isLabComponent(Object obj) {
        if (obj == null) return false;
        
        String className = obj.getClass().getSimpleName();
        
        // ✅ 순환 의존성 방지: 자기 자신과 다른 레지스트리는 제외
        // LabAccessor는 ApplicationContext 사용으로 순환참조 해결됨
        if (className.equals("IAMLabRegistry") || 
            className.contains("Registry")) {
            return false;
        }
        
        // Lab으로 끝나는 클래스명을 가진 컴포넌트들을 Lab으로 인식
        boolean isLab = className.endsWith("Lab");
        
        if (isLab) {
            log.debug("🔍 Lab component detected: {}", className);
        }
        
        return isLab;
    }
    
    /**
     * Lab을 동적으로 등록합니다
     * @param lab Lab 인스턴스
     */
    private void registerLabDynamically(Object lab) {
        String className = lab.getClass().getSimpleName();
        
        // 클래스 이름을 키로 사용하여 등록
        labs.put(className, lab);
        labsByType.put(lab.getClass(), lab);
        
        log.debug("🔬 Lab registered dynamically: {} -> {}", className, lab.getClass().getName());
    }
    
    /**
     * 수동으로 연구소를 등록합니다 (필요시)
     * @param name 연구소 이름
     * @param lab 연구소 인스턴스
     */
    public void registerLab(String name, Object lab) {
        // ✅ Lab 컴포넌트 여부 체크 (LabAccessor 순환참조 해결됨)
        if (!isLabComponent(lab)) {
            log.warn("⚠️ Rejected lab registration (not a Lab component): {}", name);
            return;
        }
        
        labs.put(name, lab);
        labsByType.put(lab.getClass(), lab);
        log.debug("🔬 Lab registered manually: {} -> {}", name, lab.getClass().getSimpleName());
    }
    
    /**
     * 이름으로 연구소를 조회합니다
     * @param labName 연구소 이름 (클래스 이름)
     * @param labType 연구소 타입
     * @return 연구소 인스턴스
     */
    public <T> Optional<T> getLab(String labName, Class<T> labType) {
        Object lab = labs.get(labName);
        if (lab != null && labType.isInstance(lab)) {
            return Optional.of((T) lab);
        }
        return Optional.empty();
    }
    
    /**
     * 타입으로 연구소를 조회합니다
     * @param labType 연구소 타입
     * @return 연구소 인스턴스
     */
    public <T> Optional<T> getLab(Class<T> labType) {
        Object lab = labsByType.get(labType);
        if (lab != null) {
            return Optional.of((T) lab);
        }
        return Optional.empty();
    }
    
    /**
     * 클래스 이름으로 연구소를 조회합니다 (동적 조회)
     * @param className 클래스 이름 (예: "ConditionTemplateGenerationLab")
     * @return 연구소 인스턴스
     */
    public Optional<Object> getLabByClassName(String className) {
        return Optional.ofNullable(labs.get(className));
    }
    
    /**
     * 특정 타입의 모든 연구소를 조회합니다
     * @param baseType 기본 타입
     * @return 해당 타입의 연구소 목록
     */
    public <T> List<T> getLabsByType(Class<T> baseType) {
        return labs.values().stream()
            .filter(baseType::isInstance)
            .map(lab -> (T) lab)
            .toList();
    }
    
    /**
     * 모든 등록된 연구소 이름을 조회합니다
     * @return 연구소 이름 목록
     */
    public List<String> getAllLabNames() {
        return new ArrayList<>(labs.keySet());
    }
    
    /**
     * 모든 등록된 연구소 인스턴스를 조회합니다
     * @return 연구소 인스턴스 목록
     */
    public List<Object> getAllLabs() {
        return new ArrayList<>(labs.values());
    }
    
    /**
     * 연구소 등록 상태를 확인합니다
     * @param labName 연구소 이름
     * @return 등록 여부
     */
    public boolean isLabRegistered(String labName) {
        return labs.containsKey(labName);
    }
    
    /**
     * 타입별 연구소 등록 상태를 확인합니다
     * @param labType 연구소 타입
     * @return 등록 여부
     */
    public boolean isLabRegistered(Class<?> labType) {
        return labsByType.containsKey(labType);
    }
    
    /**
     * 등록된 연구소 수를 조회합니다
     * @return 연구소 수
     */
    public int getLabCount() {
        return labs.size();
    }
    
    /**
     * 연구소 상태 정보를 조회합니다
     * @return 연구소별 상태 정보
     */
    public Map<String, String> getLabStatus() {
        Map<String, String> status = new ConcurrentHashMap<>();
        
        labs.forEach((name, lab) -> {
            status.put(name, String.format("ACTIVE - %s [%s]", 
                lab.getClass().getSimpleName(), 
                lab.getClass().getPackage().getName()));
        });
        
        return status;
    }
    
    /**
     * 연구소 통계 정보를 조회합니다
     * @return 통계 정보
     */
    public Map<String, Object> getLabStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("totalLabs", labs.size());
        stats.put("labNames", getAllLabNames());
        stats.put("labTypes", labs.values().stream()
            .map(lab -> lab.getClass().getSimpleName())
            .distinct()
            .sorted()
            .toList());
        
        // 패키지별 분류
        Map<String, Long> packageStats = labs.values().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                lab -> lab.getClass().getPackage().getName(),
                java.util.stream.Collectors.counting()
            ));
        stats.put("packageDistribution", packageStats);
        
        // 초기화 상태
        stats.put("initialized", !labs.isEmpty());
        stats.put("lastUpdate", System.currentTimeMillis());
        
        return stats;
    }
    
    /**
     * ✅ 순환 의존성 디버깅 정보
     * @return 디버깅 정보
     */
    public Map<String, Object> getCircularDependencyDebugInfo() {
        Map<String, Object> debugInfo = new ConcurrentHashMap<>();
        
        debugInfo.put("registryClass", this.getClass().getName());
        debugInfo.put("totalInjectedObjects", allLabs.size());
        debugInfo.put("filteredLabCount", labs.size());
        debugInfo.put("rejectedObjects", allLabs.size() - labs.size());
        
        // 거부된 객체들 (Lab이 아닌 것들)
        List<String> rejectedClasses = allLabs.stream()
            .filter(obj -> !isLabComponent(obj))
            .map(obj -> obj.getClass().getSimpleName())
            .distinct()
            .sorted()
            .toList();
        debugInfo.put("rejectedClasses", rejectedClasses);
        
        return debugInfo;
    }
} 