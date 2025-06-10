package io.spring.identityadmin.admin.controller;

import io.spring.identityadmin.admin.service.PolicyService;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.entity.policy.Policy;
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
    private final ModelMapper modelMapper;

    @GetMapping
    public String listPolicies(Model model) {
        List<Policy> policies = policyService.getAllPolicies();
        List<PolicyDto> dtoList = policies.stream()
                .map(p -> modelMapper.map(p, PolicyDto.class))
                .collect(Collectors.toList());
        model.addAttribute("policies", dtoList);
        return "admin/policies";
    }

    @GetMapping("/register")
    public String registerForm(Model model, PolicyDto policyDto) {
        policyDto.getTargets().add(new PolicyDto.TargetDto());
        policyDto.getRules().add(new PolicyDto.RuleDto());
        model.addAttribute("policy", policyDto);
        return "admin/policydetails";
    }

    @PostMapping
    public String createPolicy(@ModelAttribute PolicyDto policyDto, RedirectAttributes ra) {
        policyService.createPolicy(policyDto);
        ra.addFlashAttribute("message", "정책이 성공적으로 생성되었습니다.");
        return "redirect:/admin/policies";
    }

    @GetMapping("/{id}")
    public String detailForm(@PathVariable Long id, Model model) {
        Policy policy = policyService.findById(id);
        PolicyDto dto = modelMapper.map(policy, PolicyDto.class);
        if (dto.getRules().isEmpty()) {
            dto.getRules().add(new PolicyDto.RuleDto());
        }
        model.addAttribute("policy", dto);
        return "admin/policydetails";
    }

    @PostMapping("/{id}/edit")
    public String updatePolicy(@PathVariable Long id, @ModelAttribute PolicyDto policyDto, RedirectAttributes ra) {
        policyDto.setId(id);
        policyService.updatePolicy(policyDto);
        ra.addFlashAttribute("message", "정책이 성공적으로 수정되었습니다.");
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
}
