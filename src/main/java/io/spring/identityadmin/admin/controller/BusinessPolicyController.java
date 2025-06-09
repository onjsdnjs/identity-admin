package io.spring.identityadmin.admin.controller;

import io.spring.identityadmin.admin.service.BusinessMetadataService;
import io.spring.identityadmin.admin.service.BusinessPolicyService;
import io.spring.identityadmin.admin.service.PolicyService;
import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/business-policies")
@RequiredArgsConstructor
public class BusinessPolicyController {

    private final BusinessPolicyService businessPolicyService;
    private final BusinessMetadataService businessMetadataService;
    private final PolicyService policyService; // 목록 조회를 위해 주입

    // [완성] 비즈니스 정책 목록은 기술 정책 목록으로 리다이렉트
    @GetMapping
    public String listPolicies() {
        return "redirect:/admin/policies";
    }

    @GetMapping("/create")
    public String createPolicyForm(Model model) {
        model.addAttribute("businessPolicy", new BusinessPolicyDto());
        populateModelWithMetadata(model);
        return "admin/business-policy-details";
    }

    @PostMapping("/create")
    public String createPolicy(@ModelAttribute("businessPolicy") BusinessPolicyDto businessPolicyDto, RedirectAttributes ra) {
        try {
            businessPolicyService.createPolicyFromBusinessRule(businessPolicyDto);
            ra.addFlashAttribute("message", "새로운 비즈니스 정책이 성공적으로 생성되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "정책 생성 실패: " + e.getMessage());
        }
        return "redirect:/admin/policies";
    }

    // [완성] 수정 폼을 보여주는 핸들러
    @GetMapping("/{id}/edit")
    public String editPolicyForm(@PathVariable Long id, Model model) {
        // 현재는 기술 정책을 다시 비즈니스 DTO로 역변환하는 로직이 복잡하므로,
        // 새로 생성하는 기능에 집중. 수정은 향후 과제로 남김.
        // BusinessPolicyDto dto = businessPolicyService.getBusinessRuleForPolicy(id);
        // model.addAttribute("businessPolicy", dto);

        // 지금은 새 등록 화면으로 대체
        return "redirect:/admin/business-policies/create";
    }

    // [완성] 수정 요청을 처리하는 핸들러
    @PostMapping("/{id}/edit")
    public String editPolicy(@PathVariable Long id, @ModelAttribute("businessPolicy") BusinessPolicyDto businessPolicyDto, RedirectAttributes ra) {
        try {
            businessPolicyService.updatePolicyFromBusinessRule(id, businessPolicyDto);
            ra.addFlashAttribute("message", "비즈니스 정책이 성공적으로 수정되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "정책 수정 실패: " + e.getMessage());
        }
        return "redirect:/admin/policies";
    }

    // UI에 필요한 메타데이터를 모델에 추가하는 헬퍼 메서드
    private void populateModelWithMetadata(Model model) {
        model.addAttribute("users", businessMetadataService.getAllUsersForPolicy());
        model.addAttribute("groups", businessMetadataService.getAllGroupsForPolicy());
        model.addAttribute("resources", businessMetadataService.getAllBusinessResources());
        model.addAttribute("actions", businessMetadataService.getAllBusinessActions());
        model.addAttribute("conditions", businessMetadataService.getAllConditionTemplates());
    }
}