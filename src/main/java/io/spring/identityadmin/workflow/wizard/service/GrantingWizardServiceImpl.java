package io.spring.identityadmin.workflow.wizard.service;

import io.spring.identityadmin.admin.iam.service.GroupService;
import io.spring.identityadmin.admin.iam.service.UserManagementService;
import io.spring.identityadmin.admin.support.context.service.UserContextService;
import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.domain.entity.Group;
import io.spring.identityadmin.domain.entity.Permission;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrantingWizardServiceImpl implements GrantingWizardService {

    private final UserContextService userContextService;
    private final UserManagementService userManagementService;
    private final GroupService groupService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final StudioVisualizerService visualizerService;

    @Override
    @Transactional
    public WizardInitiationDto beginManagementSession(InitiateManagementRequestDto request) {
        Assert.notNull(request, "Request DTO cannot be null.");
        Assert.notNull(request.getSubjectId(), "Subject ID cannot be null.");
        Assert.notNull(request.getSubjectType(), "Subject Type cannot be null.");

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
    @Transactional
    public void commitAssignments(String contextId, AssignmentChangeDto finalAssignments) {
        WizardContext context = userContextService.getWizardProgress(contextId);
        WizardContext.Subject subject = context.targetSubject();

        if ("USER".equalsIgnoreCase(subject.type())) {
            UserDto userDto = userManagementService.getUser(subject.id());
            List<Long> finalGroupIds = finalAssignments.getAdded().stream()
                    .filter(a -> "GROUP".equalsIgnoreCase(a.getTargetType()))
                    .map(AssignmentChangeDto.Assignment::getTargetId)
                    .toList();
            userDto.setSelectedGroupIds(finalGroupIds);
            userManagementService.modifyUser(userDto);
        } else if ("GROUP".equalsIgnoreCase(subject.type())) {
            Group group = groupService.getGroup(subject.id()).orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + subject.id()));
            List<Long> finalRoleIds = finalAssignments.getAdded().stream()
                    .filter(a -> "ROLE".equalsIgnoreCase(a.getTargetType()))
                    .map(AssignmentChangeDto.Assignment::getTargetId)
                    .toList();
            groupService.updateGroup(group, finalRoleIds);
        }
        userContextService.clearWizardProgress(contextId);
    }

    @Override
    @Transactional(readOnly = true)
    public SimulationResultDto simulateAssignmentChanges(String contextId, AssignmentChangeDto changes) {
        WizardContext context = userContextService.getWizardProgress(contextId);
        WizardContext.Subject subject = context.targetSubject();
        List<EffectivePermissionDto> beforePermissions = visualizerService.getEffectivePermissionsForSubject(subject.id(), subject.type());
        Map<String, EffectivePermissionDto> beforePermMap = beforePermissions.stream()
                .collect(Collectors.toMap(EffectivePermissionDto::permissionName, p -> p, (p1, p2) -> p1));
        List<EffectivePermissionDto> afterPermissions;
        String subjectName;

        if ("USER".equalsIgnoreCase(subject.type())) {
            Users originalUser = userRepository.findByIdWithGroupsRolesAndPermissions(subject.id()).orElseThrow(() -> new IllegalArgumentException("User not found"));
            subjectName = originalUser.getName();
            Set<Long> afterGroupIds = changes.getAdded().stream().filter(a -> "GROUP".equalsIgnoreCase(a.getTargetType())).map(AssignmentChangeDto.Assignment::getTargetId).collect(Collectors.toSet());
            Set<Group> virtualGroups = CollectionUtils.isEmpty(afterGroupIds)
                    ? new HashSet<>()
                    : new HashSet<>(groupRepository.findAllByIdWithRolesAndPermissions(afterGroupIds));

            afterPermissions = visualizerService.getEffectivePermissionsForSubject(new VirtualSubject(originalUser, virtualGroups));
        } else if ("GROUP".equalsIgnoreCase(subject.type())) {
            Group originalGroup = groupRepository.findById(subject.id()).orElseThrow(() -> new IllegalArgumentException("Group not found"));
            subjectName = originalGroup.getName();
            Set<Long> afterRoleIds = changes.getAdded().stream().filter(a -> "ROLE".equalsIgnoreCase(a.getTargetType())).map(AssignmentChangeDto.Assignment::getTargetId).collect(Collectors.toSet());
            afterPermissions = roleRepository.findAllByIdWithPermissions(afterRoleIds).stream()
                    .flatMap(role -> role.getRolePermissions().stream().map(rp -> new EffectivePermissionDto(rp.getPermission().getName(), rp.getPermission().getDescription(), "역할: " + role.getRoleName())))
                    .distinct().collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException("Unsupported subject type for simulation: " + subject.type());
        }

        Map<String, EffectivePermissionDto> afterPermMap = afterPermissions.stream().collect(Collectors.toMap(EffectivePermissionDto::permissionName, p -> p, (p1, p2) -> p1));
        List<SimulationResultDto.ImpactDetail> impacts = new ArrayList<>();
        afterPermMap.forEach((name, perm) -> {
            if (!beforePermMap.containsKey(name)) {
                impacts.add(new SimulationResultDto.ImpactDetail(
                        subjectName,
                        subject.type(),
                        perm.permissionName(),        // 1. 기술 이름
                        perm.permissionDescription(), // 2. 사용자 친화적 설명
                        SimulationResultDto.ImpactType.PERMISSION_GAINED,
                        perm.origin()
                ));
            }
        });
        beforePermMap.forEach((name, perm) -> {
            if (!afterPermMap.containsKey(name)) {
                // 'perm' 객체는 '변경 전'의 완전한 정보를 담고 있음
                impacts.add(new SimulationResultDto.ImpactDetail(
                        subjectName,
                        subject.type(),
                        perm.permissionName(), // 기술 이름
                        perm.permissionDescription(), // 사용자 친화적 설명
                        SimulationResultDto.ImpactType.PERMISSION_LOST,
                        "멤버십 변경으로 인한 권한 회수"
                ));
            }
        });
        long gainedCount = impacts.stream().filter(i -> i.impactType() == SimulationResultDto.ImpactType.PERMISSION_GAINED).count();
        long lostCount = impacts.stream().filter(i -> i.impactType() == SimulationResultDto.ImpactType.PERMISSION_LOST).count();
        return new SimulationResultDto(String.format("권한 %d개 획득, %d개 상실 예상", gainedCount, lostCount), impacts);
    }
    @Override
    @Transactional(readOnly = true)
    public WizardContext getWizardProgress(String contextId) {
        return userContextService.getWizardProgress(contextId);
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assert.notNull(authentication, "Authentication object must not be null");
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDto userDto) return userDto.getId();
        if (principal instanceof CustomUserDetails userDetails) return userDetails.getUsers().getId();
        if (principal instanceof Users user) return user.getId();
        throw new IllegalStateException("Cannot determine admin user ID from principal of type: " + principal.getClass().getName());
    }
}