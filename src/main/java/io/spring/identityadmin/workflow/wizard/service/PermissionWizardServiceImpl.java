package io.spring.identityadmin.workflow.wizard.service;

import io.spring.identityadmin.admin.iam.service.RoleService;
import io.spring.identityadmin.admin.support.context.service.UserContextService;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.entity.Role;
import io.spring.identityadmin.domain.entity.Users;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.domain.entity.policy.PolicyCondition;
import io.spring.identityadmin.security.core.CustomUserDetails;
import io.spring.identityadmin.security.xacml.pap.service.PolicyService;
import io.spring.identityadmin.studio.dto.InitiateGrantRequestDto;
import io.spring.identityadmin.workflow.translator.BusinessPolicyTranslator;
import io.spring.identityadmin.workflow.wizard.dto.SavePermissionsRequest;
import io.spring.identityadmin.workflow.wizard.dto.SaveSubjectsRequest;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionWizardServiceImpl implements PermissionWizardService {

    private final UserContextService userContextService;
    private final RoleService roleService;

    @Override
    @Transactional
    public WizardContext beginCreation(InitiateGrantRequestDto request, String policyName, String policyDescription) {
        String contextId = UUID.randomUUID().toString();
        log.info("Beginning new permission grant wizard. Context ID: {}", contextId);

        Set<WizardContext.Subject> subjects = new HashSet<>();
        if (!CollectionUtils.isEmpty(request.getUserIds())) {
            request.getUserIds().stream().map(id -> new WizardContext.Subject(id, "USER")).forEach(subjects::add);
        }
        if (!CollectionUtils.isEmpty(request.getGroupIds())) {
            request.getGroupIds().stream().map(id -> new WizardContext.Subject(id, "GROUP")).forEach(subjects::add);
        }

        WizardContext initialContext = WizardContext.builder()
                .contextId(contextId)
                .sessionTitle(policyName)
                .sessionDescription(policyDescription)
                .subjects(subjects)
                .permissionIds(request.getPermissionIds())
                .build();

        Long currentUserId = getCurrentUserId();
        userContextService.saveWizardProgress(contextId, currentUserId, initialContext);
        return initialContext;
    }

    @Override
    public WizardContext getWizardProgress(String contextId) {
        return userContextService.getWizardProgress(contextId);
    }

    @Override
    @Transactional
    public WizardContext updateSubjects(String contextId, SaveSubjectsRequest request) {
        WizardContext currentContext = userContextService.getWizardProgress(contextId);
        log.info("Updating subjects for wizard context: {}", contextId);

        Set<WizardContext.Subject> newSubjects = new HashSet<>();
        if (request.userIds() != null) request.userIds().forEach(id -> newSubjects.add(new WizardContext.Subject(id, "USER")));
        if (request.groupIds() != null) request.groupIds().forEach(id -> newSubjects.add(new WizardContext.Subject(id, "GROUP")));

        WizardContext updatedContext = WizardContext.builder()
                .contextId(currentContext.contextId())
                .sessionTitle(currentContext.sessionTitle())
                .sessionDescription(currentContext.sessionDescription())
                .subjects(newSubjects) // 새로운 주체 정보로 교체
                .permissionIds(currentContext.permissionIds()) // 기존 권한 정보는 유지
                .targetSubject(currentContext.targetSubject())
                .initialAssignmentIds(currentContext.initialAssignmentIds())
                .build();

        userContextService.saveWizardProgress(contextId, getCurrentUserId(), updatedContext);
        return updatedContext;
    }

    @Override
    @Transactional
    public WizardContext updatePermissions(String contextId, SavePermissionsRequest request) {
        WizardContext currentContext = userContextService.getWizardProgress(contextId);
        log.info("Updating permissions for wizard context: {}", contextId);

        WizardContext updatedContext = WizardContext.builder()
                .contextId(currentContext.contextId())
                .sessionTitle(currentContext.sessionTitle())
                .sessionDescription(currentContext.sessionDescription())
                .subjects(currentContext.subjects()) // 기존 주체 정보는 유지
                .permissionIds(request.permissionIds()) // 새로운 권한 정보로 교체
                .targetSubject(currentContext.targetSubject())
                .initialAssignmentIds(currentContext.initialAssignmentIds())
                .build();

        userContextService.saveWizardProgress(contextId, getCurrentUserId(), updatedContext);
        return updatedContext;
    }

    @Override
    @Transactional
    public WizardContext updatePolicyDetails(String contextId, String policyName, String policyDescription) {
        WizardContext currentContext = userContextService.getWizardProgress(contextId);
        log.info("Updating policy details for wizard context: {}", contextId);

        WizardContext updatedContext = WizardContext.builder()
                .contextId(currentContext.contextId())
                .sessionTitle(policyName) // 새로운 정책 이름으로 교체
                .sessionDescription(policyDescription) // 새로운 정책 설명으로 교체
                .subjects(currentContext.subjects())
                .permissionIds(currentContext.permissionIds())
                .targetSubject(currentContext.targetSubject())
                .initialAssignmentIds(currentContext.initialAssignmentIds())
                .build();

        userContextService.saveWizardProgress(contextId, getCurrentUserId(), updatedContext);
        return updatedContext;
    }

    @Override
    @Transactional
    public void commitPolicy(String contextId, List<Long> selectedRoleIds) { // [수정] 시그니처 변경
        log.info("Committing permission-to-role assignment for wizard context: {}", contextId);
        WizardContext context = userContextService.getWizardProgress(contextId);

        Set<Long> permissionIds = context.permissionIds();

        if (CollectionUtils.isEmpty(selectedRoleIds) || CollectionUtils.isEmpty(permissionIds)) {
            throw new IllegalStateException("역할과 권한이 반드시 선택되어야 합니다.");
        }

        // 마법사는 단일 권한 할당을 전제로 함
        Long permissionIdToAdd = permissionIds.iterator().next();

        for (Long roleId : selectedRoleIds) {
            Role role = roleService.getRole(roleId);

            List<Long> existingPermIds = role.getRolePermissions().stream()
                    .map(rp -> rp.getPermission().getId())
                    .toList();

            if (!existingPermIds.contains(permissionIdToAdd)) {
                List<Long> newPermissionIds = new ArrayList<>(existingPermIds);
                newPermissionIds.add(permissionIdToAdd);
                // 역할 업데이트 -> 내부적으로 이벤트 발행 -> 정책 자동 동기화
                roleService.updateRole(role, newPermissionIds);
            }
        }

        userContextService.clearWizardProgress(contextId);
    }

    /**
     * [신규] Policy 엔티티를 PolicyDto로 안전하게 변환하는 헬퍼 메서드.
     * 중첩된 Target, Rule, Condition을 모두 명시적으로 변환하여 데이터 누락을 방지합니다.
     * @param policy 변환할 Policy 엔티티
     * @return 변환된 PolicyDto
     */
    private PolicyDto convertEntityToDto(Policy policy) {
        if (policy == null) return null;

        List<PolicyDto.TargetDto> targetDtos = policy.getTargets().stream()
                .map(t -> PolicyDto.TargetDto.builder()
                        .targetType(t.getTargetType())
                        .targetIdentifier(t.getTargetIdentifier())
                        .httpMethod(t.getHttpMethod())
                        .build())
                .collect(Collectors.toList());

        List<PolicyDto.RuleDto> ruleDtos = policy.getRules().stream()
                .map(r -> {
                    List<String> conditionExpressions = r.getConditions().stream()
                            .map(PolicyCondition::getExpression)
                            .collect(Collectors.toList());
                    return PolicyDto.RuleDto.builder()
                            .description(r.getDescription())
                            .conditions(conditionExpressions)
                            .build();
                })
                .collect(Collectors.toList());

        return PolicyDto.builder()
                .id(policy.getId())
                .name(policy.getName())
                .description(policy.getDescription())
                .effect(policy.getEffect())
                .priority(policy.getPriority())
                .targets(targetDtos)
                .rules(ruleDtos)
                .build();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found. Returning 0 as fallback.");
            return 0L;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            Users user = userDetails.getUsers();
            return user.getId();

        } else if (principal instanceof Users user) { // principal이 Users 타입인 경우
            return user.getId();
        } else {
            log.warn("Principal is not an instance of CustomUserDetails. Principal type: {}. Returning 0 as fallback.", principal.getClass().getName());
            return 0L;
        }
    }
}