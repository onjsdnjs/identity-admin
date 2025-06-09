package io.spring.identityadmin.admin.controller;

import io.spring.identityadmin.admin.service.BusinessMetadataService;
import io.spring.identityadmin.admin.service.BusinessPolicyService;
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

    @GetMapping("/create")
    public String createPolicyForm(Model model) {
        // UI에 필요한 모든 선택 목록 데이터를 모델에 추가
        model.addAttribute("businessPolicy", new BusinessPolicyDto());
        model.addAttribute("users", businessMetadataService.getAllUsersForPolicy());
        model.addAttribute("groups", businessMetadataService.getAllGroupsForPolicy());
        model.addAttribute("resources", businessMetadataService.getAllBusinessResources());
        model.addAttribute("actions", businessMetadataService.getAllBusinessActions());
        model.addAttribute("conditions", businessMetadataService.getAllConditionTemplates());
        return "admin/business-policy-details";
    }

    @PostMapping
    public String createPolicy(@ModelAttribute BusinessPolicyDto businessPolicyDto, RedirectAttributes ra) {
        try {
            businessPolicyService.createPolicyFromBusinessRule(businessPolicyDto);
            ra.addFlashAttribute("message", "새로운 비즈니스 정책이 성공적으로 생성되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "정책 생성 실패: " + e.getMessage());
        }
        return "redirect:/admin/policies"; // 생성 후에는 기술적 정책 목록으로 이동
    }

    // 수정(Update), 목록(List) 등의 핸들러는 유사한 패턴으로 추가...
}