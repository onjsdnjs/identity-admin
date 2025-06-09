package io.spring.identityadmin.admin.controller;

import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.entity.policy.Policy;
import io.spring.identityadmin.admin.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/policies")
@RequiredArgsConstructor
@Slf4j
public class PolicyController {

    private final PolicyService policyService;
    private final ModelMapper modelMapper;

    @GetMapping
    public String listPolicies(Model model) {
        List<Policy> policies = policyService.getAllPolicies();
        model.addAttribute("policies", policies);
        return "admin/policies"; // admin/policies.html
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("policy", new PolicyDto());
        return "admin/policydetails"; // admin/policydetails.html
    }

    @PostMapping
    public String createPolicy(@ModelAttribute PolicyDto policyDto, RedirectAttributes ra) {
        try {
            policyService.createPolicy(policyDto);
            ra.addFlashAttribute("message", "정책이 성공적으로 생성되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "정책 생성 실패: " + e.getMessage());
            log.error("Error creating policy", e);
        }
        return "redirect:/admin/policies";
    }

    @GetMapping("/{id}")
    public String detailForm(@PathVariable Long id, Model model) {
        Policy policy = policyService.findById(id);
        PolicyDto dto = modelMapper.map(policy, PolicyDto.class);

        // Entity -> DTO 변환 (Target, Condition을 String 리스트로)
        dto.setTargets(policy.getTargets().stream()
                .map(t -> t.getTargetType() + ":" + t.getTargetIdentifier())
                .collect(Collectors.toList()));
        dto.setConditions(policy.getRules().stream()
                .flatMap(r -> r.getConditions().stream())
                .map(c -> c.getExpression())
                .collect(Collectors.toList()));

        model.addAttribute("policy", dto);
        return "admin/policydetails";
    }

    @PostMapping("/{id}/edit")
    public String updatePolicy(@PathVariable Long id, @ModelAttribute PolicyDto policyDto, RedirectAttributes ra) {
        try {
            policyDto.setId(id);
            policyService.updatePolicy(policyDto);
            ra.addFlashAttribute("message", "정책이 성공적으로 수정되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "정책 수정 실패: " + e.getMessage());
            log.error("Error updating policy", e);
        }
        return "redirect:/admin/policies";
    }

    @GetMapping("/delete/{id}")
    public String deletePolicy(@PathVariable Long id, RedirectAttributes ra) {
        try {
            policyService.deletePolicy(id);
            ra.addFlashAttribute("message", "정책이 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "정책 삭제 실패: " + e.getMessage());
            log.error("Error deleting policy", e);
        }
        return "redirect:/admin/policies";
    }
}
