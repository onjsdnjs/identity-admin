package io.spring.identityadmin.admin.iam.controller;

import io.spring.identityadmin.admin.iam.service.PermissionService;
import io.spring.identityadmin.admin.metadata.service.FunctionCatalogService;
import io.spring.identityadmin.domain.dto.PermissionDto;
import io.spring.identityadmin.domain.entity.FunctionCatalog;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.repository.FunctionCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
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
    private final FunctionCatalogService functionCatalogService;

    /**
     * 권한 목록 페이지를 반환합니다.
     * @param model Model 객체
     * @return admin/permissions.html 템플릿 경로
     */
    @GetMapping
    public String getPermissions(Model model) {
        List<Permission> permissions = permissionService.getAllPermissions();
        // [개선] DTO 변환 시 연결된 리소스 정보도 매핑되도록 처리
        List<PermissionDto> dtoList = permissions.stream()
                .map(this::convertToDto)
                .toList();
        model.addAttribute("permissions", dtoList);
        return "admin/permissions";
    }

    @GetMapping("/register")
    public String registerPermissionForm(Model model) {
        model.addAttribute("permission", new PermissionDto());
        return "admin/permissiondetails";
    }

    @PostMapping
    public String createPermission(@ModelAttribute("permission") PermissionDto permissionDto, RedirectAttributes ra) {
        Permission permission = modelMapper.map(permissionDto, Permission.class);
        permissionService.createPermission(permission);
        ra.addFlashAttribute("message", "권한 '" + permission.getName() + "'이 성공적으로 생성되었습니다.");
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

        // [오류 수정] getFunctions() 호출 제거 및 DTO 변환 로직 사용
        PermissionDto permissionDto = convertToDto(permission);

        model.addAttribute("permission", permissionDto);
        return "admin/permissiondetails";
    }

    @PostMapping("/{id}/edit")
    public String updatePermission(@PathVariable Long id, @ModelAttribute("permission") PermissionDto permissionDto,
                                   RedirectAttributes ra) {
        // [오류 수정] functionIds 파라미터 제거
        Permission permission = permissionService.updatePermission(id, permissionDto);
        ra.addFlashAttribute("message", "권한 '" + permission.getName() + "'이 성공적으로 업데이트되었습니다.");
        return "redirect:/admin/permissions";
    }

    private void addCommonAttributesToModel(Model model) {
        List<FunctionCatalog> allActiveFunctions = functionCatalogService.findAllActiveFunctions();
        model.addAttribute("allFunctions", allActiveFunctions);
    }

    @GetMapping("/delete/{id}")
    public String deletePermission(@PathVariable Long id, RedirectAttributes ra) {
        try {
            permissionService.deletePermission(id);
            ra.addFlashAttribute("message", "권한 (ID: " + id + ")이 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "권한 삭제 중 오류 발생: " + e.getMessage());
        }
        return "redirect:/admin/permissions";
    }

    /**
     * Entity to DTO 변환을 위한 헬퍼 메서드.
     * ManagedResource 정보를 DTO에 매핑합니다.
     */
    private PermissionDto convertToDto(Permission permission) {
        PermissionDto dto = modelMapper.map(permission, PermissionDto.class);
        ManagedResource resource = permission.getManagedResource();
        if (resource != null) {
            dto.setManagedResourceId(resource.getId());
            dto.setManagedResourceIdentifier(resource.getResourceIdentifier());
        }
        return dto;
    }
}
