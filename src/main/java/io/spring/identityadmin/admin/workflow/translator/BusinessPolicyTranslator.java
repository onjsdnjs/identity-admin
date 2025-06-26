package io.spring.identityadmin.admin.workflow.translator;

import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.admin.workflow.wizard.dto.WizardContext;

public interface BusinessPolicyTranslator {
    Policy translate(WizardContext context);
}
