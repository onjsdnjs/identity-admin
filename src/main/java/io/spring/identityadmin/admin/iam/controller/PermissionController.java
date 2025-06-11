package io.spring.identityadmin.admin.iam.controller;

import io.spring.identityadmin.admin.iam.service.PermissionService;
import io.spring.identityadmin.domain.dto.PermissionDto;
import io.spring.identityadmin.domain.entity.FunctionCatalog;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.repository.FunctionCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/permissions") // 권한 관리를 위한 공통 경로 설정
@RequiredArgsConstructor
@Slf4j
public class PermissionController {

    private final PermissionService permissionService;
    private final ModelMapper modelMapper;
    private final FunctionCatalogRepository functionCatalogRepository;

    /**
     * 권한 목록 페이지를 반환합니다.
     * @param model Model 객체
     * @return admin/permissions.html 템플릿 경로
     */
    @GetMapping
    public String getPermissions(Model model) {
        List<Permission> permissions = permissionService.getAllPermissions();
        List<PermissionDto> dtoList = permissions.stream()
                .map(p -> modelMapper.map(p, PermissionDto.class))
                .toList();
        model.addAttribute("permissions", dtoList);
        return "admin/permissions";
    }

    /**
     * 새 권한 등록 폼 페이지를 반환합니다.
     * @param model Model 객체
     * @return admin/permissiondetails.html 템플릿 경로
     */
    @GetMapping("/register")
    public String registerPermissionForm(Model model) {
        model.addAttribute("permission", new PermissionDto()); // 빈 DTO 객체 전달
        log.info("Displaying new permission registration form.");
        return "admin/permissiondetails";
    }

    /**
     * 새 권한을 생성하는 POST 요청을 처리합니다.
     * @param permissionDto 폼에서 전송된 Permission 데이터
     * @param ra RedirectAttributes for flash messages
     * @return 리다이렉트 경로
     */
    @PostMapping
    public String createPermission(@ModelAttribute("permission") PermissionDto permissionDto, RedirectAttributes ra) {

        Permission permission = modelMapper.map(permissionDto, Permission.class);
        permissionService.createPermission(permission);
        ra.addFlashAttribute("message", "권한 '" + permission.getName() + "'이 성공적으로 생성되었습니다.");
        log.info("Permission created: {}", permission.getName());

        return "redirect:/admin/permissions";
    }

    /**
     * 특정 권한의 상세 정보 및 수정 폼 페이지를 반환합니다.
     * @param id 조회할 권한 ID
     * @param model Model 객체
     * @return admin/permissiondetails.html 템플릿 경로
     */
    @GetMapping("/{id}")
    public String permissionDetails(@PathVariable Long id, Model model) {
        Permission permission = permissionService.getPermission(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid permission ID: " + id));

        // 이 권한에 현재 매핑된 기능 ID 목록
        Set<Long> selectedFunctionIds = permission.getFunctions().stream()
                .map(FunctionCatalog::getId)
                .collect(Collectors.toSet());

        // 선택 가능한 전체 기능 목록 (Active 상태인 것만)
        List<FunctionCatalog> allActiveFunctions = functionCatalogRepository.findAll().stream()
                .filter(fc -> fc.getStatus() == FunctionCatalog.CatalogStatus.ACTIVE)
                .toList();

        model.addAttribute("permission", modelMapper.map(permission, PermissionDto.class));
        model.addAttribute("allFunctions", allActiveFunctions);
        model.addAttribute("selectedFunctionIds", selectedFunctionIds);

        return "admin/permissiondetails";
    }

    /**
     * 특정 권한을 업데이트하는 POST 요청을 처리합니다.
     * @param id 업데이트할 권한 ID
     * @param permissionDto 폼에서 전송된 Permission 데이터
     * @param ra RedirectAttributes for flash messages
     * @return 리다이렉트 경로
     */
    @PostMapping("/{id}/edit")
    public String updatePermission(@PathVariable Long id, @ModelAttribute("permission") PermissionDto permissionDto,
                                   @RequestParam(value="functionIds", required = false) Set<Long> functionIds,
                                   RedirectAttributes ra) {

        Permission permission = permissionService.updatePermission(id, permissionDto, functionIds);
        ra.addFlashAttribute("message", "권한 '" + permission.getName() + "'이 성공적으로 업데이트되었습니다.");
        return "redirect:/admin/permissions";
    }

    /**
     * 특정 권한을 삭제하는 GET 요청을 처리합니다.
     * @param id 삭제할 권한 ID
     * @param ra RedirectAttributes for flash messages
     * @return 리다이렉트 경로
     */
    @GetMapping("/delete/{id}")
    public String deletePermission(@PathVariable Long id, RedirectAttributes ra) {
        try {
            permissionService.deletePermission(id);
            ra.addFlashAttribute("message", "권한 (ID: " + id + ")이 성공적으로 삭제되었습니다.");
            log.info("Permission deleted: ID {}", id);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "권한 삭제 중 오류 발생: " + e.getMessage());
            log.error("Error deleting permission ID: {}", id, e);
        }
        return "redirect:/admin/permissions";
    }
}
