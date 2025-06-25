package io.spring.identityadmin.resource.service;

import io.spring.identityadmin.domain.entity.ConditionTemplate;
import io.spring.identityadmin.domain.entity.ManagedResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 🔄 2단계: 조건과 리소스/메서드 간의 호환성을 검사하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConditionCompatibilityService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("#(\\w+)");
    
    /**
     * 호환성 검사 결과를 담는 DTO
     */
    public static class CompatibilityResult {
        private final boolean compatible;
        private final String reason;
        private final Set<String> missingVariables;
        private final Set<String> availableVariables;
        private final ConditionTemplate.ConditionClassification classification;

        public CompatibilityResult(boolean compatible, String reason, 
                                 Set<String> missingVariables, Set<String> availableVariables,
                                 ConditionTemplate.ConditionClassification classification) {
            this.compatible = compatible;
            this.reason = reason;
            this.missingVariables = missingVariables != null ? missingVariables : new HashSet<>();
            this.availableVariables = availableVariables != null ? availableVariables : new HashSet<>();
            this.classification = classification;
        }

        // Getters
        public boolean isCompatible() { return compatible; }
        public String getReason() { return reason; }
        public Set<String> getMissingVariables() { return missingVariables; }
        public Set<String> getAvailableVariables() { return availableVariables; }
        public ConditionTemplate.ConditionClassification getClassification() { return classification; }
    }
    
    /**
     * 조건 템플릿과 관리 리소스 간의 호환성을 검사합니다.
     */
    public CompatibilityResult checkCompatibility(ConditionTemplate condition, ManagedResource resource) {
        if (condition == null || resource == null) {
            return new CompatibilityResult(false, "조건 또는 리소스가 null입니다.", 
                null, null, ConditionTemplate.ConditionClassification.CUSTOM_COMPLEX);
        }

        // 1. 범용 조건은 항상 호환 가능
        if (ConditionTemplate.ConditionClassification.UNIVERSAL.equals(condition.getClassification())) {
            return new CompatibilityResult(true, "범용 조건으로 모든 리소스와 호환됩니다.", 
                new HashSet<>(), getAllUniversalVariables(), condition.getClassification());
        }

        // 2. 필요한 변수 추출
        Set<String> requiredVariables = extractVariablesFromSpel(condition.getSpelTemplate());
        
        // 3. 리소스에서 사용 가능한 변수 계산
        Set<String> availableVariables = calculateAvailableVariables(resource);
        
        // 4. 호환성 검사
        return performCompatibilityCheck(condition, requiredVariables, availableVariables);
    }
    
    /**
     * SpEL 템플릿에서 변수들을 추출합니다.
     */
    private Set<String> extractVariablesFromSpel(String spelTemplate) {
        if (spelTemplate == null || spelTemplate.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        Set<String> variables = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(spelTemplate);
        
        while (matcher.find()) {
            variables.add("#" + matcher.group(1)); // # 포함하여 저장
        }
        
        return variables;
    }
    
    /**
     * 리소스에서 사용 가능한 변수들을 계산합니다.
     */
    private Set<String> calculateAvailableVariables(ManagedResource resource) {
        Set<String> variables = new HashSet<>();
        
        // 1. 전역적으로 항상 사용 가능한 변수들
        variables.addAll(getAllUniversalVariables());
        
        // 2. 메서드 타입별 특정 변수들
        if (ManagedResource.ResourceType.METHOD.equals(resource.getResourceType())) {
            // 반환 타입이 있는 경우 #returnObject 사용 가능
            if (hasReturnObject(resource)) {
                variables.add("#returnObject");
            }
            
            // 파라미터들에서 사용 가능한 변수 추출
            variables.addAll(extractParameterVariables(resource));
        }
        
        // 3. URL 타입의 경우
        if (ManagedResource.ResourceType.URL.equals(resource.getResourceType())) {
            variables.add("#request");
            variables.add("#httpMethod");
        }
        
        return variables;
    }
    
    /**
     * 범용적으로 사용 가능한 변수들
     */
    private Set<String> getAllUniversalVariables() {
        return Set.of(
            "#authentication",
            "#ai",
            "#request",
            "#session",
            "#currentTime",
            "#isBusinessHours",
            "#clientIp"
        );
    }
    
    /**
     * 리소스가 반환 객체를 가지는지 확인
     */
    private boolean hasReturnObject(ManagedResource resource) {
        return resource.getReturnType() != null && 
               !resource.getReturnType().equals("void") &&
               !resource.getReturnType().equals("java.lang.Void");
    }
    
    /**
     * 메서드 파라미터에서 변수들을 추출
     */
    private Set<String> extractParameterVariables(ManagedResource resource) {
        Set<String> variables = new HashSet<>();
        
        try {
            // parameterTypes JSON에서 파라미터 정보 추출
            // 예: [{"name":"userId","type":"Long"}, {"name":"document","type":"Document"}]
            if (resource.getParameterTypes() != null) {
                // 간단한 파라미터명 추출 (실제로는 JSON 파싱 필요)
                String paramTypes = resource.getParameterTypes();
                if (paramTypes.contains("userId")) variables.add("#userId");
                if (paramTypes.contains("documentId")) variables.add("#documentId");
                if (paramTypes.contains("groupId")) variables.add("#groupId");
                if (paramTypes.contains("id")) variables.add("#id");
                // 기타 일반적인 파라미터들...
            }
        } catch (Exception e) {
            log.warn("파라미터 변수 추출 중 오류: {}", e.getMessage());
        }
        
        return variables;
    }
    
    /**
     * 실제 호환성 검사를 수행합니다.
     */
    private CompatibilityResult performCompatibilityCheck(ConditionTemplate condition, 
                                                        Set<String> requiredVariables, 
                                                        Set<String> availableVariables) {
        
        // 누락된 변수 계산
        Set<String> missingVariables = new HashSet<>(requiredVariables);
        missingVariables.removeAll(availableVariables);
        
        if (missingVariables.isEmpty()) {
            // 모든 변수가 사용 가능한 경우
            String reason = String.format("모든 필요 변수(%s)가 사용 가능합니다.", 
                requiredVariables.isEmpty() ? "없음" : String.join(", ", requiredVariables));
            return new CompatibilityResult(true, reason, missingVariables, availableVariables, 
                condition.getClassification());
        } else {
            // 누락된 변수가 있는 경우
            String reason = String.format("필요한 변수가 누락되었습니다: %s", 
                String.join(", ", missingVariables));
            return new CompatibilityResult(false, reason, missingVariables, availableVariables, 
                condition.getClassification());
        }
    }
    
    /**
     * 여러 조건에 대한 호환성을 일괄 검사합니다.
     */
    public Map<Long, CompatibilityResult> checkBatchCompatibility(List<ConditionTemplate> conditions, 
                                                                ManagedResource resource) {
        Map<Long, CompatibilityResult> results = new HashMap<>();
        
        for (ConditionTemplate condition : conditions) {
            CompatibilityResult result = checkCompatibility(condition, resource);
            results.put(condition.getId(), result);
        }
        
        log.debug("🔍 배치 호환성 검사 완료: {} 개 조건, 호환 가능: {} 개", 
            conditions.size(), 
            results.values().stream().mapToInt(r -> r.isCompatible() ? 1 : 0).sum());
        
        return results;
    }
    
    /**
     * 특정 분류의 조건들만 필터링합니다.
     */
    public List<ConditionTemplate> filterByClassification(List<ConditionTemplate> conditions, 
                                                         ConditionTemplate.ConditionClassification classification) {
        return conditions.stream()
            .filter(condition -> classification.equals(condition.getClassification()))
            .collect(Collectors.toList());
    }
    
    /**
     * 위험도별로 조건들을 그룹화합니다.
     */
    public Map<ConditionTemplate.RiskLevel, List<ConditionTemplate>> groupByRiskLevel(List<ConditionTemplate> conditions) {
        return conditions.stream()
            .collect(Collectors.groupingBy(
                condition -> condition.getRiskLevel() != null ? 
                    condition.getRiskLevel() : ConditionTemplate.RiskLevel.LOW));
    }
} 