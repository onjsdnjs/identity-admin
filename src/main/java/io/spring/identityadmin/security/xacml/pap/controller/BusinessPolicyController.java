package io.spring.identityadmin.security.xacml.pap.controller;

import io.spring.identityadmin.admin.metadata.service.BusinessMetadataService;
import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.security.xacml.pap.service.BusinessPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/admin/authoring")
@RequiredArgsConstructor
public class BusinessPolicyController {

    private final BusinessPolicyService businessPolicyService;
    private final BusinessMetadataService businessMetadataService;

    @GetMapping("/policy-workbench")
    public String getPolicyAuthoringWorkbench(Model model) {
        return "admin/policy-authoring-workbench";
    }

    @PostMapping("/policy-workbench")
    public String createBusinessPolicy(@ModelAttribute BusinessPolicyDto businessPolicyDto, RedirectAttributes ra) {
        Policy newPolicy = businessPolicyService.createPolicyFromBusinessRule(businessPolicyDto);
        ra.addFlashAttribute("message", "비즈니스 정책 '" + newPolicy.getName() + "' 이(가) 성공적으로 생성되었습니다.");
        return "redirect:/admin/policies";
    }

    @RestController
    @RequestMapping("/api/admin/authoring")
    @RequiredArgsConstructor
    static class BusinessPolicyApiController {
        private final BusinessMetadataService businessMetadataService;

        @GetMapping("/metadata")
        public ResponseEntity<Map<String, Object>> getPolicyAuthoringMetadata() {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("subjects", businessMetadataService.getAllUsersAndGroups());
            metadata.put("resources", businessMetadataService.getAllBusinessResources());
            metadata.put("actions", businessMetadataService.getAllBusinessActions());
            metadata.put("conditionTemplates", businessMetadataService.getAllConditionTemplates());
            return ResponseEntity.ok(metadata);
        }
    }
}