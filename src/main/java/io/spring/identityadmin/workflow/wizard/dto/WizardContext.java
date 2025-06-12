package io.spring.identityadmin.workflow.wizard.dto;


import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class WizardContext {
    private final String contextId;
    private final String policyName;
    private final String policyDescription;
    private final Set<Subject> subjects; // [버그 수정] ID와 Type을 결합한 객체 Set으로 변경
    private final Set<Long> permissionIds;
    private final Map<String, Object> conditions;

    public record Subject(Long id, String type) {}

    // 생성자, Getter, equals, hashCode 등
    public WizardContext(String contextId, String policyName, String policyDescription, Set<Subject> subjects, Set<Long> permissionIds, Map<String, Object> conditions) {
        this.contextId = contextId;
        this.policyName = policyName;
        this.policyDescription = policyDescription;
        this.subjects = subjects;
        this.permissionIds = permissionIds;
        this.conditions = conditions;
    }
    public String contextId() { return contextId; }
    public String policyName() { return policyName; }
    public String policyDescription() { return policyDescription; }
    public Set<Subject> subjects() { return subjects; }
    public Set<Long> permissionIds() { return permissionIds; }
    public Map<String, Object> conditions() { return conditions; }
    @Override
    public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; WizardContext that = (WizardContext) o; return Objects.equals(contextId, that.contextId); }
    @Override
    public int hashCode() { return Objects.hash(contextId); }
}