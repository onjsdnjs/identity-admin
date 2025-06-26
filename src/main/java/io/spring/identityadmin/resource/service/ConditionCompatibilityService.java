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
        private final boolean requiresAiValidation;

        public CompatibilityResult(boolean compatible, String reason, 
                                 Set<String> missingVariables, Set<String> availableVariables,
                                 ConditionTemplate.ConditionClassification classification) {
            this(compatible, reason, missingVariables, availableVariables, classification, false);
        }
        
        public CompatibilityResult(boolean compatible, String reason, 
                                 Set<String> missingVariables, Set<String> availableVariables,
                                 ConditionTemplate.ConditionClassification classification,
                                 boolean requiresAiValidation) {
            this.compatible = compatible;
            this.reason = reason;
            this.missingVariables = missingVariables != null ? missingVariables : new HashSet<>();
            this.availableVariables = availableVariables != null ? availableVariables : new HashSet<>();
            this.classification = classification;
            this.requiresAiValidation = requiresAiValidation;
        }

        // Getters
        public boolean isCompatible() { return compatible; }
        public String getReason() { return reason; }
        public Set<String> getMissingVariables() { return missingVariables; }
        public Set<String> getAvailableVariables() { return availableVariables; }
        public ConditionTemplate.ConditionClassification getClassification() { return classification; }
        public boolean requiresAiValidation() { return requiresAiValidation; }
    }
    
    /**
     * 조건 템플릿과 관리 리소스 간의 호환성을 검사합니다.
     */
    public CompatibilityResult checkCompatibility(ConditionTemplate condition, ManagedResource resource) {
        if (condition == null || resource == null) {
            return new CompatibilityResult(false, "조건 또는 리소스가 null입니다.", 
                null, null, ConditionTemplate.ConditionClassification.CUSTOM_COMPLEX, false);
        }

        log.debug("🔍 호환성 검사 시작: 조건={}, 리소스={}, 분류={}", 
            condition.getName(), resource.getResourceIdentifier(), condition.getClassification());

        // 1. 범용 조건은 항상 호환 가능하며 AI 검증 불필요
        if (ConditionTemplate.ConditionClassification.UNIVERSAL.equals(condition.getClassification())) {
            log.debug("✅ 범용 조건으로 즉시 승인: {}", condition.getName());
            return new CompatibilityResult(true, "범용 조건으로 모든 리소스와 호환됩니다.", 
                new HashSet<>(), getAllUniversalVariables(), condition.getClassification(), false);
        }

        // 2. 일반 정책 컨텍스트인 경우 특별 처리
        if ("GENERAL_POLICY".equals(resource.getResourceIdentifier())) {
            return handleGeneralPolicyContext(condition, resource);
        }

        // 3. 필요한 변수 추출
        Set<String> requiredVariables = extractVariablesFromSpel(condition.getSpelTemplate());
        log.debug("🔍 필요한 변수들: {}", requiredVariables);
        
        // 4. 리소스에서 사용 가능한 변수 계산
        Set<String> availableVariables = calculateAvailableVariables(resource);
        log.debug("🔍 사용 가능한 변수들: {}", availableVariables);
        
        // 5. 호환성 검사 및 AI 검증 필요성 판단
        return performEnhancedCompatibilityCheck(condition, requiredVariables, availableVariables, resource);
    }
    
    /**
     * 🔧 신규: 일반 정책 컨텍스트에서의 호환성 검사
     */
    private CompatibilityResult handleGeneralPolicyContext(ConditionTemplate condition, ManagedResource resource) {
        log.debug("🔧 일반 정책 컨텍스트 처리: {}", condition.getName());
        
        // 일반 정책에서는 범용 변수만 사용 가능한 조건들을 허용
        Set<String> requiredVariables = extractVariablesFromSpel(condition.getSpelTemplate());
        Set<String> universalVariables = getAllUniversalVariables();
        
        Set<String> missingVariables = new HashSet<>(requiredVariables);
        missingVariables.removeAll(universalVariables);
        
        if (missingVariables.isEmpty()) {
            // 범용 변수만 사용하는 경우 - AI 검증 불필요
            boolean needsAi = shouldRequireAiValidation(condition, requiredVariables);
            String reason = needsAi ? 
                "일반 정책에서 사용 가능하지만 AI 검증이 필요합니다." :
                "일반 정책에서 즉시 사용 가능합니다.";
            
            return new CompatibilityResult(true, reason, 
                new HashSet<>(), universalVariables, condition.getClassification(), needsAi);
        } else {
            // 특정 리소스 변수가 필요한 경우
            return new CompatibilityResult(false, 
                "일반 정책에서는 리소스별 변수를 사용할 수 없습니다: " + String.join(", ", missingVariables), 
                missingVariables, universalVariables, condition.getClassification(), false);
        }
    }
    
    /**
     * 🔧 개선: 향상된 호환성 검사 - AI 검증 필요성 판단 포함
     */
    private CompatibilityResult performEnhancedCompatibilityCheck(ConditionTemplate condition, 
                                                                Set<String> requiredVariables, 
                                                                Set<String> availableVariables,
                                                                ManagedResource resource) {
        
        // 누락된 변수 계산
        Set<String> missingVariables = new HashSet<>(requiredVariables);
        missingVariables.removeAll(availableVariables);
        
        if (!missingVariables.isEmpty()) {
            // 누락된 변수가 있는 경우 - 호환 불가
            String reason = String.format("필요한 변수가 누락되었습니다: %s", 
                String.join(", ", missingVariables));
            log.debug("❌ 호환성 실패: {}", reason);
            return new CompatibilityResult(false, reason, missingVariables, availableVariables, 
                condition.getClassification(), false);
        }
        
        // 모든 변수가 사용 가능한 경우 - AI 검증 필요성 판단
        boolean needsAiValidation = shouldRequireAiValidation(condition, requiredVariables);
        
        String reason = buildCompatibilityReason(condition, requiredVariables, needsAiValidation);
        
        log.debug("✅ 호환성 성공: AI검증필요={}, 이유={}", needsAiValidation, reason);
        
        return new CompatibilityResult(true, reason, missingVariables, availableVariables, 
            condition.getClassification(), needsAiValidation);
    }
    
    /**
     * 🔧 신규: AI 검증이 필요한지 판단합니다.
     */
    private boolean shouldRequireAiValidation(ConditionTemplate condition, Set<String> requiredVariables) {
        // 1. 분류별 AI 검증 필요성
        if (condition.getClassification() != null) {
            switch (condition.getClassification()) {
                case UNIVERSAL:
                    return false; // 범용 조건은 AI 검증 불필요
                case CONTEXT_DEPENDENT:
                    return true;  // 컨텍스트 의존 조건은 AI 검증 필요
                case CUSTOM_COMPLEX:
                    return true;  // 복잡한 조건은 AI 검증 필요
            }
        }
        
        // 2. 복잡도 기반 판단
        if (condition.getComplexityScore() != null && condition.getComplexityScore() > 5) {
            return true; // 복잡도 5 초과시 AI 검증 필요
        }
        
        // 3. 특정 패턴 검사
        String spel = condition.getSpelTemplate();
        if (spel != null) {
            // 복잡한 SpEL 표현식은 AI 검증 필요
            if (spel.contains("&&") || spel.contains("||") || spel.contains("?") || spel.contains(":")) {
                return true;
            }
            
            // hasPermission 함수는 AI 검증 불필요 (잘 정의된 함수)
            if (spel.contains("hasPermission(") && !spel.contains("&&") && !spel.contains("||")) {
                return false;
            }
            
            // 단순한 함수 호출은 AI 검증 불필요
            if (spel.matches("^[a-zA-Z]+\\([^)]*\\)$")) {
                return false;
            }
        }
        
        // 4. 기본값: 안전을 위해 AI 검증 필요
        return true;
    }
    
    /**
     * 🔧 신규: 호환성 결과 메시지를 구성합니다.
     */
    private String buildCompatibilityReason(ConditionTemplate condition, Set<String> requiredVariables, boolean needsAi) {
        StringBuilder reason = new StringBuilder();
        
        if (requiredVariables.isEmpty()) {
            reason.append("파라미터가 필요 없는 조건입니다.");
        } else {
            reason.append(String.format("필요한 변수(%s)가 모두 사용 가능합니다.", 
                String.join(", ", requiredVariables)));
        }
        
        if (needsAi) {
            reason.append(" [AI 고급 검증 필요]");
        } else {
            reason.append(" [즉시 사용 가능]");
        }
        
        return reason.toString();
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
            String paramTypes = resource.getParameterTypes();
            log.debug("🔍 파라미터 타입 원본: {}", paramTypes);
            
            if (paramTypes != null && !paramTypes.trim().isEmpty()) {
                // 다양한 형태의 파라미터 타입 처리
                if (paramTypes.startsWith("[") && paramTypes.endsWith("]")) {
                    // JSON 배열 형태: [{"name":"userId","type":"Long"}, {"name":"document","type":"Document"}]
                    variables.addAll(extractFromJsonArray(paramTypes));
                } else if (paramTypes.contains(",")) {
                    // 쉼표로 구분된 형태: "Group,List" 또는 "Long userId, Document document"
                    variables.addAll(extractFromCommaSeparated(paramTypes));
                } else {
                    // 단일 파라미터
                    variables.addAll(extractFromSingleParam(paramTypes));
                }
            }
            
            log.debug("🔍 추출된 파라미터 변수들: {}", variables);
        } catch (Exception e) {
            log.warn("파라미터 변수 추출 중 오류: {}", e.getMessage());
        }
        
        return variables;
    }
    
    /**
     * JSON 배열 형태에서 파라미터 추출
     */
    private Set<String> extractFromJsonArray(String paramTypes) {
        Set<String> variables = new HashSet<>();
        try {
            // 간단한 JSON 파싱 (정규식 사용)
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = namePattern.matcher(paramTypes);
            while (matcher.find()) {
                String paramName = matcher.group(1);
                variables.add("#" + paramName);
            }
        } catch (Exception e) {
            log.warn("JSON 배열 파싱 실패: {}", e.getMessage());
        }
        return variables;
    }
    
    /**
     * 쉼표로 구분된 형태에서 파라미터 추출
     */
    private Set<String> extractFromCommaSeparated(String paramTypes) {
        Set<String> variables = new HashSet<>();
        String[] parts = paramTypes.split(",");
        
        for (String part : parts) {
            part = part.trim();
            
            // "Long userId" 형태인지 확인
            if (part.contains(" ")) {
                String[] typeName = part.split("\\s+");
                if (typeName.length >= 2) {
                    variables.add("#" + typeName[1]); // 변수명
                }
            } else {
                // "Group", "List" 등 타입만 있는 경우
                String paramName = inferParameterName(part);
                if (paramName != null) {
                    variables.add("#" + paramName);
                }
            }
        }
        
        return variables;
    }
    
    /**
     * 단일 파라미터에서 변수 추출
     */
    private Set<String> extractFromSingleParam(String paramTypes) {
        Set<String> variables = new HashSet<>();
        String param = paramTypes.trim();
        
        if (param.contains(" ")) {
            String[] typeName = param.split("\\s+");
            if (typeName.length >= 2) {
                variables.add("#" + typeName[1]);
            }
        } else {
            String paramName = inferParameterName(param);
            if (paramName != null) {
                variables.add("#" + paramName);
            }
        }
        
        return variables;
    }
    
    /**
     * 타입으로부터 파라미터명을 추론
     */
    private String inferParameterName(String type) {
        // 패키지명 제거
        String simpleType = type.substring(type.lastIndexOf('.') + 1);
        
        // 일반적인 타입별 파라미터명 매핑
        Map<String, String> typeToParamMap = new HashMap<>();
        typeToParamMap.put("Long", "id");
        typeToParamMap.put("Integer", "id");
        typeToParamMap.put("String", "name");
        typeToParamMap.put("Group", "group");
        typeToParamMap.put("User", "user");
        typeToParamMap.put("Users", "user");
        typeToParamMap.put("Document", "document");
        typeToParamMap.put("Permission", "permission");
        typeToParamMap.put("Role", "role");
        typeToParamMap.put("List", "list");
        typeToParamMap.put("Set", "set");
        
        return typeToParamMap.get(simpleType);
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