package io.spring.identityadmin.admin.iam.controller;

import io.spring.identityadmin.admin.iam.service.RoleService;
import io.spring.identityadmin.admin.iam.service.impl.RoleHierarchyService;
import io.spring.identityadmin.domain.dto.RoleHierarchyDto;
import io.spring.identityadmin.domain.dto.RoleMetadataDto;
import io.spring.identityadmin.domain.entity.RoleHierarchyEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/role-hierarchies")
@RequiredArgsConstructor
@Slf4j
public class RoleHierarchyController {

    private final RoleHierarchyService roleHierarchyService;
    private final ModelMapper modelMapper;
    private final RoleService roleService;

    @GetMapping
    public String getRoleHierarchies(Model model) {
        List<RoleHierarchyEntity> hierarchies = roleHierarchyService.getAllRoleHierarchies();
        List<RoleHierarchyDto> roleHierarchyList = hierarchies.stream().map(roleHierarchy -> {
            return modelMapper.map(roleHierarchy, RoleHierarchyDto.class);
        }).toList();
        model.addAttribute("hierarchies", roleHierarchyList);
        log.info("Displaying role hierarchies list. Total: {}", roleHierarchyList.size());
        return "admin/role-hierarchies";
    }

    @GetMapping("/register")
    public String registerRoleHierarchyForm(Model model) {
        model.addAttribute("hierarchy", new RoleHierarchyDto());
        List<RoleMetadataDto> allRoles = roleService.getRolesWithoutExpression().stream()
                .map(role -> modelMapper.map(role, RoleMetadataDto.class))
                .collect(Collectors.toList());

        model.addAttribute("allRoles", allRoles);
        model.addAttribute("hierarchyPairs", new ArrayList<>());
        return "admin/role-hierarchy-details";
    }

    @PostMapping
    public String createRoleHierarchy(@ModelAttribute("hierarchy") RoleHierarchyDto hierarchyDto, RedirectAttributes ra) {
        RoleHierarchyEntity entity = modelMapper.map(hierarchyDto, RoleHierarchyEntity.class);
        roleHierarchyService.createRoleHierarchy(entity);
        ra.addFlashAttribute("message", "역할 계층이 성공적으로 생성되었습니다!");
        log.info("Role hierarchy created: {}", entity.getHierarchyString());
        return "redirect:/admin/role-hierarchies";
    }

    @GetMapping("/{id}")
    public String roleHierarchyDetails(@PathVariable Long id, Model model) {
        RoleHierarchyEntity entity = roleHierarchyService.getRoleHierarchy(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid RoleHierarchy ID: " + id));

        RoleHierarchyDto dto = modelMapper.map(entity, RoleHierarchyDto.class);

        List<RoleHierarchyDto.HierarchyPair> pairs = Arrays.stream(entity.getHierarchyString().split("\\n"))
                .map(String::trim)
                .filter(s -> s.contains(">"))
                .map(s -> {
                    String[] parts = s.split(">");
                    return new RoleHierarchyDto.HierarchyPair(parts[0].trim(), parts[1].trim());
                })
                .collect(Collectors.toList());
        dto.setHierarchyPairs(pairs);

        model.addAttribute("hierarchy", dto);

        List<RoleMetadataDto> allRoles = roleService.getRolesWithoutExpression().stream()
                .map(role -> modelMapper.map(role, RoleMetadataDto.class))
                .collect(Collectors.toList());
        model.addAttribute("allRoles", allRoles);

        model.addAttribute("hierarchyPairs", pairs);
        return "admin/role-hierarchy-details";
    }

    @PostMapping("/{id}/edit")
    public String updateRoleHierarchy(@PathVariable Long id, @ModelAttribute("hierarchy") RoleHierarchyDto hierarchyDto, RedirectAttributes ra) {
        hierarchyDto.setId(id); // URL 경로에서 받은 ID를 DTO에 설정
        RoleHierarchyEntity entity = modelMapper.map(hierarchyDto, RoleHierarchyEntity.class);
        roleHierarchyService.updateRoleHierarchy(entity);
        ra.addFlashAttribute("message", "역할 계층이 성공적으로 업데이트되었습니다!");
        log.info("Role hierarchy updated: {}", entity.getHierarchyString());
        return "redirect:/admin/role-hierarchies";
    }

    @GetMapping("/delete/{id}")
    public String deleteRoleHierarchy(@PathVariable Long id, RedirectAttributes ra) {
        roleHierarchyService.deleteRoleHierarchy(id);
        ra.addFlashAttribute("message", "역할 계층 (ID: " + id + ")이 성공적으로 삭제되었습니다!");
        log.info("Role hierarchy deleted: ID {}", id);
        return "redirect:/admin/role-hierarchies";
    }

    @PostMapping("/{id}/activate")
    public String activateRoleHierarchy(@PathVariable Long id, RedirectAttributes ra) {
        roleHierarchyService.activateRoleHierarchy(id);
        ra.addFlashAttribute("message", "역할 계층 (ID: " + id + ")이 활성화되었습니다!");
        log.info("Role hierarchy activated: ID {}", id);
        return "redirect:/admin/role-hierarchies";
    }
}