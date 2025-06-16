package io.spring.identityadmin.workflow.wizard.controller;

import io.spring.identityadmin.admin.iam.service.GroupService;
import io.spring.identityadmin.admin.iam.service.RoleService;
import io.spring.identityadmin.admin.iam.service.UserManagementService;
import io.spring.identityadmin.studio.dto.SimulationResultDto;
import io.spring.identityadmin.workflow.wizard.dto.AssignmentChangeDto;
import io.spring.identityadmin.workflow.wizard.dto.InitiateManagementRequestDto;
import io.spring.identityadmin.workflow.wizard.service.GrantingWizardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        // TODO: contextId로 관리 대상 주체 정보를 가져와서,
        // user/group에 따라 다른 데이터를 모델에 담아야 함
        // 예시: 주체가 User이면, 전체 Group 목록과 선택된 Group 목록을 전달
        model.addAttribute("allGroups", groupService.getAllGroups());
        model.addAttribute("selectedGroupIds", java.util.Collections.emptyList()); // 실제 데이터로 교체 필요
        model.addAttribute("contextId", contextId);

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