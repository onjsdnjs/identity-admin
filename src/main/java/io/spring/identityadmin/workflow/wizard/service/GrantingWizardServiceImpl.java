package io.spring.identityadmin.workflow.wizard.service;

import io.spring.identityadmin.admin.iam.service.GroupService;
import io.spring.identityadmin.admin.iam.service.UserManagementService;
import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.domain.entity.Group;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.domain.entity.RolePermission;
import io.spring.identityadmin.domain.entity.Users;
import io.spring.identityadmin.repository.GroupRepository;
import io.spring.identityadmin.repository.RoleRepository;
import io.spring.identityadmin.repository.UserRepository;
import io.spring.identityadmin.studio.dto.EffectivePermissionDto;
import io.spring.identityadmin.studio.dto.SimulationResultDto;
import io.spring.identityadmin.studio.dto.WizardInitiationDto;
import io.spring.identityadmin.studio.service.StudioVisualizerService;
import io.spring.identityadmin.workflow.wizard.dto.AssignmentChangeDto;
import io.spring.identityadmin.workflow.wizard.dto.InitiateManagementRequestDto;
import io.spring.identityadmin.workflow.wizard.dto.VirtualSubject;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import io.spring.identityadmin.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrantingWizardServiceImpl implements GrantingWizardService {

    private final io.spring.identityadmin.admin.support.context.service.UserContextService userContextService;
    private final UserManagementService userManagementService;
    private final GroupService groupService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final StudioVisualizerService visualizerService;

    @Override
    @Transactional
    public WizardInitiationDto beginManagementSession(InitiateManagementRequestDto request) {
        String contextId = UUID.randomUUID().toString();
        log.info("Beginning new granting wizard session for subject: {}/{}", request.getSubjectType(), request.getSubjectId());

        WizardContext.Subject targetSubject = new WizardContext.Subject(request.getSubjectId(), request.getSubjectType());
        Set<Long> initialAssignmentIds = getInitialAssignmentIds(targetSubject);
        String subjectName = getSubjectName(targetSubject);
        String sessionTitle = String.format("'%s' 멤버십 관리", subjectName);

        WizardContext initialContext = WizardContext.builder()
                .contextId(contextId)
                .sessionTitle(sessionTitle)
                .targetSubject(targetSubject)
                .initialAssignmentIds(initialAssignmentIds)
                .build();

        Long adminUserId = getCurrentAdminId();
        userContextService.saveWizardProgress(contextId, adminUserId, initialContext);

        return new WizardInitiationDto(contextId, "/admin/granting-wizard/" + contextId);
    }

    @Override
    @Transactional(readOnly = true)
    public WizardContext getWizardProgress(String contextId) {
        return userContextService.getWizardProgress(contextId);
    }

    @Override
    @Transactional
    public void commitAssignments(String contextId, AssignmentChangeDto finalAssignments) {
        WizardContext context = getWizardProgress(contextId);
        WizardContext.Subject subject = Optional.ofNullable(context.targetSubject())
                .orElseThrow(() -> new IllegalStateException("Management session is invalid: No target subject found."));

        log.info("Committing assignments for subject: {}/{}", subject.type(), subject.id());

        if ("USER".equalsIgnoreCase(subject.type())) {
            UserDto userDto = userManagementService.getUser(subject.id());

            // UI에서 체크된 모든 그룹 ID를 최종 목록으로 간주
            List<Long> finalGroupIds = finalAssignments.getAdded().stream()
                    .filter(a -> "GROUP".equalsIgnoreCase(a.getTargetType()))
                    .map(AssignmentChangeDto.Assignment::getTargetId)
                    .collect(Collectors.toList());
            userDto.setSelectedGroupIds(finalGroupIds);

            // 기존 사용자 수정 서비스를 재사용하여 그룹 멤버십 업데이트
            userManagementService.modifyUser(userDto);
            log.info("User {}'s group memberships have been updated.", userDto.getUsername());

        } else if ("GROUP".equalsIgnoreCase(subject.type())) {
            Group group = groupService.getGroup(subject.id()).orElseThrow(
                    () -> new IllegalArgumentException("Group not found with ID: " + subject.id())
            );

            // UI에서 체크된 모든 역할 ID를 최종 목록으로 간주
            List<Long> finalRoleIds = finalAssignments.getAdded().stream()
                    .filter(a -> "ROLE".equalsIgnoreCase(a.getTargetType()))
                    .map(AssignmentChangeDto.Assignment::getTargetId)
                    .collect(Collectors.toList());

            // 기존 그룹 수정 서비스를 재사용하여 역할 할당 업데이트
            groupService.updateGroup(group, finalRoleIds);
            log.info("Group {}'s role assignments have been updated.", group.getName());
        } else {
            throw new IllegalArgumentException("Unsupported subject type for commit: " + subject.type());
        }

        userContextService.clearWizardProgress(contextId);
        log.info("Cleared wizard session: {}", contextId);
    }

    @Override
    @Transactional(readOnly = true)
    public SimulationResultDto simulateAssignmentChanges(String contextId, AssignmentChangeDto changes) {
        WizardContext context = getWizardProgress(contextId);
        WizardContext.Subject subject = Optional.ofNullable(context.targetSubject())
                .orElseThrow(() -> new IllegalStateException("Management session is invalid: No target subject found."));

        List<EffectivePermissionDto> beforePermissions = visualizerService.getEffectivePermissionsForSubject(subject.id(), subject.type());
        Map<String, EffectivePermissionDto> beforePermMap = beforePermissions.stream()
                .collect(Collectors.toMap(EffectivePermissionDto::permissionName, p -> p, (p1, p2) -> p1));

        List<EffectivePermissionDto> afterPermissions;
        String subjectName;

        if ("USER".equalsIgnoreCase(subject.type())) {
            Users originalUser = userRepository.findByIdWithGroupsRolesAndPermissions(subject.id())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + subject.id()));
            subjectName = originalUser.getName();

            Set<Long> afterGroupIds = changes.getAdded().stream()
                    .filter(a -> "GROUP".equalsIgnoreCase(a.getTargetType()))
                    .map(AssignmentChangeDto.Assignment::getTargetId)
                    .collect(Collectors.toSet());

            Set<Group> virtualGroups = CollectionUtils.isEmpty(afterGroupIds) ? new HashSet<>() : new HashSet<>(groupRepository.findAllById(afterGroupIds));
            VirtualSubject virtualSubject = new VirtualSubject(originalUser, virtualGroups);
            afterPermissions = visualizerService.getEffectivePermissionsForSubject(virtualSubject);

        } else if ("GROUP".equalsIgnoreCase(subject.type())) {
            Group originalGroup = groupRepository.findById(subject.id())
                    .orElseThrow(() -> new IllegalArgumentException("Group not found: " + subject.id()));
            subjectName = originalGroup.getName();

            Set<Long> afterRoleIds = changes.getAdded().stream()
                    .filter(a -> "ROLE".equalsIgnoreCase(a.getTargetType()))
                    .map(AssignmentChangeDto.Assignment::getTargetId)
                    .collect(Collectors.toSet());

            // ========================= [오류 수정된 로직] =========================
            // flatMap을 통해 Role -> RolePermission -> Permission으로 순차적으로 접근하면서
            // Role의 컨텍스트(role.getRoleName())를 유지하여 DTO를 생성합니다.
            afterPermissions = roleRepository.findAllById(afterRoleIds).stream() // Stream<Role>
                    .flatMap(role -> // 각 역할에 대해
                            role.getRolePermissions().stream() // 역할에 연결된 RolePermission 목록을 스트림으로 변환
                                    .map(rolePermission -> { // 각 RolePermission에 대해
                                        Permission perm = rolePermission.getPermission();
                                        String origin = "역할: " + role.getRoleName(); // Role 컨텍스트가 살아있어 역할 이름을 가져올 수 있음
                                        return new EffectivePermissionDto(perm.getName(), perm.getDescription(), origin);
                                    })
                    )
                    .distinct() // 중복된 권한 제거
                    .collect(Collectors.toList());
            // ====================================================================

        } else {
            return new SimulationResultDto("알 수 없는 주체 타입입니다.", Collections.emptyList());
        }

        Map<String, EffectivePermissionDto> afterPermMap = afterPermissions.stream()
                .collect(Collectors.toMap(EffectivePermissionDto::permissionName, p -> p, (p1, p2) -> p1));

        List<SimulationResultDto.ImpactDetail> impacts = new ArrayList<>();

        afterPermMap.forEach((name, perm) -> {
            if (!beforePermMap.containsKey(name)) {
                impacts.add(new SimulationResultDto.ImpactDetail(subjectName, subject.type(), perm.permissionDescription(),
                        SimulationResultDto.ImpactType.PERMISSION_GAINED, perm.origin()));
            }
        });

        beforePermMap.forEach((name, perm) -> {
            if (!afterPermMap.containsKey(name)) {
                impacts.add(new SimulationResultDto.ImpactDetail(subjectName, subject.type(), perm.permissionDescription(),
                        SimulationResultDto.ImpactType.PERMISSION_LOST, "멤버십 변경으로 인한 권한 회수"));
            }
        });

        long gainedCount = impacts.stream().filter(i -> i.impactType() == SimulationResultDto.ImpactType.PERMISSION_GAINED).count();
        long lostCount = impacts.stream().filter(i -> i.impactType() == SimulationResultDto.ImpactType.PERMISSION_LOST).count();
        String summary = String.format("권한 %d개 획득, %d개 상실 예상", gainedCount, lostCount);

        return new SimulationResultDto(summary, impacts);
    }

    private Set<Long> getInitialAssignmentIds(WizardContext.Subject subject) {
        if ("USER".equalsIgnoreCase(subject.type())) {
            UserDto user = userManagementService.getUser(subject.id());
            return user.getSelectedGroupIds() != null ? new HashSet<>(user.getSelectedGroupIds()) : new HashSet<>();
        } else if ("GROUP".equalsIgnoreCase(subject.type())) {
            Group group = groupService.getGroup(subject.id()).orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + subject.id()));
            return group.getGroupRoles().stream().map(gr -> gr.getRole().getId()).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private String getSubjectName(WizardContext.Subject subject) {
        if ("USER".equalsIgnoreCase(subject.type())) {
            return userManagementService.getUser(subject.id()).getName();
        } else if ("GROUP".equalsIgnoreCase(subject.type())) {
            return groupService.getGroup(subject.id()).map(Group::getName).orElse("알 수 없는 그룹");
        }
        return "알 수 없는 주체";
    }

    private Long getCurrentAdminId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Users) {
            return ((Users) principal).getId();
        } else if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUsers().getId();
        }
        else if (principal instanceof UserDto) {
            return ((UserDto) principal).getId();
        }
        log.warn("Cannot determine current admin user ID from principal of type: {}", principal.getClass().getName());
        return 0L;
    }
}