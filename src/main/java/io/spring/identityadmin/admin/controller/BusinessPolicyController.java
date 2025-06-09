package io.spring.identityadmin.admin.controller;

import io.spring.identityadmin.admin.service.BusinessMetadataService;
import io.spring.identityadmin.admin.service.BusinessPolicyService;
import io.spring.identityadmin.admin.service.PolicyService;
import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import io.spring.identityadmin.entity.policy.Policy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/business-policies")
@RequiredArgsConstructor
public class BusinessPolicyController {

    private final BusinessPolicyService businessPolicyService;
    private final BusinessMetadataService businessMetadataService;
    private final PolicyService policyService;

    /**
     * [신규] 비즈니스 정책의 결과물인 기술 정책 목록을 보여주는 핸들러.
     * @param model
     * @return
     */
    @GetMapping
    public String listBusinessPolicies(Model model) {
        List<Policy> policies = policyService.getAllPolicies();
        model.addAttribute("policies", policies);
        // 비즈니스 정책 목록을 보여주는 전용 템플릿을 렌더링
        return "admin/business-policies";
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

    private void populateModelWithMetadata(Model model) {
        model.addAttribute("users", businessMetadataService.getAllUsersForPolicy());
        model.addAttribute("groups", businessMetadataService.getAllGroupsForPolicy());
        model.addAttribute("resources", businessMetadataService.getAllBusinessResources());
        model.addAttribute("actions", businessMetadataService.getAllBusinessActions());
        model.addAttribute("conditions", businessMetadataService.getAllConditionTemplates());
    }

    // 수정 및 삭제 기능은 향후 확장 과제
}