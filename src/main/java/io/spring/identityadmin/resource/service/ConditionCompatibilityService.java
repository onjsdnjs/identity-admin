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
 * 🚀 [완전 리팩토링] 조건 호환성 서비스
 * 
 * 기존 방식: 사용자가 조건을 드래그할 때마다 하나씩 검증
 * 새로운 방식: 권한 선택 시 호환되는 조건만 사전 필터링하여 제공
 * 
 * AI 사용 영역:
 * - 복잡한 정책 조합 추천
 * - 보안 위험도 분석
 * - 정책 충돌 감지
 * - 자연어 → 정책 변환
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConditionCompatibilityService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("#(\\w+)");
    
    /**
     * 🎯 핵심 메서드: 특정 리소스와 호환되는 조건들만 반환
     * 
     * @param resource 대상 리소스
     * @param allConditions 모든 조건 템플릿
     * @return 호환되는 조건들만 필터링된 리스트
     */
    public List<ConditionTemplate> getCompatibleConditions(ManagedResource resource, List<ConditionTemplate> allConditions) {
        if (resource == null) {
            log.warn("🚨 리소스가 null입니다. 범용 조건만 반환합니다.");
            return getUniversalConditions(allConditions);
        }

        log.info("🔍 조건 호환성 사전 필터링 시작: {}", resource.getResourceIdentifier());
        
        List<ConditionTemplate> compatibleConditions = new ArrayList<>();
        Set<String> availableVariables = calculateAvailableVariables(resource);
        
        log.info("🔍 사용 가능한 변수들: {}", availableVariables);

        for (ConditionTemplate condition : allConditions) {
            CompatibilityResult result = checkCompatibility(condition, resource, availableVariables);
            
            if (result.isCompatible()) {
                compatibleConditions.add(condition);
                log.debug("✅ 호환 조건 추가: {} ({})", condition.getName(), result.getClassification());
            } else {
                log.debug("❌ 호환 불가 조건 제외: {} - {}", condition.getName(), result.getReason());
            }
        }

        log.info("🎯 필터링 완료: 전체 {} 개 중 {} 개 호환 조건 반환", 
            allConditions.size(), compatibleConditions.size());

        return compatibleConditions;
    }

    /**
     * 🌟 범용 조건들만 반환 (항상 호환됨)
     */
    public List<ConditionTemplate> getUniversalConditions(List<ConditionTemplate> allConditions) {
        return allConditions.stream()
            .filter(condition -> ConditionTemplate.ConditionClassification.UNIVERSAL.equals(condition.getClassification()))
            .collect(Collectors.toList());
    }

    /**
     * 🔍 개별 조건의 호환성 검사 (내부용)
     */
    private CompatibilityResult checkCompatibility(ConditionTemplate condition, ManagedResource resource, Set<String> availableVariables) {
        
        // 1. 범용 조건은 항상 호환됨 (즉시 승인)
        if (ConditionTemplate.ConditionClassification.UNIVERSAL.equals(condition.getClassification())) {
            return new CompatibilityResult(
                true, 
                "범용 조건 - 즉시 승인", 
                Collections.emptySet(), 
                availableVariables,
                ConditionTemplate.ConditionClassification.UNIVERSAL,
                false // AI 검증 불필요
            );
        }

        // 2. 메서드가 ABAC 적용 가능한지 검사
        if (!isAbacApplicableMethod(resource)) {
            return new CompatibilityResult(
                false, 
                "ABAC 적용 불가능한 메서드", 
                Collections.emptySet(), 
                availableVariables,
                condition.getClassification(),
                false
            );
        }

        // 3. 필요한 변수들 추출
        Set<String> requiredVariables = extractVariablesFromSpel(condition.getSpelTemplate());
        Set<String> missingVariables = new HashSet<>(requiredVariables);
        missingVariables.removeAll(availableVariables);

        // 4. 모든 필요한 변수가 있는지 확인
        boolean isCompatible = missingVariables.isEmpty();
        
        if (isCompatible) {
            return new CompatibilityResult(
                true, 
                "모든 필요 변수 사용 가능", 
                Collections.emptySet(), 
                availableVariables,
                condition.getClassification(),
                shouldRequireAiValidation(condition, requiredVariables)
            );
        } else {
            return new CompatibilityResult(
                false, 
                "필요한 변수가 누락되었습니다: " + String.join(", ", missingVariables), 
                missingVariables, 
                availableVariables,
                condition.getClassification(),
                false
            );
        }
    }

    /**
     * 🧠 AI 검증이 필요한지 판단
     */
    private boolean shouldRequireAiValidation(ConditionTemplate condition, Set<String> requiredVariables) {
        // 범용 조건은 AI 검증 불필요
        if (ConditionTemplate.ConditionClassification.UNIVERSAL.equals(condition.getClassification())) {
            return false;
        }
        
        // 복잡한 조건이나 커스텀 조건은 AI 검증 필요
        if (ConditionTemplate.ConditionClassification.CUSTOM_COMPLEX.equals(condition.getClassification())) {
            return true;
        }
        
        // 컨텍스트 의존 조건 중 복잡한 것들은 AI 검증 필요
        if (ConditionTemplate.ConditionClassification.CONTEXT_DEPENDENT.equals(condition.getClassification())) {
            // hasPermission 같은 복잡한 조건은 AI 검증
            String spelTemplate = condition.getSpelTemplate().toLowerCase();
            return spelTemplate.contains("haspermission") || 
                   spelTemplate.contains("complex") || 
                   requiredVariables.size() > 2;
        }
        
        return false;
    }

    /**
     * 🔍 SpEL 표현식에서 변수 추출
     */
    private Set<String> extractVariablesFromSpel(String spelTemplate) {
        Set<String> variables = new HashSet<>();
        if (spelTemplate != null) {
            Matcher matcher = VARIABLE_PATTERN.matcher(spelTemplate);
            while (matcher.find()) {
                variables.add("#" + matcher.group(1));
            }
        }
        return variables;
    }

    /**
     * 🔍 리소스에서 사용 가능한 변수들 계산
     */
    private Set<String> calculateAvailableVariables(ManagedResource resource) {
        Set<String> variables = new HashSet<>();
        
        // 항상 사용 가능한 범용 변수들
        variables.addAll(getAllUniversalVariables());
        
        // 파라미터에서 추출한 변수들
        variables.addAll(extractParameterVariables(resource));
        
        // 반환 객체가 있는 경우
        if (hasReturnObject(resource)) {
            variables.add("#returnObject");
        }
        
        return variables;
    }

    /**
     * 🌍 범용 변수들 (항상 사용 가능)
     */
    private Set<String> getAllUniversalVariables() {
        return Set.of(
            "#request", "#clientIp", "#session", 
            "#isBusinessHours", "#ai", "#currentTime", "#authentication"
        );
    }

    /**
     * 🔍 메서드 파라미터에서 변수들을 추출
     */
    private Set<String> extractParameterVariables(ManagedResource resource) {
        Set<String> variables = new HashSet<>();
        
        try {
            String paramTypes = resource.getParameterTypes();
            log.debug("🔍 파라미터 타입 처리: {}", paramTypes);
            
            if (paramTypes != null && !paramTypes.trim().isEmpty()) {
                if (paramTypes.startsWith("[") && paramTypes.endsWith("]")) {
                    // JSON 배열 형태: ["java.lang.Long", "java.util.List"]
                    variables.addAll(extractFromJsonArray(paramTypes));
                } else if (paramTypes.contains(",")) {
                    // 쉼표 구분 형태: Long,List<String>
                    variables.addAll(extractFromCommaSeparated(paramTypes));
                } else if (!paramTypes.equals("[]") && !paramTypes.equals("()")) {
                    // 단일 파라미터
                    variables.addAll(extractFromSingleParam(paramTypes));
                }
            }
            
        } catch (Exception e) {
            log.warn("파라미터 변수 추출 실패: {}", resource.getResourceIdentifier(), e);
        }
        
        return variables;
    }

    /**
     * JSON 배열에서 파라미터 변수 추출
     */
    private Set<String> extractFromJsonArray(String paramTypes) {
        Set<String> variables = new HashSet<>();
        
        try {
            String content = paramTypes.substring(1, paramTypes.length() - 1).trim();
            if (content.isEmpty()) {
                return variables;
            }
            
            String[] types = content.split(",");
            for (String type : types) {
                String cleanType = type.trim().replaceAll("[\\\"']", "");
                String paramName = inferParameterNameFromType(cleanType);
                if (paramName != null) {
                    variables.add("#" + paramName);
                }
            }
            
        } catch (Exception e) {
            log.warn("JSON 배열 파라미터 파싱 실패: {}", paramTypes, e);
        }
        
        return variables;
    }

    /**
     * 쉼표 구분 파라미터에서 변수 추출
     */
    private Set<String> extractFromCommaSeparated(String paramTypes) {
        Set<String> variables = new HashSet<>();
        
        String[] types = paramTypes.split(",");
        for (String type : types) {
            String cleanType = type.trim();
            if (cleanType.contains("<")) {
                cleanType = cleanType.substring(0, cleanType.indexOf("<"));
            }
            
            String paramName = inferParameterNameFromType(cleanType);
            if (paramName != null) {
                variables.add("#" + paramName);
            }
        }
        
        return variables;
    }

    /**
     * 단일 파라미터에서 변수 추출
     */
    private Set<String> extractFromSingleParam(String paramTypes) {
        Set<String> variables = new HashSet<>();
        
        String cleanType = paramTypes.trim();
        if (cleanType.contains("<")) {
            cleanType = cleanType.substring(0, cleanType.indexOf("<"));
        }
        
        String paramName = inferParameterNameFromType(cleanType);
        if (paramName != null) {
            variables.add("#" + paramName);
        }
        
        return variables;
    }

    /**
     * 타입으로부터 파라미터명을 추론
     */
    private String inferParameterNameFromType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        
        String simpleType = type.substring(type.lastIndexOf('.') + 1);
        
        Map<String, String> typeToParamMap = new HashMap<>();
        
        // ID 타입들
        typeToParamMap.put("Long", "id");
        typeToParamMap.put("Integer", "id");
        typeToParamMap.put("String", "name");
        typeToParamMap.put("UUID", "id");
        
        // 도메인 객체들
        typeToParamMap.put("Group", "group");
        typeToParamMap.put("User", "user");
        typeToParamMap.put("Users", "user");
        typeToParamMap.put("UserDto", "userDto");
        typeToParamMap.put("Document", "document");
        typeToParamMap.put("Permission", "permission");
        typeToParamMap.put("Role", "role");
        typeToParamMap.put("Policy", "policy");
        
        String paramName = typeToParamMap.get(simpleType);
        if (paramName != null) {
            return paramName;
        }
        
        // 매핑되지 않은 경우 타입명을 소문자로 변환
        return Character.toLowerCase(simpleType.charAt(0)) + simpleType.substring(1);
    }

    /**
     * 반환 객체가 있는지 확인
     */
    private boolean hasReturnObject(ManagedResource resource) {
        String returnType = resource.getReturnType();
        return returnType != null && 
               !returnType.equals("void") && 
               !returnType.equals("java.lang.Void");
    }

    /**
     * ABAC 적용 가능한 메서드인지 판단
     */
    private boolean isAbacApplicableMethod(ManagedResource resource) {
        if (ManagedResource.ResourceType.URL.equals(resource.getResourceType())) {
            return true;
        }
        
        if (!ManagedResource.ResourceType.METHOD.equals(resource.getResourceType())) {
            return false;
        }
        
        String resourceIdentifier = resource.getResourceIdentifier();
        String parameterTypes = resource.getParameterTypes();
        
        // 파라미터가 없는 메서드는 ABAC 적용 불가
        if (parameterTypes == null || parameterTypes.trim().isEmpty() || 
            parameterTypes.equals("[]") || parameterTypes.equals("()")) {
            
            // 단, 반환 객체가 있으면 Post-Authorization 가능
            if (hasReturnObject(resource)) {
                return true;
            }
            
            return false;
        }
        
        // getAll, findAll 등 전체 조회 메서드는 ABAC 적용 불가
        if (resourceIdentifier != null) {
            String methodName = extractMethodName(resourceIdentifier).toLowerCase();
            if (methodName.contains("getall") || methodName.contains("findall") || 
                methodName.contains("listall") || methodName.contains("all")) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 리소스 식별자에서 메서드명 추출
     */
    private String extractMethodName(String resourceIdentifier) {
        if (resourceIdentifier == null) return "";
        
        int lastDotIndex = resourceIdentifier.lastIndexOf('.');
        if (lastDotIndex == -1) return resourceIdentifier;
        
        String methodPart = resourceIdentifier.substring(lastDotIndex + 1);
        int parenIndex = methodPart.indexOf('(');
        if (parenIndex == -1) return methodPart;
        
        return methodPart.substring(0, parenIndex);
    }

    /**
     * 호환성 검사 결과 클래스
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
                                 ConditionTemplate.ConditionClassification classification,
                                 boolean requiresAiValidation) {
            this.compatible = compatible;
            this.reason = reason;
            this.missingVariables = missingVariables != null ? missingVariables : Collections.emptySet();
            this.availableVariables = availableVariables != null ? availableVariables : Collections.emptySet();
            this.classification = classification;
            this.requiresAiValidation = requiresAiValidation;
        }

        public boolean isCompatible() { return compatible; }
        public String getReason() { return reason; }
        public Set<String> getMissingVariables() { return missingVariables; }
        public Set<String> getAvailableVariables() { return availableVariables; }
        public ConditionTemplate.ConditionClassification getClassification() { return classification; }
        public boolean requiresAiValidation() { return requiresAiValidation; }
    }

    /**
     * 🔄 기존 코드 호환성을 위한 메서드 (2 파라미터)
     */
    public CompatibilityResult checkCompatibility(ConditionTemplate condition, ManagedResource resource) {
        if (condition == null || resource == null) {
            return new CompatibilityResult(false, "조건 또는 리소스가 null입니다.", 
                Collections.emptySet(), Collections.emptySet(), 
                ConditionTemplate.ConditionClassification.CUSTOM_COMPLEX, false);
        }

        Set<String> availableVariables = calculateAvailableVariables(resource);
        return checkCompatibility(condition, resource, availableVariables);
    }

    /**
     * 🔄 기존 코드 호환성을 위한 배치 호환성 검사
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
     * 🔄 기존 코드 호환성을 위한 위험도별 그룹화
     */
    public Map<ConditionTemplate.RiskLevel, List<ConditionTemplate>> groupByRiskLevel(List<ConditionTemplate> conditions) {
        return conditions.stream()
            .collect(Collectors.groupingBy(
                condition -> condition.getRiskLevel() != null ? 
                    condition.getRiskLevel() : ConditionTemplate.RiskLevel.LOW));
    }
} 