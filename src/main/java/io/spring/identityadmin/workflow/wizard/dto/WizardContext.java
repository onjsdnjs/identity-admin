package io.spring.identityadmin.workflow.wizard.dto;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * [최종] 권한 부여 마법사의 전체 진행 상태를 담는 컨텍스트 DTO 입니다.
 * 주체의 ID와 Type을 명확하게 결합하여 데이터 무결성을 보장합니다.
 */
public final class WizardContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String contextId;
    private final String policyName;
    private final String policyDescription;
    private final Set<Subject> subjects; // [오류 수정] ID와 Type을 결합한 객체 Set으로 변경
    private final Set<Long> permissionIds;
    private final Map<String, Object> conditions;

    public record Subject(Long id, String type) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public WizardContext(String contextId, String policyName, String policyDescription, Set<Subject> subjects, Set<Long> permissionIds, Map<String, Object> conditions) {
        this.contextId = contextId;
        this.policyName = policyName;
        this.policyDescription = policyDescription;
        this.subjects = subjects;
        this.permissionIds = permissionIds;
        this.conditions = conditions;
    }

    // Getters
    public String contextId() { return contextId; }
    public String policyName() { return policyName; }
    public String policyDescription() { return policyDescription; }
    public Set<Subject> subjects() { return subjects; }
    public Set<Long> permissionIds() { return permissionIds; }
    public Map<String, Object> conditions() { return conditions; }
}