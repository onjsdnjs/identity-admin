package io.spring.identityadmin.workflow.translator;

import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;

public interface BusinessPolicyTranslator {
    Policy translate(WizardContext context);
}
