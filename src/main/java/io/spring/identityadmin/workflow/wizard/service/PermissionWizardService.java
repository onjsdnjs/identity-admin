package io.spring.identityadmin.workflow.wizard.service;

import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import java.util.Set;

public interface PermissionWizardService {
    WizardContext beginPolicyCreation(WizardContext initialContext);
    WizardContext addSubjects(String contextId, Set<Long> subjectIds, Set<String> subjectTypes);
    WizardContext addPermissions(String contextId, Set<Long> permissionIds);
    Policy commitPolicy(String contextId);
}