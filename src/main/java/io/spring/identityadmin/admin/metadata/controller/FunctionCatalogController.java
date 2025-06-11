package io.spring.identityadmin.admin.metadata.controller;

import io.spring.identityadmin.admin.metadata.service.FunctionCatalogService;
import io.spring.identityadmin.domain.dto.FunctionCatalogDto;
import io.spring.identityadmin.domain.dto.FunctionCatalogUpdateDto;
import io.spring.identityadmin.resource.ResourceEnhancementService;
import io.spring.identityadmin.resource.ResourceRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/admin/catalog") // 새로운 URL 경로
@RequiredArgsConstructor
public class FunctionCatalogController {

    private final ResourceRegistryService resourceRegistryService;
    private final ResourceEnhancementService resourceEnhancementService;
    private final FunctionCatalogService functionCatalogService;

    @GetMapping("/unconfirmed")
    public String unconfirmedListPage(Model model) {
        model.addAttribute("unconfirmedFunctions", functionCatalogService.findUnconfirmedFunctions());
        model.addAttribute("functionGroups", functionCatalogService.getAllFunctionGroups());
        return "admin/catalog-unconfirmed";
    }

    @PostMapping("/{catalogId}/confirm")
    public String confirmFunction(@PathVariable Long catalogId, @RequestParam Long groupId, RedirectAttributes ra) {
        functionCatalogService.confirmFunction(catalogId, groupId);
        ra.addFlashAttribute("message", "기능이 성공적으로 확인 및 등록되었습니다.");
        return "redirect:/admin/catalog/unconfirmed";
    }

    @GetMapping
    public String catalogListPage(Model model) {
        List<FunctionCatalogDto> catalogs = functionCatalogService.getManageableCatalogs();
        model.addAttribute("catalogs", catalogs);
        return "admin/catalog";
    }

    // 2. 개별 기능(리소스) 정보 업데이트를 처리합니다. (기존 로직과 동일하지만 새로운 서비스 호출)
    @PostMapping("/{id}/update")
    public String updateCatalogItem(@PathVariable Long id, @ModelAttribute FunctionCatalogUpdateDto dto, RedirectAttributes ra) {
        functionCatalogService.updateCatalog(id, dto);
        ra.addFlashAttribute("message", "기능(ID: " + id + ") 정보가 성공적으로 업데이트되었습니다.");
        return "redirect:/admin/catalog";
    }

    // 3. 리소스 스캔을 수동으로 트리거합니다.
    @PostMapping("/refresh")
    public String refreshResources(RedirectAttributes ra) {
        resourceEnhancementService.refreshResources();
        ra.addFlashAttribute("message", "시스템의 모든 기능을 성공적으로 다시 스캔했습니다.");
        return "redirect:/admin/catalog";
    }

    // 4. '워크벤치 표시' 일괄 업데이트 API
    @PostMapping("/batch-status")
    public ResponseEntity<?> batchUpdateStatus(@RequestBody Map<String, Object> payload) {
        try {
            List<Integer> idsAsInteger = (List<Integer>) payload.get("ids");
            List<Long> ids = idsAsInteger.stream().map(Integer::longValue).toList();
            String status = (String) payload.get("status");
            functionCatalogService.batchUpdateStatus(ids, status);
            return ResponseEntity.ok(Map.of("message", "선택된 기능들의 상태가 성공적으로 변경되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
