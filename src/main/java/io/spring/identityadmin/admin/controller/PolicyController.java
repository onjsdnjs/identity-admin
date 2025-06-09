package io.spring.identityadmin.admin.controller;

import io.spring.identityadmin.admin.service.BusinessMetadataService;
import io.spring.identityadmin.admin.service.BusinessPolicyService;
import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.entity.policy.Policy;
import io.spring.identityadmin.admin.service.PolicyService;
import io.spring.identityadmin.entity.policy.PolicyCondition;
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
    private final BusinessPolicyService businessPolicyService;
    private final BusinessMetadataService businessMetadataService;

    @GetMapping
    public String listPolicies(Model model) {
        List<Policy> policies = policyService.getAllPolicies();
        model.addAttribute("policies", policies);
        return "admin/policies";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        PolicyDto policyDto = new PolicyDto();
        // [수정] 기본적으로 하나의 빈 Target과 Rule을 추가하여 UI 렌더링
        policyDto.getTargets().add(new PolicyDto.TargetDto());
        policyDto.getRules().add(new PolicyDto.RuleDto());
        model.addAttribute("policy", policyDto);
        return "admin/policydetails";
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
        PolicyDto dto = toDto(policy);

        // 규칙이 하나도 없는 경우, UI 렌더링을 위해 빈 규칙 DTO를 하나 추가
        if (dto.getRules().isEmpty()) {
            dto.getRules().add(new PolicyDto.RuleDto());
        }

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

    private PolicyDto toDto(Policy policy) {
        PolicyDto dto = new PolicyDto();
        dto.setId(policy.getId());
        dto.setName(policy.getName());
        dto.setDescription(policy.getDescription());
        dto.setEffect(policy.getEffect());
        dto.setPriority(policy.getPriority());

        dto.setTargets(policy.getTargets().stream().map(t ->
                new PolicyDto.TargetDto(t.getTargetType(), t.getTargetIdentifier(), t.getHttpMethod() == null ? "ALL" : t.getHttpMethod())
        ).collect(Collectors.toList()));

        dto.setRules(policy.getRules().stream().map(rule -> {
            PolicyDto.RuleDto ruleDto = new PolicyDto.RuleDto();
            ruleDto.setDescription(rule.getDescription());
            ruleDto.setConditions(rule.getConditions().stream()
                    .map(PolicyCondition::getExpression)
                    .collect(Collectors.toList()));
            return ruleDto;
        }).collect(Collectors.toList()));

        return dto;
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

    @GetMapping("/business")
    public String listBusinessPolicies(Model model) {
        List<Policy> policies = policyService.getAllPolicies();
        model.addAttribute("policies", policies);
        return "admin/business-policies"; // 새로운 목록 템플릿을 렌더링
    }

    @GetMapping("/business/create")
    public String createBusinessPolicyForm(Model model) {
        model.addAttribute("businessPolicy", new BusinessPolicyDto());
        populateModelWithMetadata(model);
        return "admin/business-policy-details"; // 간편 정책 생성 템플릿
    }

    @PostMapping("/business/create")
    public String createBusinessPolicy(@ModelAttribute("businessPolicy") BusinessPolicyDto businessPolicyDto, RedirectAttributes ra) {
        try {
            businessPolicyService.createPolicyFromBusinessRule(businessPolicyDto);
            ra.addFlashAttribute("message", "새로운 비즈니스 정책이 성공적으로 생성되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "정책 생성 실패: " + e.getMessage());
        }
        return "redirect:/admin/policies"; // 생성 후에는 결과 확인을 위해 기술 정책 목록으로 이동
    }

    private void populateModelWithMetadata(Model model) {
        model.addAttribute("users", businessMetadataService.getAllUsersForPolicy());
        model.addAttribute("groups", businessMetadataService.getAllGroupsForPolicy());
        model.addAttribute("resources", businessMetadataService.getAllBusinessResources());
        model.addAttribute("actions", businessMetadataService.getAllBusinessActions());
        model.addAttribute("conditions", businessMetadataService.getAllConditionTemplates());
    }
}
