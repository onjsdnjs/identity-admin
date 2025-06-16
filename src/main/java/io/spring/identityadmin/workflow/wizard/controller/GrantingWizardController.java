package io.spring.identityadmin.workflow.wizard.controller;

import io.spring.identityadmin.admin.iam.service.GroupService;
import io.spring.identityadmin.admin.iam.service.RoleService;
import io.spring.identityadmin.admin.iam.service.UserManagementService;
import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.domain.entity.Group;
import io.spring.identityadmin.studio.dto.SimulationResultDto;
import io.spring.identityadmin.workflow.wizard.dto.AssignmentChangeDto;
import io.spring.identityadmin.workflow.wizard.dto.InitiateManagementRequestDto;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import io.spring.identityadmin.workflow.wizard.service.GrantingWizardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;

@Controller
@RequestMapping("/admin/granting-wizard")
@RequiredArgsConstructor
public class GrantingWizardController {

    private final GrantingWizardService grantingWizardService;
    private final UserManagementService userManagementService;
    private final GroupService groupService;
    private final RoleService roleService;

    @PostMapping("/start")
    public String startManagementSession(@ModelAttribute InitiateManagementRequestDto request, RedirectAttributes ra) {
        try {
            var initiation = grantingWizardService.beginManagementSession(request);
            return "redirect:/admin/granting-wizard/" + initiation.wizardContextId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "권한 관리 세션 시작 중 오류 발생: " + e.getMessage());
            return "redirect:/admin/studio"; // 오류 발생 시 이전 페이지로
        }
    }

    @GetMapping("/{contextId}")
    public String getWizardPage(@PathVariable String contextId, Model model) {
        WizardContext context = grantingWizardService.getWizardProgress(contextId);
        WizardContext.Subject subject = context.targetSubject();

        String subjectName = "알 수 없음";
        String subjectType = "UNKNOWN";
        Object allAssignments = Collections.emptyList();
        Object selectedAssignmentIds = Collections.emptyList();

        if (subject != null) {
            subjectType = subject.type();
            if ("USER".equalsIgnoreCase(subjectType)) {
                UserDto user = userManagementService.getUser(subject.id());
                subjectName = user.getName();
                allAssignments = groupService.getAllGroups();
                selectedAssignmentIds = user.getSelectedGroupIds();
                model.addAttribute("assignmentType", "GROUP");
            } else if ("GROUP".equalsIgnoreCase(subjectType)) {
                Group group = groupService.getGroup(subject.id()).orElseThrow();
                subjectName = group.getName();
                allAssignments = roleService.getRoles();
                selectedAssignmentIds = group.getGroupRoles().stream().map(gr -> gr.getRole().getId()).toList();
                model.addAttribute("assignmentType", "ROLE");
            }
        }

        model.addAttribute("contextId", contextId);
        model.addAttribute("subjectName", subjectName);
        model.addAttribute("subjectType", subjectType);
        model.addAttribute("allAssignments", allAssignments);
        model.addAttribute("selectedAssignmentIds", selectedAssignmentIds);

        return "admin/granting-wizard";
    }

    @PostMapping("/{contextId}/commit")
    public String commitAssignments(@PathVariable String contextId,
                                    @ModelAttribute AssignmentChangeDto finalAssignments,
                                    RedirectAttributes ra) {
        try {
            grantingWizardService.commitAssignments(contextId, finalAssignments);
            ra.addFlashAttribute("message", "권한 할당이 성공적으로 저장되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "저장 중 오류 발생: " + e.getMessage());
        }
        // TODO: 작업 완료 후 원래 주체 목록 페이지 등으로 이동
        return "redirect:/admin/users";
    }

    /**
     * [신규 API]
     * 멤버십 변경사항을 실시간으로 시뮬레이션합니다.
     */
    @PostMapping("/{contextId}/simulate")
    @ResponseBody
    public ResponseEntity<SimulationResultDto> simulateChanges(
            @PathVariable String contextId,
            @RequestBody AssignmentChangeDto changes) {
        SimulationResultDto result = grantingWizardService.simulateAssignmentChanges(contextId, changes);
        return ResponseEntity.ok(result);
    }
}