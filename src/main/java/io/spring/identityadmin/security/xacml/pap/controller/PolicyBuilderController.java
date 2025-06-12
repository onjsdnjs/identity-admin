package io.spring.identityadmin.security.xacml.pap.controller;

import io.spring.identityadmin.admin.iam.service.GroupService;
import io.spring.identityadmin.admin.iam.service.UserManagementService;
import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.repository.ConditionTemplateRepository;
import io.spring.identityadmin.security.xacml.pap.dto.VisualPolicyDto;
import io.spring.identityadmin.security.xacml.pap.service.PolicyBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/policy-builder")
@RequiredArgsConstructor
public class PolicyBuilderController {

    private final PolicyBuilderService policyBuilderService;
    private final UserManagementService userManagementService;
    private final GroupService groupService;
    private final PermissionCatalogService permissionCatalogService;
    private final ConditionTemplateRepository conditionTemplateRepository;

    /**
     * 시각적 정책 빌더 UI 페이지를 렌더링합니다.
     * 빌더의 팔레트를 채우기 위해 필요한 모든 구성요소(주체, 권한, 조건)를 모델에 담아 전달합니다.
     */
    @GetMapping
    public String policyBuilder(Model model) {
        model.addAttribute("allUsers", userManagementService.getUsers());
        model.addAttribute("allGroups", groupService.getAllGroups());
        model.addAttribute("allPermissions", permissionCatalogService.getAvailablePermissions());
        model.addAttribute("allConditions", conditionTemplateRepository.findAll());
        model.addAttribute("activePage", "policy-builder");
        return "admin/policy-builder";
    }

    /**
     * UI에서 생성된 시각적 정책 데이터를 받아 실제 정책을 생성하는 API 엔드포인트입니다.
     */
    @PostMapping("/api/build")
    public ResponseEntity<Policy> buildPolicy(@RequestBody VisualPolicyDto visualPolicyDto) {
        Policy createdPolicy = policyBuilderService.buildPolicyFromVisualComponents(visualPolicyDto);
        return ResponseEntity.ok(createdPolicy);
    }
}