package io.spring.identityadmin.workflow.wizard.controller;

import io.spring.identityadmin.admin.iam.service.GroupService;
import io.spring.identityadmin.admin.iam.service.UserManagementService;
import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.studio.dto.InitiateGrantRequestDto;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import io.spring.identityadmin.workflow.wizard.service.PermissionWizardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/policy-wizard")
@RequiredArgsConstructor
public class PolicyWizardController {

    private final PermissionWizardService wizardService;
    private final UserManagementService userManagementService;
    private final GroupService groupService;
    private final PermissionCatalogService permissionCatalogService;

    /**
     * Studio 등에서 전달된 초기 정보를 바탕으로 마법사를 시작하고, UI 페이지로 리다이렉트합니다.
     */
    @PostMapping("/start")
    public String startWizardFromStudio(@ModelAttribute InitiateGrantRequestDto request) {
        String policyName = "Studio에서 시작된 정책 - " + System.currentTimeMillis();
        String policyDescription = "Authorization Studio의 빠른 실행을 통해 생성된 정책입니다.";
        var initiation = wizardService.beginCreation(request, policyName, policyDescription);
        return "redirect:/admin/policy-wizard/" + initiation.wizardContextId();
    }

    /**
     * 마법사 UI 페이지를 렌더링합니다.
     * 마법사의 각 단계에서 필요한 데이터(사용자 목록, 권한 목록 등)를 모델에 담아 전달합니다.
     */
    @GetMapping("/{contextId}")
    public String getWizardPage(@PathVariable String contextId, Model model) {
        WizardContext context = wizardService.getWizardProgress(contextId);
        model.addAttribute("wizardContext", context);
        model.addAttribute("allUsers", userManagementService.getUsers());
        model.addAttribute("allGroups", groupService.getAllGroups());
        model.addAttribute("allPermissions", permissionCatalogService.getAvailablePermissions());
        return "admin/policy-wizard";
    }

    /**
     * Step 1: 주체 선택 데이터를 저장하는 API
     */
    @PostMapping("/{contextId}/subjects")
    public ResponseEntity<WizardContext> saveSubjects(@PathVariable String contextId, @RequestBody Map<String, Set<Long>> payload) {
        Set<WizardContext.Subject> subjects = payload.get("userIds").stream()
                .map(id -> new WizardContext.Subject(id, "USER"))
                .collect(Collectors.toSet());
        payload.get("groupIds").stream()
                .map(id -> new WizardContext.Subject(id, "GROUP"))
                .forEach(subjects::add);

        WizardContext updatedContext = wizardService.updateSubjects(contextId, subjects);
        return ResponseEntity.ok(updatedContext);
    }

    /**
     * Step 2: 권한 선택 데이터를 저장하는 API
     */
    @PostMapping("/{contextId}/permissions")
    public ResponseEntity<WizardContext> savePermissions(@PathVariable String contextId, @RequestBody Map<String, Set<Long>> payload) {
        WizardContext updatedContext = wizardService.addPermissions(contextId, payload.get("permissionIds"));
        return ResponseEntity.ok(updatedContext);
    }

    /**
     * Step 3: 최종 정책을 생성하고 저장하는 API
     */
    @PostMapping("/{contextId}/commit")
    public ResponseEntity<Policy> commitPolicy(@PathVariable String contextId, @RequestBody Map<String, String> payload) {
        wizardService.updatePolicyDetails(contextId, payload.get("policyName"), payload.get("policyDescription"));
        Policy createdPolicy = wizardService.commitPolicy(contextId);
        return ResponseEntity.ok(createdPolicy);
    }
}
