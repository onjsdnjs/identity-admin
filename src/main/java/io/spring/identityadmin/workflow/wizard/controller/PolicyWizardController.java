package io.spring.identityadmin.workflow.wizard.controller;

import io.spring.identityadmin.admin.iam.service.GroupService;
import io.spring.identityadmin.admin.iam.service.UserManagementService;
import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.studio.dto.InitiateGrantRequestDto;
import io.spring.identityadmin.workflow.wizard.dto.CommitPolicyRequest;
import io.spring.identityadmin.workflow.wizard.dto.SavePermissionsRequest;
import io.spring.identityadmin.workflow.wizard.dto.SaveSubjectsRequest;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import io.spring.identityadmin.workflow.wizard.service.PermissionWizardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/policy-wizard")
@RequiredArgsConstructor
public class PolicyWizardController {

    private final PermissionWizardService wizardService;
    private final UserManagementService userManagementService;
    private final GroupService groupService;
    private final PermissionCatalogService permissionCatalogService;

    @PostMapping("/start")
    public String startWizardFromStudio(@ModelAttribute InitiateGrantRequestDto request) {
        String policyName = "Studio에서 시작된 정책 - " + System.currentTimeMillis();
        String policyDescription = "Authorization Studio의 빠른 실행을 통해 생성된 정책입니다.";
        var initiation = wizardService.beginCreation(request, policyName, policyDescription);
        return "redirect:/admin/policy-wizard/" + initiation.wizardContextId();
    }

    @GetMapping("/{contextId}")
    public String getWizardPage(@PathVariable String contextId, Model model) {
        // ... (기존과 동일)
    }

    @PostMapping("/{contextId}/subjects")
    public ResponseEntity<WizardContext> saveSubjects(@PathVariable String contextId, @RequestBody SaveSubjectsRequest request) {
        return ResponseEntity.ok(wizardService.updateSubjects(contextId, request));
    }

    @PostMapping("/{contextId}/permissions")
    public ResponseEntity<WizardContext> savePermissions(@PathVariable String contextId, @RequestBody SavePermissionsRequest request) {
        return ResponseEntity.ok(wizardService.updatePermissions(contextId, request));
    }

    @PostMapping("/{contextId}/commit")
    public ResponseEntity<Policy> commitPolicy(@PathVariable String contextId, @RequestBody CommitPolicyRequest request) {
        return ResponseEntity.ok(wizardService.commitPolicy(contextId, request));
    }
}
