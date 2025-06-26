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
import io.spring.identityadmin.domain.entity.Role;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.repository.ConditionTemplateRepository;
import io.spring.identityadmin.repository.ManagedResourceRepository;
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
    private final ObjectMapper objectMapper;
    private final PermissionService permissionService;
    private final ModelMapper modelMapper;
    private static final Pattern SPEL_VARIABLE_PATTERN = Pattern.compile("#(\\w+)");
    private static final Set<String> GLOBAL_CONTEXT_VARIABLES = Set.of("#authentication", "#request", "#ai");


    /**
     * 시각적 정책 빌더 UI 페이지를 렌더링합니다.
     * 빌더의 팔레트를 채우기 위해 필요한 모든 구성요소(주체, 권한, 조건)를 모델에 담아 전달합니다.
     */
    @GetMapping
    public String policyBuilder(Model model) {

        // Entity 대신 DTO 사용으로 순환 참조 방지
        List<RoleDto> roleDtos = roleService.getRolesWithoutExpression().stream()
                .map(role -> RoleDto.builder()
                        .id(role.getId())
                        .roleName(role.getRoleName())
                        .roleDesc(role.getRoleDesc())
                        .build())
                .collect(Collectors.toList());

        List<PermissionDto> permissionDtos = permissionCatalogService.getAvailablePermissions().stream()
                .map(permission -> PermissionDto.builder()
                        .id(permission.getId())
                        .name(permission.getName())
                        .friendlyName(permission.getFriendlyName())
                        .description(permission.getDescription())
                        .targetType(permission.getTargetType())
                        .actionType(permission.getActionType())
                        .build())
                .collect(Collectors.toList());

        model.addAttribute("allRoles", roleDtos);
        model.addAttribute("allPermissions", permissionDtos);
        
        // 🔧 개선: 리소스 컨텍스트가 없을 때 기본 컨텍스트 설정
        if (!model.containsAttribute("resourceContext")) {
            Map<String, Object> defaultContext = createDefaultResourceContext();
            model.addAttribute("resourceContext", defaultContext);
            log.info("🔧 정책 빌더 직접 접근: 기본 리소스 컨텍스트 설정됨");
        }
        
        addContextAwareConditionsToModel(model);

        model.addAttribute("activePage", "policy-builder");
        return "admin/policy-builder";
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

        ManagedResource resource = managedResourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));

        // 리소스에 사용 가능한 컨텍스트 변수 정보
        Map<String, Object> resourceContext = new HashMap<>();
        resourceContext.put("resourceIdentifier", resource.getResourceIdentifier());
        try {
            resourceContext.put("parameterTypes", objectMapper.readValue(resource.getParameterTypes(), new TypeReference<>() {}));
        } catch (Exception e) {
            resourceContext.put("parameterTypes", Collections.emptyList());
        }
        resourceContext.put("returnObjectType", resource.getReturnType());

        model.addAttribute("resourceContext", resourceContext);

        // Permission을 DTO로 변환하여 전달
        permissionService.getPermission(permissionId)
                .ifPresent(permission -> {
                    PermissionDto permissionDto = PermissionDto.builder()
                            .id(permission.getId())
                            .name(permission.getName())
                            .friendlyName(permission.getFriendlyName())
                            .description(permission.getDescription())
                            .build();
                    model.addAttribute("preselectedPermission", permissionDto);
                });

        // 기존 policyBuilder 메서드를 호출하여 공통 데이터 추가 및 뷰 렌더링
        return policyBuilder(model);
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