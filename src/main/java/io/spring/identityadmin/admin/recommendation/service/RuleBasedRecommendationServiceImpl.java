package io.spring.identityadmin.admin.recommendation.service;

import io.spring.identityadmin.admin.recommendation.dto.RecommendedResourceDto;
import io.spring.identityadmin.admin.recommendation.dto.SubjectContext;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.domain.entity.Users;
import io.spring.identityadmin.repository.PermissionRepository;
import io.spring.identityadmin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RuleBasedRecommendationServiceImpl implements PermissionRecommendationService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    /**
     * 1. 대상 사용자가 속한 그룹 ID를 가져옵니다.
     * 2. 해당 그룹에 속한 다른 모든 사용자를 조회합니다.
     * 3. 다른 사용자들의 모든 권한을 집계하여, 권한별 보유자 수를 계산합니다.
     * 4. 다른 사용자의 50% 이상이 공통으로 보유하고 있고, 대상 사용자는 아직 없는 권한을 추천합니다.
     */
    @Override
    public List<RecommendedResourceDto> recommendPermissionsForSubject(SubjectContext subjectContext) {
        if (!"USER".equalsIgnoreCase(subjectContext.subjectType()) || subjectContext.groupIds().isEmpty()) {
            return Collections.emptyList();
        }

        Users targetUser = userRepository.findByIdWithGroupsRolesAndPermissions(subjectContext.subjectId())
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));
        Set<String> targetUserPermissions = new HashSet<>(targetUser.getPermissionNames());

        List<Users> otherUsersInSameGroup = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(targetUser.getId()) && u.getUserGroups().stream()
                        .anyMatch(ug -> subjectContext.groupIds().contains(ug.getGroup().getId())))
                .toList();

        if (otherUsersInSameGroup.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Permission, Long> commonPermissionCounts = otherUsersInSameGroup.stream()
                .flatMap(u -> u.getPermissionNames().stream())
                .map(name -> permissionRepository.findByName(name).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        long threshold = Math.round(otherUsersInSameGroup.size() * 0.5);

        return commonPermissionCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .filter(perm -> !targetUserPermissions.contains(perm.getName()))
                .map(perm -> new RecommendedResourceDto(perm.getId(), perm.getDescription(),
                        "같은 그룹 멤버 " + otherUsersInSameGroup.size() + "명 중 " + commonPermissionCounts.get(perm) + "명이 보유한 권한입니다.",
                        (double) commonPermissionCounts.get(perm) / otherUsersInSameGroup.size()))
                .collect(Collectors.toList());
    }
}