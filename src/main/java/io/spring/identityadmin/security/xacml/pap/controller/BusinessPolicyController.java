package io.spring.identityadmin.security.xacml.pap.controller;

import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.security.xacml.pap.service.BusinessPolicyService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/authoring")
@RequiredArgsConstructor
public class BusinessPolicyController {

    private final BusinessPolicyService businessPolicyService;
    private final ModelMapper modelMapper;

    @GetMapping("/policy-workbench")
    public String getPolicyAuthoringWorkbench(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long resourceId,
            Model model) {

        // 워크벤치에서 전달된 파라미터를 모델에 담아 UI로 전달
        model.addAttribute("initialUserId", userId);
        model.addAttribute("initialGroupId", groupId);
        model.addAttribute("initialResourceId", resourceId);

        return "admin/policy-authoring-workbench";
    }

    @PostMapping("/policy-workbench")
    public String createBusinessPolicy(@ModelAttribute BusinessPolicyDto businessPolicyDto, RedirectAttributes ra) {
        try {
            Policy newPolicy = businessPolicyService.createPolicyFromBusinessRule(businessPolicyDto);
            ra.addFlashAttribute("message", "비즈니스 정책 '" + newPolicy.getName() + "' 이(가) 성공적으로 생성되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "정책 생성 중 오류 발생: " + e.getMessage());
        }
        return "redirect:/admin/policies"; // 생성 후 기술 정책 목록으로 이동
    }

    // API 요청 처리를 위한 RestController
    @RestController
    @RequestMapping("/api/admin/authoring")
    @RequiredArgsConstructor
    static class BusinessPolicyApiController {
        private final BusinessPolicyService businessPolicyService;
        private final ModelMapper modelMapper;

        @PostMapping("/policies")
        public ResponseEntity<PolicyDto> createPolicy(@RequestBody BusinessPolicyDto businessPolicyDto) {
            Policy policy = businessPolicyService.createPolicyFromBusinessRule(businessPolicyDto);
            return ResponseEntity.ok(modelMapper.map(policy, PolicyDto.class));
        }

        @PutMapping("/policies/{id}")
        public ResponseEntity<PolicyDto> updatePolicy(@PathVariable Long id, @RequestBody BusinessPolicyDto businessPolicyDto) {
            Policy policy = businessPolicyService.updatePolicyFromBusinessRule(id, businessPolicyDto);
            return ResponseEntity.ok(modelMapper.map(policy, PolicyDto.class));
        }
    }
}
