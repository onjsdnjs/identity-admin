package io.spring.identityadmin.admin.iam.controller; // 패키지명 변경: io.springsecurity.springsecurity6x.controller 로 변경 권장

import io.spring.identityadmin.admin.iam.service.GroupService;
import io.spring.identityadmin.admin.iam.service.RoleService;
import io.spring.identityadmin.admin.iam.service.UserManagementService;
import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.domain.dto.UserListDto;
import io.spring.identityadmin.domain.entity.Group;
import io.spring.identityadmin.domain.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

	private final UserManagementService userManagementService;
	private final RoleService roleService;
	private final GroupService groupService;

	@GetMapping
	@Operation(summary = "사용자 목록 조회", description = "사용자들의 목록을 조회 할 수 있습니다.")
	public String getUsers(Model model) {
		List<UserListDto> users = userManagementService.getUsers();
		model.addAttribute("users", users);
		return "admin/users";
	}

	@PostMapping
	@Operation(summary = "사용자 정보 수정", description = "사용자의 정보를 수정 할 수 있습니다.")
	public String modifyUser(@ModelAttribute("user") UserDto userDto, RedirectAttributes ra) {
		userManagementService.modifyUser(userDto);
		ra.addFlashAttribute("message", "사용자 '" + userDto.getUsername() + "' 정보가 성공적으로 수정되었습니다!");
		log.info("User {} modified.", userDto.getUsername());
		return "redirect:/admin/users";
	}

	@GetMapping("/{id}")
	@Operation(summary = "사용자 정보 조회", description = "사용자의 정보를 조회 할 수 있습니다.")
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
	@Operation(summary = "사용자 정보 삭제", description = "사용자의 정보을 삭제 할 수 있습니다.")
	public String removeUser(@PathVariable Long id, RedirectAttributes ra) {
		userManagementService.deleteUser(id);
		ra.addFlashAttribute("message", "사용자 (ID: " + id + ")가 성공적으로 삭제되었습니다!");
		log.info("User ID {} deleted.", id);
		return "redirect:/admin/users";
	}
}