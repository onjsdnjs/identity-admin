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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrantingWizardServiceImpl implements GrantingWizardService {

    // UserContextService는 세션 관리를 위해 재사용
    private final io.spring.identityadmin.admin.support.context.service.UserContextService userContextService;
    private final UserManagementService userManagementService;
    private final GroupService groupService;
    private final StudioVisualizerService visualizerService;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public WizardInitiationDto beginManagementSession(InitiateManagementRequestDto request) {
        String contextId = UUID.randomUUID().toString();
        log.info("Beginning new granting wizard session for subject: {}/{}", request.getSubjectType(), request.getSubjectId());

        // TODO: WizardContext DTO를 확장하여 관리 대상 주체 정보 및 현재 멤버십 정보를 담도록 설계 필요
        // 이 단계에서는 임시로 컨텍스트를 생성하고 저장하는 로직만 구현
        WizardContext initialContext = new WizardContext(contextId, "Granting Session", "", null, null, null);

        // TODO: 현재 로그인한 관리자 ID를 가져오는 로직 필요
        Long adminUserId = 1L; // 임시 값
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
        // TODO: context에서 관리 대상 주체(subjectId, subjectType) 정보를 가져와야 함
        Long subjectId = 1L; // 임시 값
        String subjectType = "USER"; // 임시 값

        log.info("Committing assignments for subject: {}/{}", subjectType, subjectId);

        if ("USER".equalsIgnoreCase(subjectType)) {
            UserDto userDto = userManagementService.getUser(subjectId);
            // AssignmentChangeDto에 담긴 최종 그룹 ID 목록으로 설정
            userDto.setSelectedGroupIds(
                    finalAssignments.getAdded().stream()
                            .filter(a -> "GROUP".equals(a.getTargetType()))
                            .map(AssignmentChangeDto.Assignment::getTargetId)
                            .collect(Collectors.toList())
            );
            userManagementService.modifyUser(userDto);

        } else if ("GROUP".equalsIgnoreCase(subjectType)) {
            Group group = groupService.getGroup(subjectId).orElseThrow();
            // AssignmentChangeDto에 담긴 최종 역할 ID 목록으로 설정
            groupService.updateGroup(group,
                    finalAssignments.getAdded().stream()
                            .filter(a -> "ROLE".equals(a.getTargetType()))
                            .map(AssignmentChangeDto.Assignment::getTargetId)
                            .collect(Collectors.toList())
            );
        } else {
            throw new IllegalArgumentException("Unsupported subject type: " + subjectType);
        }

        // 작업 완료 후 세션 정리
        userContextService.clearWizardProgress(contextId);
    }

    @Override
    @Transactional(readOnly = true)
    public SimulationResultDto simulateAssignmentChanges(String contextId, AssignmentChangeDto changes) {
        // TODO: contextId에서 관리 대상 주체 정보를 가져와야 함
        Long subjectId = 1L; // 임시 값
        String subjectType = "USER"; // 임시 값

        // 1. 변경 전 권한 계산
        List<EffectivePermissionDto> beforePermissions = visualizerService.getEffectivePermissionsForSubject(subjectId, subjectType);

        // 2. 가상 주체 생성 및 변경 후 권한 계산
        Users originalUser = userRepository.findByIdWithGroupsRolesAndPermissions(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + subjectId));

        Set<Long> originalGroupIds = originalUser.getUserGroups().stream()
                .map(ug -> ug.getGroup().getId())
                .collect(Collectors.toSet());

        Set<Long> afterGroupIds = new HashSet<>(originalGroupIds);
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

        // 3. 변경점(Gained/Lost) 비교 분석
        List<SimulationResultDto.ImpactDetail> impacts = new ArrayList<>();
        Map<String, EffectivePermissionDto> beforePermMap = beforePermissions.stream()
                .collect(Collectors.toMap(EffectivePermissionDto::permissionName, p -> p));
        Map<String, EffectivePermissionDto> afterPermMap = afterPermissions.stream()
                .collect(Collectors.toMap(EffectivePermissionDto::permissionName, p -> p));

        // 획득한 권한
        afterPermMap.forEach((name, perm) -> {
            if (!beforePermMap.containsKey(name)) {
                impacts.add(new SimulationResultDto.ImpactDetail(originalUser.getName(), "USER", name,
                        SimulationResultDto.ImpactType.PERMISSION_GAINED, perm.origin()));
            }
        });

        // 상실한 권한
        beforePermMap.forEach((name, perm) -> {
            if (!afterPermMap.containsKey(name)) {
                impacts.add(new SimulationResultDto.ImpactDetail(originalUser.getName(), "USER", name,
                        SimulationResultDto.ImpactType.PERMISSION_LOST, "멤버십 변경으로 인한 권한 회수"));
            }
        });

        String summary = String.format("총 %d개의 권한 변경이 예상됩니다.", impacts.size());
        return new SimulationResultDto(summary, impacts);
    }
}