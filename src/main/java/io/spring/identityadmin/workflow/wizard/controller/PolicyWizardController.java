package io.spring.identityadmin.workflow.wizard.controller;

import io.spring.identityadmin.admin.iam.service.GroupService;
import io.spring.identityadmin.admin.iam.service.PermissionService;
import io.spring.identityadmin.admin.iam.service.UserManagementService;
import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.domain.dto.PermissionDto;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.domain.entity.policy.PolicyCondition;
import io.spring.identityadmin.studio.dto.InitiateGrantRequestDto;
import io.spring.identityadmin.workflow.wizard.dto.CommitPolicyRequest;
import io.spring.identityadmin.workflow.wizard.dto.SavePermissionsRequest;
import io.spring.identityadmin.workflow.wizard.dto.SaveSubjectsRequest;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import io.spring.identityadmin.workflow.wizard.service.PermissionWizardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

/**
 * [최종 구현] 권한 부여 마법사의 전체 UI 흐름과 각 단계별 API 요청을 처리하는 컨트롤러입니다.
 * 명확한 DTO를 사용하여 안정적인 API 계약을 보장하고, PermissionWizardService에 작업을 위임합니다.
 */
@Slf4j
@Controller
@RequestMapping("/admin/policy-wizard")
@RequiredArgsConstructor
public class PolicyWizardController {

    private final PermissionWizardService wizardService;
    private final UserManagementService userManagementService;
    private final GroupService groupService;
    private final PermissionCatalogService permissionCatalogService;
    private final PermissionService permissionService; // 의존성 주입
    private final ModelMapper modelMapper;             // 의존성 주입

    /**
     * Authorization Studio 등 외부에서 권한 부여 워크플로우를 시작하는 진입점입니다.
     * 초기 데이터를 받아 마법사 컨텍스트를 생성하고, 생성된 컨텍스트 ID를 포함한 마법사 첫 페이지로 리다이렉트합니다.
     */
    @PostMapping("/start")
    public String startWizard(@ModelAttribute InitiateGrantRequestDto request, RedirectAttributes ra) {
        String policyName = "마법사 생성 정책 - " + System.currentTimeMillis();
        String policyDescription = "권한 부여 마법사를 통해 생성된 정책입니다.";
        var initiation = wizardService.beginCreation(request, policyName, policyDescription);
        log.info("Redirecting to wizard page with contextId: {}", initiation.contextId());
        return "redirect:/admin/policy-wizard/" + initiation.contextId();
    }

    /**
     * 마법사 UI 페이지를 렌더링합니다.
     * URL 경로의 contextId를 사용하여 현재 진행 중인 마법사의 상태를 조회하고,
     * 각 단계에 필요한 데이터(전체 사용자/그룹/권한 목록)를 모델에 담아 뷰에 전달합니다.
     */
    @GetMapping("/{contextId}")
    public String getWizardPage(
        @PathVariable String contextId,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String permName,
        Model model,
        RedirectAttributes ra) {

        WizardContext context;
        // 1. 모델에 Flash Attribute로 전달된 wizardContext가 있는지 확인
        if (model.containsAttribute("wizardContext")) {
            context = (WizardContext) model.asMap().get("wizardContext");
            log.info("Retrieved WizardContext from Flash Attributes for ID: {}", contextId);
        } else {
            // 2. 없다면(예: 페이지 새로고침) DB에서 조회
            log.info("No Flash Attribute found. Retrieving WizardContext from DB for ID: {}", contextId);
            context = wizardService.getWizardProgress(contextId);
        }

        if (context == null) {
            throw new IllegalStateException("유효하지 않거나 만료된 마법사 세션입니다.");
        }

        // 워크벤치에서 넘어온 경우, 권한 정보 처리
        if (model.containsAttribute("fromWorkbench") && !CollectionUtils.isEmpty(context.permissionIds())) {
            Long preselectedPermissionId = context.permissionIds().iterator().next();
            permissionService.getPermission(preselectedPermissionId)
                    .ifPresent(permission -> {
                        PermissionDto permissionDto = modelMapper.map(permission, PermissionDto.class);
                        model.addAttribute("preselectedPermission", permissionDto);
                        String friendlyName = permission.getFriendlyName() != null ? permission.getFriendlyName() : permission.getName();
                        model.addAttribute("message", "권한 '" + friendlyName + "'이(가) 생성되었습니다. 이제 이 권한을 부여할 주체(역할/그룹)를 선택하세요.");
                    });
        }

        model.addAttribute("wizardContext", context);
        model.addAttribute("allUsers", userManagementService.getUsers());
        model.addAttribute("allGroups", groupService.getAllGroups());
        model.addAttribute("allPermissions", permissionCatalogService.getAvailablePermissions());
        model.addAttribute("activePage", "policy-wizard");
        return "admin/policy-wizard";
    }

    /**
     * API: Step 1(주체 선택)의 데이터를 받아 마법사 컨텍스트를 업데이트합니다.
     */
    @PostMapping("/{contextId}/subjects")
    public ResponseEntity<WizardContext> saveSubjects(@PathVariable String contextId, @RequestBody SaveSubjectsRequest request) {
        log.debug("API: Saving subjects for contextId: {}", contextId);
        WizardContext updatedContext = wizardService.updateSubjects(contextId, request);
        return ResponseEntity.ok(updatedContext);
    }

    /**
     * API: Step 2(권한 선택)의 데이터를 받아 마법사 컨텍스트를 업데이트합니다.
     */
    @PostMapping("/{contextId}/permissions")
    public ResponseEntity<WizardContext> savePermissions(@PathVariable String contextId, @RequestBody SavePermissionsRequest request) {
        log.debug("API: Saving permissions for contextId: {}", contextId);
        WizardContext updatedContext = wizardService.updatePermissions(contextId, request);
        return ResponseEntity.ok(updatedContext);
    }

    /**
     * API: Step 3(검토 및 생성)에서 최종 정책을 생성하고 저장합니다.
     */
    @PostMapping("/{contextId}/commit")
    public ResponseEntity<PolicyDto> commitPolicy(@PathVariable String contextId, @RequestBody CommitPolicyRequest request) {
        log.debug("API: Committing policy for contextId: {}", contextId);
        wizardService.updatePolicyDetails(contextId, request.policyName(), request.policyDescription());

        PolicyDto policyDto = wizardService.commitPolicy(contextId);

        // [신규] 엔티티를 안전한 DTO로 변환하는 과정을 컨트롤러에 추가합니다.
//        PolicyDto responseDto = toDto(createdPolicy);

        return ResponseEntity.ok(policyDto);
    }

    /**
     * [신규] Policy 엔티티를 PolicyDto로 안전하게 변환하는 private 헬퍼 메서드.
     * 순환 참조 문제를 회피하고 필요한 데이터만 담습니다.
     */
    private PolicyDto toDto(Policy policy) {
        PolicyDto dto = new PolicyDto();
        dto.setId(policy.getId());
        dto.setName(policy.getName());
        dto.setDescription(policy.getDescription());
        dto.setEffect(policy.getEffect());
        dto.setPriority(policy.getPriority());

        if (policy.getTargets() != null) {
            dto.setTargets(policy.getTargets().stream().map(t ->
                    new PolicyDto.TargetDto(t.getTargetType(), t.getTargetIdentifier(), t.getHttpMethod() == null ? "ALL" : t.getHttpMethod())
            ).collect(Collectors.toList()));
        }

        if (policy.getRules() != null) {
            dto.setRules(policy.getRules().stream().map(rule -> {
                PolicyDto.RuleDto ruleDto = new PolicyDto.RuleDto();
                ruleDto.setDescription(rule.getDescription());
                if (rule.getConditions() != null) {
                    ruleDto.setConditions(rule.getConditions().stream()
                            .map(PolicyCondition::getExpression)
                            .collect(Collectors.toList()));
                }
                return ruleDto;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}