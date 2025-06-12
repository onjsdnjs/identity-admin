package io.spring.identityadmin.workflow.wizard.service;

import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.studio.dto.InitiateGrantRequestDto;
import io.spring.identityadmin.studio.dto.WizardInitiationDto;
import io.spring.identityadmin.workflow.wizard.dto.SavePermissionsRequest;
import io.spring.identityadmin.workflow.wizard.dto.SaveSubjectsRequest;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;

public interface PermissionWizardService {
    WizardInitiationDto beginCreation(InitiateGrantRequestDto request, String policyName, String policyDescription);
    WizardContext updateSubjects(String contextId, SaveSubjectsRequest request);
    WizardContext updatePermissions(String contextId, SavePermissionsRequest request);
    WizardContext updatePolicyDetails(String contextId, String policyName, String policyDescription);
    Policy commitPolicy(String contextId);
    WizardContext getWizardProgress(String contextId);
}