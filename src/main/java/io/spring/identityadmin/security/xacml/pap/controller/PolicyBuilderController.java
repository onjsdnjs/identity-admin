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
        addContextAwareConditionsToModel(model);

        model.addAttribute("activePage", "policy-builder");
        return "admin/policy-builder";
    }

    /**
     * 모델에 '컨텍스트 인지형' 조건 템플릿 목록을 추가하는 헬퍼 메서드
     */
    private void addContextAwareConditionsToModel(Model model) {
        List<ConditionTemplate> allConditions = conditionTemplateRepository.findAll();

        // 현재 컨텍스트에서 사용 가능한 모든 '타입' 목록을 계산합니다.
        Set<String> availableTypes = getAvailableTypesFromModel(model);

        List<ConditionTemplateDto> conditionDtos = allConditions.stream().map(cond -> {
            String requiredType = cond.getRequiredTargetType();

            // [핵심] 호환성 여부를 '타입' 기반으로 정확하게 판단합니다.
            // 템플릿이 특정 타입을 요구하지 않거나, 요구하는 타입이 사용 가능한 타입 목록에 있으면 활성화됩니다.
            boolean isCompatible = !StringUtils.hasText(requiredType) || availableTypes.contains(requiredType);

            return new ConditionTemplateDto(cond.getId(), cond.getName(), cond.getDescription(),
                    // UI 표시용으로 required_context_variables를 그대로 전달할 수 있음
                    Set.of(requiredType != null ? requiredType : ""),
                    isCompatible);
        }).toList();

        model.addAttribute("allConditions", conditionDtos);
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