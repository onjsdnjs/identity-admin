package io.spring.identityadmin.workflow.wizard.service;

import io.spring.identityadmin.admin.iam.service.GroupService;
import io.spring.identityadmin.admin.iam.service.UserManagementService;
import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.domain.entity.Group;
import io.spring.identityadmin.domain.entity.Users;
import io.spring.identityadmin.repository.GroupRepository;
import io.spring.identityadmin.repository.UserRepository;
import io.spring.identityadmin.studio.dto.EffectivePermissionDto;
import io.spring.identityadmin.studio.dto.SimulationResultDto;
import io.spring.identityadmin.studio.dto.WizardInitiationDto;
import io.spring.identityadmin.studio.service.StudioVisualizerService;
import io.spring.identityadmin.workflow.wizard.dto.AssignmentChangeDto;
import io.spring.identityadmin.workflow.wizard.dto.InitiateManagementRequestDto;
import io.spring.identityadmin.workflow.wizard.dto.VirtualSubject;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrantingWizardServiceImpl implements GrantingWizardService {

    private final io.spring.identityadmin.admin.support.context.service.UserContextService userContextService;
    private final UserManagementService userManagementService;
    private final GroupService groupService;
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
        String sessionTitle = String.format("'%s'님의 멤버십 관리", subjectName);

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

            // [오류 수정] finalAssignments.getAssignments() -> finalAssignments.getAdded()
            // UI에서 체크된 모든 항목이 added 리스트에 담겨 넘어온다고 가정합니다.
            List<Long> finalGroupIds = finalAssignments.getAdded().stream()
                    .filter(a -> "GROUP".equalsIgnoreCase(a.getTargetType()))
                    .map(AssignmentChangeDto.Assignment::getTargetId)
                    .collect(Collectors.toList());
            userDto.setSelectedGroupIds(finalGroupIds);
            userManagementService.modifyUser(userDto);

        } else if ("GROUP".equalsIgnoreCase(subject.type())) {
            Group group = groupService.getGroup(subject.id()).orElseThrow(
                    () -> new IllegalArgumentException("Group not found with ID: " + subject.id())
            );

            // [오류 수정] finalAssignments.getAssignments() -> finalAssignments.getAdded()
            List<Long> finalRoleIds = finalAssignments.getAdded().stream()
                    .filter(a -> "ROLE".equalsIgnoreCase(a.getTargetType()))
                    .map(AssignmentChangeDto.Assignment::getTargetId)
                    .collect(Collectors.toList());
            groupService.updateGroup(group, finalRoleIds);
        } else {
            throw new IllegalArgumentException("Unsupported subject type: " + subject.type());
        }

        userContextService.clearWizardProgress(contextId);
    }

    @Override
    @Transactional(readOnly = true)
    public SimulationResultDto simulateAssignmentChanges(String contextId, AssignmentChangeDto changes) {
        WizardContext context = getWizardProgress(contextId);
        WizardContext.Subject subject = Optional.ofNullable(context.targetSubject())
                .orElseThrow(() -> new IllegalStateException("Management session is invalid: No target subject found."));

        if (!"USER".equalsIgnoreCase(subject.type())) {
            return new SimulationResultDto("그룹/역할에 대한 시뮬레이션은 아직 지원되지 않습니다.", Collections.emptyList());
        }

        List<EffectivePermissionDto> beforePermissions = visualizerService.getEffectivePermissionsForSubject(subject.id(), subject.type());

        Users originalUser = userRepository.findByIdWithGroupsRolesAndPermissions(subject.id())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + subject.id()));

        Set<Long> afterGroupIds = new HashSet<>(context.initialAssignmentIds());
        if (changes.getRemovedGroupIds() != null) {
            afterGroupIds.removeAll(changes.getRemovedGroupIds());
        }
        if (changes.getAdded() != null) {
            changes.getAdded().stream()
                    .filter(a -> "GROUP".equalsIgnoreCase(a.getTargetType()))
                    .forEach(a -> afterGroupIds.add(a.getTargetId()));
        }

        Set<Group> virtualGroups = new HashSet<>(groupRepository.findAllById(afterGroupIds));
        VirtualSubject virtualSubject = new VirtualSubject(originalUser, virtualGroups);
        List<EffectivePermissionDto> afterPermissions = visualizerService.getEffectivePermissionsForSubject(virtualSubject);

        List<SimulationResultDto.ImpactDetail> impacts = new ArrayList<>();
        Map<String, EffectivePermissionDto> beforePermMap = beforePermissions.stream()
                .collect(Collectors.toMap(EffectivePermissionDto::permissionName, p -> p, (p1, p2) -> p1));
        Map<String, EffectivePermissionDto> afterPermMap = afterPermissions.stream()
                .collect(Collectors.toMap(EffectivePermissionDto::permissionName, p -> p, (p1, p2) -> p1));

        afterPermMap.forEach((name, perm) -> {
            if (!beforePermMap.containsKey(name)) {
                impacts.add(new SimulationResultDto.ImpactDetail(originalUser.getName(), "USER", perm.permissionDescription(),
                        SimulationResultDto.ImpactType.PERMISSION_GAINED, perm.origin()));
            }
        });

        beforePermMap.forEach((name, perm) -> {
            if (!afterPermMap.containsKey(name)) {
                impacts.add(new SimulationResultDto.ImpactDetail(originalUser.getName(), "USER", perm.permissionDescription(),
                        SimulationResultDto.ImpactType.PERMISSION_LOST, "멤버십 변경으로 인한 권한 회수"));
            }
        });

        String summary = String.format("권한 %d개 획득, %d개 상실 예상",
                (int)impacts.stream().filter(i -> i.impactType() == SimulationResultDto.ImpactType.PERMISSION_GAINED).count(),
                (int)impacts.stream().filter(i -> i.impactType() == SimulationResultDto.ImpactType.PERMISSION_LOST).count());
        return new SimulationResultDto(summary, impacts);
    }

    private Set<Long> getInitialAssignmentIds(WizardContext.Subject subject) {
        if ("USER".equalsIgnoreCase(subject.type())) {
            UserDto user = userManagementService.getUser(subject.id());
            return new HashSet<>(user.getSelectedGroupIds());
        } else if ("GROUP".equalsIgnoreCase(subject.type())) {
            Group group = groupService.getGroup(subject.id()).orElseThrow();
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
        } else if (principal instanceof io.spring.identityadmin.security.core.CustomUserDetails) {
            return ((io.spring.identityadmin.security.core.CustomUserDetails) principal).getUsers().getId();
        }
        return 0L;
    }
}