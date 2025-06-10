package io.spring.identityadmin.admin.controller; // 패키지명 변경: io.springsecurity.springsecurity6x.controller 로 변경 권장

import io.spring.identityadmin.admin.service.GroupService;
import io.spring.identityadmin.admin.service.RoleService;
import io.spring.identityadmin.admin.service.UserManagementService;
import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.domain.dto.UserListDto;
import io.spring.identityadmin.entity.Group;
import io.spring.identityadmin.entity.Role;
import io.spring.identityadmin.entity.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

	private final UserManagementService userManagementService;
	private final RoleService roleService;
	private final GroupService groupService;

	@GetMapping
	public String getUsers(Model model) {
		List<UserListDto> users = userManagementService.getUsers();
		model.addAttribute("users", users);
		return "admin/users";
	}

	@PostMapping
	public String modifyUser(@ModelAttribute("user") UserDto userDto, RedirectAttributes ra) {
		userManagementService.modifyUser(userDto);
		ra.addFlashAttribute("message", "사용자 '" + userDto.getUsername() + "' 정보가 성공적으로 수정되었습니다!");
		log.info("User {} modified.", userDto.getUsername());
		return "redirect:/admin/users";
	}

	@GetMapping("/{id}")
	public String getUser(@PathVariable Long id, Model model) {
		UserDto userDto = userManagementService.getUser(id);
		List<Role> roleList = roleService.getRolesWithoutExpression();
		List<Group> groupList = groupService.getAllGroups();

		List<Long> selectedGroupIds = userDto.getSelectedGroupIds();
		if (selectedGroupIds == null) {
			selectedGroupIds = List.of();
		}

		model.addAttribute("user", userDto);
		model.addAttribute("roleList", roleList);
		model.addAttribute("groupList", groupList);
		model.addAttribute("selectedGroupIds", selectedGroupIds);

		return "admin/userdetails";
	}

	@GetMapping("/delete/{id}")
	public String removeUser(@PathVariable Long id, RedirectAttributes ra) {
		userManagementService.deleteUser(id);
		ra.addFlashAttribute("message", "사용자 (ID: " + id + ")가 성공적으로 삭제되었습니다!");
		log.info("User ID {} deleted.", id);
		return "redirect:/admin/users";
	}
}