package io.spring.identityadmin.workflow.wizard.dto;

import java.util.Set;
import java.util.Map;

/**
 * 권한 부여 마법사의 전체 진행 상태를 담는 컨텍스트 DTO 입니다.
 */
public record WizardContext(
        String contextId,
        String policyName,
        String policyDescription,
        Set<Long> subjectIds,
        Set<String> subjectTypes,
        Set<Long> permissionIds,
        Map<Long, Map<String, Object>> conditions
) {}