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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    /**
     * Authorization Studio 등 외부에서 권한 부여 워크플로우를 시작하는 진입점입니다.
     * 초기 데이터를 받아 마법사 컨텍스트를 생성하고, 생성된 컨텍스트 ID를 포함한 마법사 첫 페이지로 리다이렉트합니다.
     */
    @PostMapping("/start")
    public String startWizard(@ModelAttribute InitiateGrantRequestDto request, RedirectAttributes ra) {
        try {
            String policyName = "마법사 생성 정책 - " + System.currentTimeMillis();
            String policyDescription = "권한 부여 마법사를 통해 생성된 정책입니다.";
            var initiation = wizardService.beginCreation(request, policyName, policyDescription);
            log.info("Redirecting to wizard page with contextId: {}", initiation.wizardContextId());
            return "redirect:/admin/policy-wizard/" + initiation.wizardContextId();
        } catch (Exception e) {
            log.error("Error starting policy wizard", e);
            ra.addFlashAttribute("errorMessage", "마법사 시작 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/admin/studio"; // 오류 발생 시 Studio로 복귀
        }
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
        try {
            WizardContext context = wizardService.getWizardProgress(contextId);
            if (context == null) {
                throw new IllegalStateException("유효하지 않거나 만료된 마법사 세션입니다.");
            }

            // [핵심 수정] 워크벤치에서 넘어온 경우, 성공 메시지를 Model에 추가
            if ("workbench".equals(from) && permName != null) {
                model.addAttribute("message", "리소스가 권한 '" + permName + "'으로 정의되었습니다. 이제 이 권한을 역할에 할당하세요.");
            }

            model.addAttribute("wizardContext", context);
            model.addAttribute("allUsers", userManagementService.getUsers());
            model.addAttribute("allGroups", groupService.getAllGroups());
            model.addAttribute("allPermissions", permissionCatalogService.getAvailablePermissions());
            model.addAttribute("activePage", "policy-wizard");
            return "admin/policy-wizard";
        } catch (IllegalStateException e) {
            log.warn("Failed to get wizard progress for contextId {}: {}", contextId, e.getMessage());
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/studio"; // 오류 발생 시 안전한 스튜디오 페이지로 이동
        }
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
    public ResponseEntity<Policy> commitPolicy(@PathVariable String contextId, @RequestBody CommitPolicyRequest request) {
        log.debug("API: Committing policy for contextId: {}", contextId);
        // 최종 단계에서 사용자가 입력한 정책 이름/설명을 컨텍스트에 반영
        wizardService.updatePolicyDetails(contextId, request.policyName(), request.policyDescription());
        // 최종 정책 생성
        Policy createdPolicy = wizardService.commitPolicy(contextId);
        return ResponseEntity.ok(createdPolicy);
    }
}