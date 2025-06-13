package io.spring.identityadmin.workflow.wizard.service;

import io.spring.identityadmin.admin.support.context.service.UserContextService;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.security.xacml.pap.service.PolicyService;
import io.spring.identityadmin.studio.dto.InitiateGrantRequestDto;
import io.spring.identityadmin.studio.dto.WizardInitiationDto;
import io.spring.identityadmin.workflow.translator.BusinessPolicyTranslator;
import io.spring.identityadmin.workflow.wizard.dto.SavePermissionsRequest;
import io.spring.identityadmin.workflow.wizard.dto.SaveSubjectsRequest;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionWizardServiceImpl implements PermissionWizardService {

    private final UserContextService userContextService;
    private final BusinessPolicyTranslator policyTranslator;
    private final PolicyService policyService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public WizardInitiationDto beginCreation(InitiateGrantRequestDto request, String policyName, String policyDescription) {
        String contextId = UUID.randomUUID().toString();
        log.info("Beginning new permission grant wizard. Context ID: {}", contextId);

        Set<WizardContext.Subject> subjects = new HashSet<>();
        if (!CollectionUtils.isEmpty(request.userIds())) {
            request.userIds().stream()
                    .map(id -> new WizardContext.Subject(id, "USER"))
                    .forEach(subjects::add);
        }
        if (!CollectionUtils.isEmpty(request.groupIds())) {
            request.groupIds().stream()
                    .map(id -> new WizardContext.Subject(id, "GROUP"))
                    .forEach(subjects::add);
        }

        WizardContext initialContext = new WizardContext(contextId, policyName, policyDescription, subjects, request.permissionIds(), null);

        Long currentUserId = getCurrentUserId();
        userContextService.saveWizardProgress(contextId, currentUserId, initialContext);

        return new WizardInitiationDto(contextId, "/admin/policy-wizard/" + contextId);
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
        request.userIds().forEach(id -> newSubjects.add(new WizardContext.Subject(id, "USER")));
        request.groupIds().forEach(id -> newSubjects.add(new WizardContext.Subject(id, "GROUP")));

        WizardContext updatedContext = new WizardContext(
                contextId, currentContext.policyName(), currentContext.policyDescription(),
                newSubjects, currentContext.permissionIds(), currentContext.conditions()
        );
        userContextService.saveWizardProgress(contextId, getCurrentUserId(), updatedContext);
        return updatedContext;
    }

    @Override
    @Transactional
    public WizardContext updatePermissions(String contextId, SavePermissionsRequest request) {
        WizardContext currentContext = userContextService.getWizardProgress(contextId);
        log.info("Updating permissions for wizard context: {}", contextId);

        WizardContext updatedContext = new WizardContext(
                contextId, currentContext.policyName(), currentContext.policyDescription(),
                currentContext.subjects(), request.permissionIds(), currentContext.conditions()
        );
        userContextService.saveWizardProgress(contextId, getCurrentUserId(), updatedContext);
        return updatedContext;
    }

    @Override
    @Transactional
    public WizardContext updatePolicyDetails(String contextId, String policyName, String policyDescription) {
        WizardContext currentContext = userContextService.getWizardProgress(contextId);
        log.info("Updating policy details for wizard context: {}", contextId);

        WizardContext updatedContext = new WizardContext(
                contextId, policyName, policyDescription,
                currentContext.subjects(), currentContext.permissionIds(), currentContext.conditions()
        );
        userContextService.saveWizardProgress(contextId, getCurrentUserId(), updatedContext);
        return updatedContext;
    }

    @Override
    @Transactional
    public Policy commitPolicy(String contextId) {
        log.info("Committing policy for wizard context: {}", contextId);
        WizardContext context = userContextService.getWizardProgress(contextId);

        if (CollectionUtils.isEmpty(context.subjects()) || CollectionUtils.isEmpty(context.permissionIds())) {
            throw new IllegalStateException("정책을 생성하려면 주체와 권한이 반드시 선택되어야 합니다.");
        }

        Policy newPolicy = policyTranslator.translate(context);
        PolicyDto policyDto = modelMapper.map(newPolicy, PolicyDto.class);

        Policy createdPolicy = policyService.createPolicy(policyDto);
        log.info("Policy successfully created with ID: {}", createdPolicy.getId());

        userContextService.clearWizardProgress(contextId);
        return createdPolicy;
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDto) { // principal이 Users 타입인 경우
            return ((UserDto) principal).getId();
        }
        log.warn("Could not determine current user ID. Returning 0 as fallback.");
        return 0L; // 익명 또는 시스템 사용자인 경우
    }
}