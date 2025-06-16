package io.spring.identityadmin.workflow.wizard.dto;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public record WizardContext(
        String contextId,
        String sessionTitle,
        String sessionDescription,

        // [신규] 관리 대상 주체 정보
        Subject targetSubject,

        // [신규] 초기 상태 정보 (시뮬레이션 비교 기준)
        Set<Long> initialAssignmentIds,

        // [변경] 레거시 필드 (향후 제거 또는 다른 용도로 사용)
        Set<Subject> legacySubjects,
        Set<Long> legacyPermissionIds,
        Map<String, Object> legacyConditions
) implements Serializable {

    private static final long serialVersionUID = 2L; // 버전 변경

    public record Subject(Long id, String type) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}