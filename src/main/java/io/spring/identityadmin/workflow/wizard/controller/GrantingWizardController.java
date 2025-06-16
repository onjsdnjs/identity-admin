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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
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
            return "redirect:" + initiation.wizardUrl();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "권한 관리 세션 시작 중 오류 발생: " + e.getMessage());
            return "redirect:/admin/studio";
        }
    }

    @GetMapping("/{contextId}")
    public String getWizardPage(@PathVariable String contextId, Model model, RedirectAttributes ra) {
        try {
            WizardContext context = grantingWizardService.getWizardProgress(contextId);
            if (context == null) {
                throw new IllegalStateException("유효하지 않거나 만료된 마법사 세션입니다.");
            }
            WizardContext.Subject subject = context.targetSubject();

            String subjectName = "알 수 없음";
            String assignmentType = "UNKNOWN";
            Object allAssignments = Collections.emptyList();
            Object selectedAssignmentIds = Collections.emptyList();

            if (subject != null) {
                if ("USER".equalsIgnoreCase(subject.type())) {
                    UserDto user = userManagementService.getUser(subject.id());
                    subjectName = user.getName();
                    allAssignments = groupService.getAllGroups();
                    selectedAssignmentIds = user.getSelectedGroupIds();
                    assignmentType = "GROUP";
                } else if ("GROUP".equalsIgnoreCase(subject.type())) {
                    Group group = groupService.getGroup(subject.id()).orElseThrow();
                    subjectName = group.getName();
                    allAssignments = roleService.getRoles();
                    selectedAssignmentIds = group.getGroupRoles().stream().map(gr -> gr.getRole().getId()).collect(Collectors.toSet());
                    assignmentType = "ROLE";
                }
            }

            model.addAttribute("contextId", contextId);
            model.addAttribute("subjectName", subjectName);
            model.addAttribute("subjectType", subject.type());
            model.addAttribute("allAssignments", allAssignments);
            model.addAttribute("selectedAssignmentIds", selectedAssignmentIds);
            model.addAttribute("assignmentType", assignmentType);

            return "admin/granting-wizard";
        } catch(Exception e) {
            log.error("Error loading wizard page for context {}", contextId, e);
            ra.addFlashAttribute("errorMessage", "마법사 페이지 로딩 중 오류 발생: " + e.getMessage());
            return "redirect:/admin/studio";
        }
    }

    @PostMapping("/{contextId}/commit")
    public String commitAssignments(@PathVariable String contextId,
                                    @ModelAttribute AssignmentChangeDto finalAssignments,
                                    RedirectAttributes ra) {
        try {
            grantingWizardService.commitAssignments(contextId, finalAssignments);
            ra.addFlashAttribute("message", "멤버십 할당이 성공적으로 저장되었습니다.");
        } catch (Exception e) {
            log.error("Error committing assignments for context {}", contextId, e);
            ra.addFlashAttribute("errorMessage", "저장 중 오류 발생: " + e.getMessage());
        }

        // 저장이 완료되면 Studio 페이지로 리다이렉트
        return "redirect:/admin/studio";
    }

    @PostMapping("/{contextId}/simulate")
    @ResponseBody
    public ResponseEntity<SimulationResultDto> simulateChanges(
            @PathVariable String contextId,
            @RequestBody AssignmentChangeDto changes) {
        SimulationResultDto result = grantingWizardService.simulateAssignmentChanges(contextId, changes);
        return ResponseEntity.ok(result);
    }
}