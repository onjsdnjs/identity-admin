package io.spring.identityadmin.admin.metadata.controller;

import io.spring.identityadmin.domain.dto.ResourceMetadataDto;
import io.spring.identityadmin.resource.ResourceRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/resources")
@RequiredArgsConstructor
public class ResourceAdminController {

    private final ResourceRegistryService resourceRegistryService;

    @GetMapping
    public String resourceListPage(Model model) {
        model.addAttribute("resources", resourceRegistryService.findAllForAdmin());
        return "admin/resources";
    }

    @PostMapping("/{id}/update")
    public String updateResource(@PathVariable Long id, @ModelAttribute ResourceMetadataDto metadataDto, RedirectAttributes ra) {
        try {
            resourceRegistryService.updateResource(id, metadataDto);
            ra.addFlashAttribute("message", "리소스 (ID: " + id + ") 정보가 성공적으로 업데이트되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "업데이트 중 오류 발생: " + e.getMessage());
        }
        return "redirect:/admin/resources";
    }
}