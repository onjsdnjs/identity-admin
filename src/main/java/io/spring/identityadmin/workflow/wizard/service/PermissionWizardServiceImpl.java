package io.spring.identityadmin.workflow.wizard.service;

import io.spring.identityadmin.admin.facade.service.CoreServiceFacade;
import io.spring.identityadmin.admin.support.context.service.UserContextService;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.security.xacml.pap.service.PolicyService;
import io.spring.identityadmin.studio.dto.InitiateGrantRequestDto;
import io.spring.identityadmin.studio.dto.WizardInitiationDto;
import io.spring.identityadmin.workflow.translator.BusinessPolicyTranslator;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionWizardServiceImpl implements PermissionWizardService {

    private final UserContextService userContextService;
    private final BusinessPolicyTranslator policyTranslator;
    private final PolicyService policyService;
    private final ModelMapper modelMapper;

    @Override
    public WizardInitiationDto beginCreation(InitiateGrantRequestDto request) {
        String contextId = UUID.randomUUID().toString();
        Set<WizardContext.Subject> subjects = request.subjectIds().stream()
                .map(id -> new WizardContext.Subject(id, "USER")) // 타입 구분 로직 필요
                .collect(Collectors.toSet());

        WizardContext initialContext = new WizardContext(contextId,
                "Wizard-Generated-Policy-" + contextId.substring(0, 8),
                "마법사를 통해 생성된 정책",
                subjects,
                request.permissionIds(),
                null);

        userContextService.saveWizardProgress(contextId, initialContext);
        return new WizardInitiationDto(contextId, "/admin/policy-wizard/" + contextId);
    }

    @Override
    public WizardContext addSubjects(String contextId, Set<Long> subjectIds, Set<String> subjectTypes) {
        WizardContext context = userContextService.getWizardProgress(contextId);
        // ... context 업데이트 로직 ...
        WizardContext updatedContext = new WizardContext(contextId, context.policyName(), context.policyDescription(), subjectIds, subjectTypes, context.permissionIds(), context.conditions());
        userContextService.saveWizardProgress(contextId, updatedContext);
        return updatedContext;
    }

    @Override
    public WizardContext addPermissions(String contextId, Set<Long> permissionIds) {
        WizardContext context = userContextService.getWizardProgress(contextId);
        // ... context 업데이트 로직 ...
        WizardContext updatedContext = new WizardContext(contextId, context.policyName(), context.policyDescription(), context.subjectIds(), context.subjectTypes(), permissionIds, context.conditions());
        userContextService.saveWizardProgress(contextId, updatedContext);
        return updatedContext;
    }

    @Override
    public Policy commitPolicy(String contextId) {
        WizardContext context = userContextService.getWizardProgress(contextId);
        if (context == null) {
            throw new IllegalStateException("Wizard context is not found for id: " + contextId);
        }
        Policy newPolicy = policyTranslator.translate(context);

        // PolicyService를 통해 정책을 최종적으로 생성
        PolicyDto policyDto = modelMapper.map(newPolicy, PolicyDto.class);
        Policy createdPolicy = policyService.createPolicy(policyDto);

        // 완료 후 세션 정리
        userContextService.clearWizardProgress(contextId);
        return createdPolicy;
    }
}