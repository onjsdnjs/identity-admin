package io.spring.identityadmin.workflow.wizard.dto;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * [오류 수정] Jackson 라이브러리가 직렬화할 수 있도록,
 * public getter가 자동으로 생성되는 record 타입으로 변경합니다.
 */
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