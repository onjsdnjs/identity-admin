package io.spring.identityadmin.admin.metadata.controller;

import io.spring.identityadmin.domain.dto.ResourceManagementDto;
import io.spring.identityadmin.domain.dto.ResourceMetadataDto;
import io.spring.identityadmin.domain.dto.ResourceSearchCriteria;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.resource.ResourceRegistryService;
import io.spring.identityadmin.studio.dto.InitiateGrantRequestDto;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import io.spring.identityadmin.workflow.wizard.service.PermissionWizardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/admin/workbench/resources")
@RequiredArgsConstructor
@Slf4j
public class ResourceAdminController {

    private final ResourceRegistryService resourceRegistryService;
    private final PermissionWizardService permissionWizardService;

    @GetMapping
    public String resourceWorkbenchPage(
            @ModelAttribute("criteria") ResourceSearchCriteria criteria,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        Page<ManagedResource> resourcePage = resourceRegistryService.findResources(criteria, pageable);
        Set<String> serviceOwners = resourceRegistryService.getAllServiceOwners();

        model.addAttribute("resourcePage", resourcePage);
        model.addAttribute("serviceOwners", serviceOwners);
        model.addAttribute("criteria", criteria);
        return "admin/resource-workbench";
    }

    @PostMapping("/refresh")
    public String refreshResources(RedirectAttributes ra) {
        try {
            resourceRegistryService.refreshAndSynchronizeResources();
            ra.addFlashAttribute("message", "시스템 리소스를 성공적으로 새로고침했습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "리소스 새로고침 중 오류 발생: " + e.getMessage());
        }
        return "redirect:/admin/workbench/resources";
    }

    @PostMapping("/{id}/define")
    public String defineResource(@PathVariable Long id, @ModelAttribute ResourceMetadataDto metadataDto, RedirectAttributes ra) {
        try {
            resourceRegistryService.defineResourceAsPermission(id, metadataDto);
            ra.addFlashAttribute("message", "리소스 (ID: " + id + ")가 성공적으로 권한으로 정의되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "권한 정의 중 오류 발생: " + e.getMessage());
        }
        return "redirect:/admin/workbench/resources";
    }

    @PostMapping("/{id}/define-and-grant")
    public String defineAndGrantPermission(@PathVariable Long id, @ModelAttribute ResourceMetadataDto metadataDto, RedirectAttributes ra) {
        Permission newPermission = resourceRegistryService.defineResourceAsPermission(id, metadataDto);
        log.info("Resource defined as permission '{}'. Initiating grant wizard.", newPermission.getName());

        InitiateGrantRequestDto grantRequest = new InitiateGrantRequestDto();
        grantRequest.setPermissionIds(Set.of(newPermission.getId()));

        WizardContext createdContext = permissionWizardService.beginCreation(grantRequest,
                "신규 권한 할당: " + newPermission.getFriendlyName(),
                "리소스 워크벤치에서 생성된 신규 권한을 역할에 할당합니다.");

        // [수정] RedirectAttributes에 컨텍스트 객체를 flash attribute로 추가
        ra.addFlashAttribute("wizardContext", createdContext);
        ra.addFlashAttribute("fromWorkbench", true); // 워크벤치에서 왔다는 플래그 추가

        // [수정] 리다이렉트 URL 수정
        return "redirect:/admin/policy-wizard/" + createdContext.contextId();
    }

    @PostMapping("/{id}/exclude")
    public String excludeResource(@PathVariable Long id, RedirectAttributes ra) {
        try {
            resourceRegistryService.excludeResourceFromManagement(id);
            ra.addFlashAttribute("message", "리소스가 '관리 제외' 처리되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "처리 중 오류 발생: " + e.getMessage());
        }
        return "redirect:/admin/workbench/resources";
    }

    @PostMapping("/{id}/manage")
    public String updateManagementStatus(@PathVariable Long id, @ModelAttribute ResourceManagementDto managementDto, RedirectAttributes ra) {
        try {
            resourceRegistryService.updateResourceManagementStatus(id, managementDto);
            ra.addFlashAttribute("message", "리소스 (ID: " + id + ")의 관리 상태가 변경되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "관리 상태 변경 중 오류 발생: " + e.getMessage());
        }
        return "redirect:/admin/workbench/resources";
    }
}