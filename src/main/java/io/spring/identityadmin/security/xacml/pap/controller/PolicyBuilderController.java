package io.spring.identityadmin.security.xacml.pap.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.identityadmin.admin.iam.service.GroupService;
import io.spring.identityadmin.admin.iam.service.PermissionService;
import io.spring.identityadmin.admin.iam.service.UserManagementService;
import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.domain.dto.ConditionTemplateDto;
import io.spring.identityadmin.domain.entity.ConditionTemplate;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.repository.ConditionTemplateRepository;
import io.spring.identityadmin.repository.ManagedResourceRepository;
import io.spring.identityadmin.security.xacml.pap.dto.VisualPolicyDto;
import io.spring.identityadmin.security.xacml.pap.service.PolicyBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/admin/policy-builder")
@RequiredArgsConstructor
public class PolicyBuilderController {

    private final PolicyBuilderService policyBuilderService;
    private final UserManagementService userManagementService;
    private final GroupService groupService;
    private final PermissionCatalogService permissionCatalogService;
    private final ConditionTemplateRepository conditionTemplateRepository;
    private final ManagedResourceRepository managedResourceRepository;
    private final ObjectMapper objectMapper;
    private final PermissionService permissionService;
    private static final Pattern SPEL_VARIABLE_PATTERN = Pattern.compile("#(\\w+)");

    /**
     * 시각적 정책 빌더 UI 페이지를 렌더링합니다.
     * 빌더의 팔레트를 채우기 위해 필요한 모든 구성요소(주체, 권한, 조건)를 모델에 담아 전달합니다.
     */
    @GetMapping
    public String policyBuilder(Model model) {
        model.addAttribute("allUsers", userManagementService.getUsers());
        model.addAttribute("allGroups", groupService.getAllGroups());
        model.addAttribute("allPermissions", permissionCatalogService.getAvailablePermissions());

        // [핵심 수정] 컨텍스트 인지형 조건 목록 생성
        addContextAwareConditionsToModel(model);

        model.addAttribute("activePage", "policy-builder");
        return "admin/policy-builder";
    }

    /**
     * [신규] 모델에 '컨텍스트 인지형' 조건 템플릿 목록을 추가하는 헬퍼 메서드
     */
    private void addContextAwareConditionsToModel(Model model) {
        List<ConditionTemplate> allConditions = conditionTemplateRepository.findAll();
        Set<String> availableVars = getAvailableVariablesFromModel(model);

        List<ConditionTemplateDto> conditionDtos = allConditions.stream()
                .map(cond -> {
                    Set<String> requiredVars = extractVariablesFromSpel(cond.getSpelTemplate());
                    boolean isCompatible = availableVars.containsAll(requiredVars);
                    return new ConditionTemplateDto(
                            cond.getId(),
                            cond.getName(),
                            cond.getDescription(),
                            requiredVars,
                            isCompatible
                    );
                }).toList();

        model.addAttribute("allConditions", conditionDtos);
    }

    /**
     * [신규] 모델에서 사용 가능한 컨텍스트 변수 Set을 추출하는 헬퍼 메서드
     */
    private Set<String> getAvailableVariablesFromModel(Model model) {
        if (model.containsAttribute("resourceContext")) {
            Map<String, Object> rc = (Map<String, Object>) model.getAttribute("resourceContext");
            List<String> variables = (List<String>) rc.get("availableVariables");
            return new HashSet<>(variables);
        }
        // 리소스 컨텍스트가 없으면, 모든 변수가 사용 가능하다고 가정 (전역 정책 생성 시)
        return Set.of("#authentication", "#request", "#ai"); // 예시 기본 변수
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
    @PostMapping("/from-resource")
    public String policyBuilderFromResource(@RequestParam Long resourceId, @RequestParam Long permissionId, Model model) {
        ManagedResource resource = managedResourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));

        // 리소스에 사용 가능한 컨텍스트 변수 정보
        Map<String, Object> resourceContext = new HashMap<>();
        resourceContext.put("resourceIdentifier", resource.getResourceIdentifier());
        try {
            List<String> variables = objectMapper.readValue(resource.getAvailableContextVariables(), new TypeReference<>() {});
            resourceContext.put("availableVariables", variables);
        } catch (IOException | NullPointerException e) {
            resourceContext.put("availableVariables", List.of());
        }

        model.addAttribute("resourceContext", resourceContext);
        permissionService.getPermission(permissionId)
                .ifPresent(permission -> model.addAttribute("preselectedPermission", permission));

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