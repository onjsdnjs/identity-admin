package io.spring.identityadmin.workflow.wizard.dto;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public record WizardContext(
        String contextId,
        String policyName,
        String policyDescription,
        Set<Subject> subjects,
        Set<Long> permissionIds,
        Map<String, Object> conditions
) implements Serializable {

    private static final long serialVersionUID = 1L; // Serializable을 위한 UID

    /**
     * 주체(Subject) 정보를 담는 중첩 레코드
     */
    public record Subject(Long id, String type) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}