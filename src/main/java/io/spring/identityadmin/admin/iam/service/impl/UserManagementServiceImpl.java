package io.spring.identityadmin.admin.iam.service.impl;

import io.spring.identityadmin.admin.iam.service.UserManagementService;
import io.spring.identityadmin.domain.entity.*;
import io.spring.identityadmin.repository.GroupRepository;
import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.domain.dto.UserListDto;
import io.spring.identityadmin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("userManagementService")
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    @CacheEvict(value = "usersWithAuthorities", allEntries = true)
    public void modifyUser(@ModelAttribute UserDto userDto){
        Users users = userRepository.findByIdWithGroupsRolesAndPermissions(userDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userDto.getId()));

        users.setName(userDto.getName());
        users.setMfaEnabled(userDto.isMfaEnabled());

        if (StringUtils.hasText(userDto.getPassword())) {
            users.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        // ========================= [오류 수정된 동기화 로직] =========================
        Set<Long> desiredGroupIds = userDto.getSelectedGroupIds() != null ? new HashSet<>(userDto.getSelectedGroupIds()) : new HashSet<>();
        Set<UserGroup> currentUserGroups = users.getUserGroups();

        // 1. 제거할 그룹 처리: 현재 그룹 목록에는 있지만, 원하는 그룹 목록에는 없는 UserGroup 관계를 제거
        currentUserGroups.removeIf(userGroup -> !desiredGroupIds.contains(userGroup.getGroup().getId()));

        // 2. 추가할 그룹 처리: 원하는 그룹 목록에는 있지만, 현재 그룹 목록에는 없는 UserGroup 관계를 추가
        Set<Long> currentGroupIds = currentUserGroups.stream()
                .map(ug -> ug.getGroup().getId())
                .collect(Collectors.toSet());

        desiredGroupIds.stream()
                .filter(desiredId -> !currentGroupIds.contains(desiredId))
                .forEach(newGroupId -> {
                    Group group = groupRepository.findById(newGroupId)
                            .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + newGroupId));
                    currentUserGroups.add(UserGroup.builder().user(users).group(group).build());
                });
        // ====================================================================

        log.info("User {} (ID: {}) modified successfully.", users.getUsername(), users.getId());
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        Users users = userRepository.findByIdWithGroupsRolesAndPermissions(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
        UserDto userDto = modelMapper.map(users, UserDto.class);
        List<String> roles = users.getRoleNames();
        List<String> permissions = users.getPermissionNames();

        userDto.setRoles(roles);
        userDto.setPermissions(permissions);
        if (users.getUserGroups() != null) {
            userDto.setSelectedGroupIds(users.getUserGroups().stream()
                    .map(ug -> ug.getGroup().getId())
                    .collect(Collectors.toList()));
        } else {
            userDto.setSelectedGroupIds(List.of());
        }

        log.debug("Fetched user {} with roles: {} and permissions: {}", users.getUsername(), roles, permissions);
        return userDto;
    }


    @Transactional(readOnly = true)
    public List<UserListDto> getUsers() {
        return userRepository.findAllWithDetails().stream()
                .map(user -> {
                    UserListDto dto = modelMapper.map(user, UserListDto.class);
                    dto.setGroupCount(user.getUserGroups() != null ? user.getUserGroups().size() : 0);
                    long roleCount = user.getRoleNames().stream().distinct().count();
                    dto.setRoleCount((int) roleCount);
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "usersWithAuthorities", allEntries = true)
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
        log.info("User ID {} deleted.", id);
    }
}