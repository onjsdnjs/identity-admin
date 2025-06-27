package io.spring.iam.admin.workflow.translator;

import io.spring.iam.domain.entity.policy.Policy;
import io.spring.iam.admin.workflow.wizard.dto.WizardContext;

public interface BusinessPolicyTranslator {
    Policy translate(WizardContext context);
}
