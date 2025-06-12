package io.spring.identityadmin.workflow.wizard.service;

import io.spring.identityadmin.admin.support.context.service.UserContextService;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.security.xacml.pap.service.PolicyService;
import io.spring.identityadmin.studio.dto.InitiateGrantRequestDto;
import io.spring.identityadmin.studio.dto.WizardInitiationDto;
import io.spring.identityadmin.workflow.translator.BusinessPolicyTranslator;
import io.spring.identityadmin.workflow.wizard.dto.CommitPolicyRequest;
import io.spring.identityadmin.workflow.wizard.dto.SavePermissionsRequest;
import io.spring.identityadmin.workflow.wizard.dto.SaveSubjectsRequest;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

        // [버그 수정] ID와 타입을 결합한 Subject 객체로 안전하게 변환
        Set<WizardContext.Subject> subjects = new HashSet<>();
        if (!CollectionUtils.isEmpty(request.subjectIds())) {
            request.subjectIds().forEach(id -> subjects.add(new WizardContext.Subject(id, "USER"))); // 타입 정보 필요
        }
        if (!CollectionUtils.isEmpty(request.groupIds())) {
            request.groupIds().forEach(id -> subjects.add(new WizardContext.Subject(id, "GROUP")));
        }

        WizardContext initialContext = new WizardContext(contextId, policyName, policyDescription, subjects, request.getPermissionIds(), null);
        userContextService.saveWizardProgress(contextId, 1L, initialContext); // 임시 사용자 ID 1
        return new WizardInitiationDto(contextId, "/admin/policy-wizard/" + contextId);
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
        userContextService.saveWizardProgress(contextId, 1L, updatedContext);
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
        userContextService.saveWizardProgress(contextId, 1L, updatedContext);
        return updatedContext;
    }

    @Override
    @Transactional
    public Policy commitPolicy(String contextId, CommitPolicyRequest request) {
        log.info("Committing policy for wizard context: {}", contextId);
        WizardContext context = userContextService.getWizardProgress(contextId);

        WizardContext finalContext = new WizardContext(contextId, request.policyName(), request.policyDescription(),
                context.subjects(), context.permissionIds(), context.conditions());

        if (CollectionUtils.isEmpty(finalContext.subjects()) || CollectionUtils.isEmpty(finalContext.permissionIds())) {
            throw new IllegalStateException("Policy cannot be created without subjects and permissions.");
        }

        Policy newPolicy = policyTranslator.translate(finalContext);
        PolicyDto policyDto = modelMapper.map(newPolicy, PolicyDto.class);

        Policy createdPolicy = policyService.createPolicy(policyDto);
        log.info("Policy successfully created with ID: {}", createdPolicy.getId());

        userContextService.clearWizardProgress(contextId);
        return createdPolicy;
    }
}