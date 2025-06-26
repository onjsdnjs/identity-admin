package io.spring.identityadmin.security.xacml.pap.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.identityadmin.admin.iam.service.GroupService;
import io.spring.identityadmin.admin.iam.service.PermissionService;
import io.spring.identityadmin.admin.iam.service.RoleService;
import io.spring.identityadmin.admin.iam.service.UserManagementService;
import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.domain.dto.ConditionTemplateDto;
import io.spring.identityadmin.domain.dto.GroupMetadataDto;
import io.spring.identityadmin.domain.dto.PermissionDto;
import io.spring.identityadmin.domain.dto.RoleDto;
import io.spring.identityadmin.domain.entity.ConditionTemplate;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.domain.entity.Role;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.repository.ConditionTemplateRepository;
import io.spring.identityadmin.repository.ManagedResourceRepository;
import io.spring.identityadmin.repository.PermissionRepository;
import io.spring.identityadmin.resource.service.ConditionCompatibilityService;
import io.spring.identityadmin.security.xacml.pap.dto.VisualPolicyDto;
import io.spring.identityadmin.security.xacml.pap.service.PolicyBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/policy-builder")
@RequiredArgsConstructor
@Slf4j
public class PolicyBuilderController {

    private final PolicyBuilderService policyBuilderService;
    private final UserManagementService userManagementService;
    private final GroupService groupService;
    private final RoleService roleService;
    private final PermissionCatalogService permissionCatalogService;
    private final ConditionTemplateRepository conditionTemplateRepository;
    private final ManagedResourceRepository managedResourceRepository;
    private final PermissionRepository permissionRepository;
    private final ConditionCompatibilityService conditionCompatibilityService;
    private final ObjectMapper objectMapper;
    private final PermissionService permissionService;
    private final ModelMapper modelMapper;
    private static final Pattern SPEL_VARIABLE_PATTERN = Pattern.compile("#(\\w+)");
    private static final Set<String> GLOBAL_CONTEXT_VARIABLES = Set.of("#authentication", "#request", "#ai");


    /**
     * 🚀 [완전 리팩토링] 정책 빌더 메인 페이지
     * 
     * 기존: 모든 조건을 표시하고 드래그 시 검증
     * 신규: 리소스에 호환되는 조건만 사전 필터링하여 표시
     */
    @GetMapping
    public String showPolicyBuilder(@RequestParam(value = "resourceId", required = false) Long resourceId,
                                   @RequestParam(value = "permissionId", required = false) Long permissionId,
                                   Model model) {
        log.info("🚀 정책 빌더 접근: resourceId={}, permissionId={}", resourceId, permissionId);

        try {
            // 1. 기본 데이터 로드
            loadBasicData(model);

            // 2. 리소스 컨텍스트 설정
            ManagedResource targetResource = determineTargetResource(resourceId, permissionId);
            if (targetResource != null) {
                model.addAttribute("resourceContext", targetResource);
                log.info("🔍 리소스 컨텍스트 설정: {}", targetResource.getResourceIdentifier());
            }

            // 3. 🎯 핵심 개선: 호환되는 조건만 필터링하여 제공
            List<ConditionTemplate> allConditions = conditionTemplateRepository.findAll();
            List<ConditionTemplate> compatibleConditions = getCompatibleConditionsForResource(targetResource, allConditions);
            
            // 4. 조건들을 DTO로 변환 (UI용)
            List<ConditionTemplateDto> conditionDtos = convertToConditionDtos(compatibleConditions, targetResource);
            
            model.addAttribute("allConditions", conditionDtos);
            model.addAttribute("conditionStatistics", calculateConditionStatistics(compatibleConditions));
            
            log.info("🎯 조건 필터링 완료: 전체 {} 개 → 호환 {} 개 → DTO {} 개", 
                allConditions.size(), compatibleConditions.size(), conditionDtos.size());

            log.info("🎯 필터링 결과: 전체 {} 개 조건 중 {} 개 호환 조건 제공", 
                allConditions.size(), compatibleConditions.size());

            return "admin/policy-builder";

        } catch (Exception e) {
            log.error("정책 빌더 로드 실패", e);
            model.addAttribute("errorMessage", "정책 빌더를 로드하는 중 오류가 발생했습니다: " + e.getMessage());
            return "admin/policy-builder";
        }
    }

    /**
     * 🎯 리소스에 호환되는 조건들만 반환
     */
    private List<ConditionTemplate> getCompatibleConditionsForResource(ManagedResource resource, List<ConditionTemplate> allConditions) {
        log.info("🔍 조건 호환성 필터링 시작: resource={}, 전체조건수={}", 
            resource != null ? resource.getResourceIdentifier() : "null", allConditions.size());
            
        if (resource == null) {
            // 리소스가 없으면 범용 조건만 반환
            log.info("🌟 리소스 컨텍스트 없음 - 범용 조건만 제공");
            List<ConditionTemplate> universalConditions = conditionCompatibilityService.getUniversalConditions(allConditions);
            log.info("🌟 범용 조건 개수: {}", universalConditions.size());
            return universalConditions;
        }

        // 리소스와 호환되는 조건들 필터링
        List<ConditionTemplate> compatibleConditions = conditionCompatibilityService.getCompatibleConditions(resource, allConditions);
        log.info("🎯 호환 조건 필터링 결과: {} 개", compatibleConditions.size());
        return compatibleConditions;
    }

    /**
     * 🔄 조건들을 UI용 DTO로 변환
     */
    private List<ConditionTemplateDto> convertToConditionDtos(List<ConditionTemplate> conditions, ManagedResource resource) {
        return conditions.stream().map(condition -> {
            // SpEL 템플릿에서 필요한 변수 목록을 추출
            Set<String> requiredVars = extractVariablesFromSpel(condition.getSpelTemplate());
            
            // 조건 설명 강화
            String enhancedDescription = enhanceConditionDescriptionV2(condition);

            // 모든 필터링된 조건은 활성화 상태
            boolean isActive = true;

            return new ConditionTemplateDto(
                    condition.getId(),
                    condition.getName(),
                    enhancedDescription,
                    requiredVars,
                    isActive,
                    condition.getSpelTemplate()
            );
        })
        .sorted((a, b) -> {
            // 범용 조건을 먼저 표시
            ConditionTemplate condA = findConditionById(conditions, a.id());
            ConditionTemplate condB = findConditionById(conditions, b.id());
            
            int classOrder = getClassificationOrder(condA.getClassification()) - 
                           getClassificationOrder(condB.getClassification());
            if (classOrder != 0) return classOrder;
            
            return a.name().compareTo(b.name());
        })
        .toList();
    }

    /**
     * 🔍 대상 리소스 결정 (resourceId 또는 permissionId로부터)
     */
    private ManagedResource determineTargetResource(Long resourceId, Long permissionId) {
        if (resourceId != null) {
            return managedResourceRepository.findById(resourceId).orElse(null);
        }
        
        if (permissionId != null) {
            // 권한으로부터 연결된 리소스 찾기
            return permissionRepository.findById(permissionId)
                .map(permission -> permission.getManagedResource())
                .orElse(null);
        }
        
        return null;
    }

    /**
     * 🔧 기본 데이터 로드 (역할, 권한 등)
     */
    private void loadBasicData(Model model) {
        // 역할 목록
        List<Role> allRoles = roleService.getRoles();
        model.addAttribute("allRoles", allRoles);

        // 권한 목록
        List<Permission> allPermissions = permissionRepository.findAll();
        model.addAttribute("allPermissions", allPermissions);

        // 사전 선택된 권한이 있는지 확인
        Permission preselectedPermission = (Permission) model.asMap().get("preselectedPermission");
        if (preselectedPermission != null) {
            model.addAttribute("preselectedPermission", preselectedPermission);
        }
    }


    
    /**
     * 🔧 신규: 기본 리소스 컨텍스트를 생성합니다.
     * 정책 빌더에 직접 접근할 때 사용됩니다.
     */
    private Map<String, Object> createDefaultResourceContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("resourceIdentifier", "GENERAL_POLICY");
        context.put("resourceType", "GENERAL");
        context.put("friendlyName", "일반 정책");
        context.put("description", "특정 리소스에 종속되지 않는 일반적인 정책");
        context.put("parameterTypes", "");
        context.put("returnType", "void");
        context.put("isDirectAccess", true);
        return context;
    }

    /**
     * 모델에 '컨텍스트 인지형' 조건 템플릿 목록을 추가하는 헬퍼 메서드
     * 🔄 2단계: 조건 분류 시스템을 적용하여 시각적으로 구분된 조건들을 제공합니다.
     */
    private void addContextAwareConditionsToModel(Model model) {
        List<ConditionTemplate> allConditions = conditionTemplateRepository.findAll();
        
        // 분류별로 조건들을 그룹화
        Map<ConditionTemplate.ConditionClassification, List<ConditionTemplate>> classifiedConditions = 
            allConditions.stream()
                .collect(Collectors.groupingBy(
                    cond -> cond.getClassification() != null ? 
                        cond.getClassification() : ConditionTemplate.ConditionClassification.UNIVERSAL));
        
        Map<ConditionTemplate.RiskLevel, List<ConditionTemplate>> riskGrouped = 
            allConditions.stream()
                .collect(Collectors.groupingBy(
                    cond -> cond.getRiskLevel() != null ? 
                        cond.getRiskLevel() : ConditionTemplate.RiskLevel.LOW));
        
        log.info("🔍 조건 템플릿 로드 (분류별): 범용 {} 개, 컨텍스트의존 {} 개, 복잡 {} 개", 
            classifiedConditions.getOrDefault(ConditionTemplate.ConditionClassification.UNIVERSAL, Collections.emptyList()).size(),
            classifiedConditions.getOrDefault(ConditionTemplate.ConditionClassification.CONTEXT_DEPENDENT, Collections.emptyList()).size(),
            classifiedConditions.getOrDefault(ConditionTemplate.ConditionClassification.CUSTOM_COMPLEX, Collections.emptyList()).size());

        List<ConditionTemplateDto> conditionDtos = allConditions.stream().map(cond -> {
            // SpEL 템플릿에서 필요한 변수 목록을 추출합니다.
            Set<String> requiredVars = extractVariablesFromSpel(cond.getSpelTemplate());
            
            // 조건 타입에 따른 설명 보강 (2단계 업데이트)
            String enhancedDescription = enhanceConditionDescriptionV2(cond);

            // 분류에 따른 활성화 상태 결정
            boolean isActive = determineConditionActivation(cond, model);

            return new ConditionTemplateDto(
                    cond.getId(),
                    cond.getName(),
                    enhancedDescription,
                    requiredVars,
                    isActive,
                    cond.getSpelTemplate()
            );
        })
        .sorted((a, b) -> {
            // 2단계: 분류 우선순위로 정렬
            ConditionTemplate condA = findConditionById(allConditions, a.id());
            ConditionTemplate condB = findConditionById(allConditions, b.id());
            
            int classOrder = getClassificationOrder(condA.getClassification()) - 
                           getClassificationOrder(condB.getClassification());
            if (classOrder != 0) return classOrder;
            
            // 같은 분류 내에서는 복잡도 순
            int complexityOrder = (condA.getComplexityScore() != null ? condA.getComplexityScore() : 1) - 
                                (condB.getComplexityScore() != null ? condB.getComplexityScore() : 1);
            if (complexityOrder != 0) return complexityOrder;
            
            return a.name().compareTo(b.name());
        })
        .toList();

        model.addAttribute("allConditions", conditionDtos);
        model.addAttribute("conditionStatistics", calculateConditionStatistics(allConditions));
    }
    
    /**
     * 조건 설명을 타입에 따라 보강합니다.
     */
    private String enhanceConditionDescription(ConditionTemplate cond) {
        StringBuilder desc = new StringBuilder();
        
        // 기본 설명
        if (StringUtils.hasText(cond.getDescription())) {
            desc.append(cond.getDescription());
        }
        
        // 자동 생성 여부 표시
        if (Boolean.TRUE.equals(cond.getIsAutoGenerated())) {
            if (Boolean.TRUE.equals(cond.getIsUniversal())) {
                desc.append(" 🤖🌍 (자동생성 범용)");
            } else {
                desc.append(" 🤖 (자동생성)");
            }
        } else {
            desc.append(" 👤 (수동 설정)");
        }
        
        // 템플릿 타입 표시
        if (StringUtils.hasText(cond.getTemplateType())) {
            switch (cond.getTemplateType()) {
                case "universal" -> desc.append(" - 모든 메서드에 적용 가능");
                case "object_return" -> desc.append(" - 객체 반환 메서드용");
                case "id_parameter" -> desc.append(" - ID 파라미터 메서드용");
                case "ownership" -> desc.append(" - 소유권 검증용");
            }
        }
        
        return desc.toString();
    }
    
    /**
     * 🔄 2단계: 조건 설명을 분류 시스템에 맞게 보강합니다.
     */
    private String enhanceConditionDescriptionV2(ConditionTemplate cond) {
        StringBuilder desc = new StringBuilder();
        
        // 기본 설명
        if (StringUtils.hasText(cond.getDescription())) {
            desc.append(cond.getDescription());
        }
        
        // 분류 아이콘 및 설명
        if (cond.getClassification() != null) {
            switch (cond.getClassification()) {
                case UNIVERSAL -> desc.append(" 🟢 (즉시 사용 가능)");
                case CONTEXT_DEPENDENT -> desc.append(" 🟡 (AI 검증 필요)");
                case CUSTOM_COMPLEX -> desc.append(" 🔴 (전문가 검토)");
            }
        }
        
        // 복잡도 표시
        if (cond.getComplexityScore() != null) {
            desc.append(" [복잡도: ").append(cond.getComplexityScore()).append("/10]");
        }
        
        // 승인 필요 여부
        if (Boolean.TRUE.equals(cond.getApprovalRequired())) {
            desc.append(" ⚠️ 승인필요");
        }
        
        return desc.toString();
    }
    
    /**
     * 조건의 활성화 상태를 결정합니다.
     */
    private boolean determineConditionActivation(ConditionTemplate cond, Model model) {
        // 1. 범용 조건은 항상 활성화
        if (ConditionTemplate.ConditionClassification.UNIVERSAL.equals(cond.getClassification())) {
            return true;
        }
        
        // 2. 승인이 필요한 조건은 기본적으로 비활성화 (관리자만 활성화)
        if (Boolean.TRUE.equals(cond.getApprovalRequired())) {
            return false;
        }
        
        // 3. 컨텍스트 의존 조건은 리소스 컨텍스트가 있을 때만 활성화
        if (ConditionTemplate.ConditionClassification.CONTEXT_DEPENDENT.equals(cond.getClassification())) {
            return model.containsAttribute("resourceContext");
        }
        
        // 기본적으로 활성화
        return true;
    }
    
    /**
     * 분류의 정렬 순서를 반환합니다.
     */
    private int getClassificationOrder(ConditionTemplate.ConditionClassification classification) {
        if (classification == null) return 2;
        return switch (classification) {
            case UNIVERSAL -> 1;
            case CONTEXT_DEPENDENT -> 2;
            case CUSTOM_COMPLEX -> 3;
        };
    }
    
    /**
     * 조건 통계를 계산합니다.
     */
    private Map<String, Object> calculateConditionStatistics(List<ConditionTemplate> conditions) {
        Map<String, Object> stats = new HashMap<>();
        
        // 분류별 개수
        Map<ConditionTemplate.ConditionClassification, Long> byClassification = 
            conditions.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getClassification() != null ? c.getClassification() : ConditionTemplate.ConditionClassification.UNIVERSAL,
                    Collectors.counting()));
        
        // 위험도별 개수
        Map<ConditionTemplate.RiskLevel, Long> byRiskLevel = 
            conditions.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getRiskLevel() != null ? c.getRiskLevel() : ConditionTemplate.RiskLevel.LOW,
                    Collectors.counting()));
        
        stats.put("total", conditions.size());
        stats.put("byClassification", byClassification);
        stats.put("byRiskLevel", byRiskLevel);
        stats.put("averageComplexity", conditions.stream()
            .mapToInt(c -> c.getComplexityScore() != null ? c.getComplexityScore() : 1)
            .average().orElse(0.0));
        stats.put("requireApproval", conditions.stream()
            .mapToLong(c -> Boolean.TRUE.equals(c.getApprovalRequired()) ? 1 : 0)
            .sum());
        
        return stats;
    }
    
    /**
     * ID로 조건 템플릿을 찾는 헬퍼 메서드
     */
    private ConditionTemplate findConditionById(List<ConditionTemplate> conditions, Long id) {
        return conditions.stream()
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .orElse(new ConditionTemplate()); // 기본값 반환
    }

    /**
     * 모델에서 사용 가능한 컨텍스트 변수 Set을 추출하는 헬퍼 메서드
     */
    private Set<String> getAvailableTypesFromModel(Model model) {
        // 1. 모든 컨텍스트에서 사용 가능한 전역 변수를 기본으로 포함합니다.
        Set<String> types = new HashSet<>(GLOBAL_CONTEXT_VARIABLES);

        // 2. 리소스 워크벤치에서 전달된 컨텍스트가 있는지 확인합니다.
        if (model.containsAttribute("resourceContext")) {
            Map<String, Object> rc = (Map<String, Object>) model.getAttribute("resourceContext");
            if (rc != null) {
                Object resourceSpecificVarsObj = rc.get("availableVariables");

                // [핵심 수정] Set 으로 강제 형변환하는 대신, Collection 타입인지 확인하고 안전하게 추가합니다.
                if (resourceSpecificVarsObj instanceof Collection) {
                    // List든 Set 이든 관계없이 모든 원소를 안전하게 추가합니다.
                    types.addAll((Collection<String>) resourceSpecificVarsObj);
                }

                // returnObjectType에 대한 처리도 안전하게 변경합니다.
                Object returnTypeObj = rc.get("returnObjectType");
                if (returnTypeObj != null) {
                    types.add(returnTypeObj.toString());
                }
            }
        }
        return types;
    }

    /**
     * [신규] SpEL 템플릿 문자열에서 '#변수명' 형태의 필요 변수 목록을 추출합니다.
     */
    private Set<String> extractVariablesFromSpel(String spelTemplate) {
        Set<String> variables = new HashSet<>();
        if (spelTemplate == null) return variables;
        Matcher matcher = SPEL_VARIABLE_PATTERN.matcher(spelTemplate);
        while (matcher.find()) {
            variables.add(matcher.group()); // # 포함하여 저장 (예: #owner)
        }
        return variables;
    }

    /**
     * [신규] 리소스 워크벤치에서 '상세 정책 설정'을 선택했을 때 호출되는 엔드포인트.
     * 리소스 컨텍스트 정보를 모델에 담아 빌더 페이지로 이동합니다.
     */
    /**
     * [수정된 메서드] 리소스 워크벤치에서 '상세 정책 설정'을 선택했을 때 호출되는 엔드포인트.
     * GET과 POST 모두 지원하여 새 창으로 열기와 폼 제출 모두 처리합니다.
     */
    @RequestMapping(value = "/from-resource", method = {RequestMethod.GET, RequestMethod.POST})
    public String policyBuilderFromResource(
            @RequestParam Long resourceId,
            @RequestParam Long permissionId,
            Model model) {

        log.info("🚀 리소스 워크벤치에서 정책빌더 접근: resourceId={}, permissionId={}", resourceId, permissionId);

        try {
            ManagedResource resource = managedResourceRepository.findById(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourceId));

            // 🔧 안전한 리소스 컨텍스트 생성 (순환 참조 방지)
            Map<String, Object> resourceContext = createSafeResourceContext(resource);
            model.addAttribute("resourceContext", resourceContext);

            // 🔧 권한 정보를 안전하게 추가
            addSafePermissionToModel(permissionId, model);

            log.info("🔍 리소스 컨텍스트 설정 완료: {}", resource.getResourceIdentifier());

            // 메인 정책빌더 로직 호출
            return showPolicyBuilder(resourceId, permissionId, model);

        } catch (Exception e) {
            log.error("🚨 리소스 워크벤치에서 정책빌더 접근 실패", e);
            model.addAttribute("errorMessage", "정책 빌더 로드 실패: " + e.getMessage());
            return "admin/policy-builder";
        }
    }

    /**
     * 🔧 순환 참조 없는 안전한 리소스 컨텍스트 생성
     */
    private Map<String, Object> createSafeResourceContext(ManagedResource resource) {
        Map<String, Object> context = new HashMap<>();
        
        // 기본 정보만 안전하게 추가
        context.put("resourceIdentifier", resource.getResourceIdentifier());
        context.put("friendlyName", resource.getFriendlyName());
        context.put("description", resource.getDescription());
        context.put("resourceType", resource.getResourceType());
        context.put("returnType", resource.getReturnType());
        
        // 파라미터 타입을 안전하게 파싱
        String paramTypes = resource.getParameterTypes();
        if (paramTypes != null && !paramTypes.trim().isEmpty()) {
            try {
                // JSON 배열이면 파싱, 아니면 문자열 그대로
                if (paramTypes.startsWith("[") && paramTypes.endsWith("]")) {
                    List<String> parsedTypes = objectMapper.readValue(paramTypes, new TypeReference<List<String>>() {});
                    context.put("parameterTypes", parsedTypes);
                } else {
                    context.put("parameterTypes", Arrays.asList(paramTypes.split(",")));
                }
            } catch (Exception e) {
                log.warn("파라미터 타입 파싱 실패, 원본 사용: {}", paramTypes);
                context.put("parameterTypes", paramTypes);
            }
        } else {
            context.put("parameterTypes", Collections.emptyList());
        }
        
        return context;
    }

    /**
     * 🔧 권한 정보를 안전하게 모델에 추가
     */
    private void addSafePermissionToModel(Long permissionId, Model model) {
        try {
            permissionService.getPermission(permissionId)
                    .ifPresent(permission -> {
                        // 순환 참조 없는 DTO 생성
                        Map<String, Object> permissionData = new HashMap<>();
                        permissionData.put("id", permission.getId());
                        permissionData.put("name", permission.getName());
                        permissionData.put("friendlyName", permission.getFriendlyName());
                        permissionData.put("description", permission.getDescription());
                        permissionData.put("actionType", permission.getActionType());
                        permissionData.put("targetType", permission.getTargetType());
                        
                        model.addAttribute("preselectedPermission", permissionData);
                        log.debug("🔍 사전 선택 권한 설정: {}", permission.getName());
                    });
        } catch (Exception e) {
            log.warn("권한 정보 로드 실패: permissionId={}", permissionId, e);
        }
    }

    /**
     * UI에서 생성된 시각적 정책 데이터를 받아 실제 정책을 생성하는 API 엔드포인트입니다.
     */
    @PostMapping("/api/build")
    public ResponseEntity<Policy> buildPolicy(@RequestBody VisualPolicyDto visualPolicyDto) {
        Policy createdPolicy = policyBuilderService.buildPolicyFromVisualComponents(visualPolicyDto);
        return ResponseEntity.ok(createdPolicy);
    }
}