package io.spring.identityadmin.admin.iam.controller;

import io.spring.identityadmin.admin.iam.service.GroupService;
import io.spring.identityadmin.admin.iam.service.RoleService;
import io.spring.identityadmin.admin.iam.service.impl.RoleHierarchyService;
import io.spring.identityadmin.domain.dto.*;
import io.spring.identityadmin.domain.entity.Group;
import io.spring.identityadmin.domain.entity.GroupRole;
import io.spring.identityadmin.domain.entity.RoleHierarchyEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/role-hierarchies")
@RequiredArgsConstructor
@Slf4j
public class RoleHierarchyController {

    private final RoleHierarchyService roleHierarchyService;
    private final ModelMapper modelMapper;
    private final RoleService roleService;
    private final GroupService groupService; // 추가: 그룹 정보 조회용

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
        prepareHierarchyFormModel(model, new ArrayList<>());
        return "admin/role-hierarchy-details";
    }

    @PostMapping
    public String createRoleHierarchy(@ModelAttribute("hierarchy") RoleHierarchyDto hierarchyDto, RedirectAttributes ra) {
        try {
            RoleHierarchyEntity entity = modelMapper.map(hierarchyDto, RoleHierarchyEntity.class);
            roleHierarchyService.createRoleHierarchy(entity);
            ra.addFlashAttribute("message", "역할 계층이 성공적으로 생성되었습니다!");
            log.info("Role hierarchy created: {}", entity.getHierarchyString());
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/role-hierarchies/register";
        }
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
        prepareHierarchyFormModel(model, pairs);

        return "admin/role-hierarchy-details";
    }

    @PostMapping("/{id}/edit")
    public String updateRoleHierarchy(@PathVariable Long id, @ModelAttribute("hierarchy") RoleHierarchyDto hierarchyDto, RedirectAttributes ra) {
        try {
            hierarchyDto.setId(id);
            RoleHierarchyEntity entity = modelMapper.map(hierarchyDto, RoleHierarchyEntity.class);
            roleHierarchyService.updateRoleHierarchy(entity);
            ra.addFlashAttribute("message", "역할 계층이 성공적으로 업데이트되었습니다!");
            log.info("Role hierarchy updated: {}", entity.getHierarchyString());
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/role-hierarchies/" + id;
        }
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

    /**
     * 역할 계층 폼에 필요한 모델 데이터를 준비합니다.
     * - 모든 역할을 그룹별로 구조화
     * - 각 역할의 권한 정보 포함
     */
    private void prepareHierarchyFormModel(Model model, List<RoleHierarchyDto.HierarchyPair> existingPairs) {
        // 모든 그룹과 역할 정보를 조회
        List<Group> allGroups = groupService.getAllGroups();

        // 그룹별 역할 정보를 구조화
        List<GroupWithRolesDto> groupsWithRoles = allGroups.stream()
                .map(group -> {
                    GroupWithRolesDto gwrDto = new GroupWithRolesDto();
                    gwrDto.setGroupId(group.getId());
                    gwrDto.setGroupName(group.getName());
                    gwrDto.setGroupDescription(group.getDescription());

                    List<RoleDetailDto> roleDetails = group.getGroupRoles().stream()
                            .map(GroupRole::getRole)
                            .filter(role -> !"Y".equals(role.getIsExpression())) // 표현식 역할 제외
                            .map(role -> {
                                RoleDetailDto rdDto = new RoleDetailDto();
                                rdDto.setRoleId(role.getId());
                                rdDto.setRoleName(role.getRoleName());
                                rdDto.setRoleDesc(role.getRoleDesc());

                                // 권한 정보 매핑
                                List<String> permissions = role.getRolePermissions().stream()
                                        .map(rp -> rp.getPermission().getFriendlyName())
                                        .sorted()
                                        .collect(Collectors.toList());
                                rdDto.setPermissions(permissions);

                                return rdDto;
                            })
                            .collect(Collectors.toList());

                    gwrDto.setRoles(roleDetails);
                    return gwrDto;
                })
                .filter(gwrDto -> !gwrDto.getRoles().isEmpty()) // 역할이 없는 그룹 제외
                .collect(Collectors.toList());

        // 그룹에 속하지 않은 역할들 (혹시 있다면)
        List<RoleMetadataDto> ungroupedRoles = roleService.getRolesWithoutExpression().stream()
                .filter(role -> allGroups.stream()
                        .noneMatch(group -> group.getGroupRoles().stream()
                                .anyMatch(gr -> gr.getRole().getId().equals(role.getId()))))
                .map(role -> modelMapper.map(role, RoleMetadataDto.class))
                .collect(Collectors.toList());

        model.addAttribute("groupsWithRoles", groupsWithRoles);
        model.addAttribute("ungroupedRoles", ungroupedRoles);
        model.addAttribute("hierarchyPairs", existingPairs);

        // 모든 역할의 간단한 목록 (호환성 유지)
        List<RoleMetadataDto> allRoles = roleService.getRolesWithoutExpression().stream()
                .map(role -> modelMapper.map(role, RoleMetadataDto.class))
                .collect(Collectors.toList());
        model.addAttribute("allRoles", allRoles);
    }
}

// 새로운 DTO 클래스들 (domain.dto 패키지에 추가)
class GroupWithRolesDto {
    private Long groupId;
    private String groupName;
    private String groupDescription;
    private List<RoleDetailDto> roles;

    // getter/setter
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getGroupDescription() { return groupDescription; }
    public void setGroupDescription(String groupDescription) { this.groupDescription = groupDescription; }
    public List<RoleDetailDto> getRoles() { return roles; }
    public void setRoles(List<RoleDetailDto> roles) { this.roles = roles; }
}

class RoleDetailDto {
    private Long roleId;
    private String roleName;
    private String roleDesc;
    private List<String> permissions;

    // getter/setter
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleDesc() { return roleDesc; }
    public void setRoleDesc(String roleDesc) { this.roleDesc = roleDesc; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}