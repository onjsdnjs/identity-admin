package io.spring.iam.admin.workflow.wizard.service;

import io.spring.iam.admin.studio.dto.InitiateGrantRequestDto;
import io.spring.iam.admin.workflow.wizard.dto.SavePermissionsRequest;
import io.spring.iam.admin.workflow.wizard.dto.SaveSubjectsRequest;
import io.spring.iam.admin.workflow.wizard.dto.WizardContext;

import java.util.List;
import java.util.Set;

public interface PermissionWizardService {
    WizardContext  beginCreation(InitiateGrantRequestDto request, String policyName, String policyDescription);
    WizardContext updateSubjects(String contextId, SaveSubjectsRequest request);
    WizardContext updatePermissions(String contextId, SavePermissionsRequest request);
    void updatePolicyDetails(String contextId, String policyName, String policyDescription);
    void commitPolicy(String contextId, List<Long> selectedRoleIds, Set<Long> permissionIds);
    WizardContext getWizardProgress(String contextId);
}