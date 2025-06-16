package io.spring.identityadmin.workflow.wizard.dto;

import lombok.Builder;

import java.io.Serializable;
import java.util.Set;

/**
 * [최종 수정]
 * 권한 부여 마법사(Granting Wizard)의 관리 세션 정보를 담는 DTO.
 * Redis에 직렬화되어 저장됩니다.
 */
@Builder
public record WizardContext(
        String contextId,
        String sessionTitle,
        String sessionDescription,

        /**
         * 이 마법사 세션에서 관리할 대상 주체(사용자 또는 그룹) 정보.
         */
        Subject targetSubject,

        /**
         * 마법사 시작 시점의 주체의 초기 멤버십 ID 목록.
         * (예: 주체가 USER이면 초기 그룹 ID 목록, 주체가 GROUP이면 초기 역할 ID 목록)
         * 이 정보는 시뮬레이션 시 '변경 전' 상태를 계산하는 기준이 됩니다.
         */
        Set<Long> initialAssignmentIds

) implements Serializable {

    private static final long serialVersionUID = 3L; // 버전 변경

    /**
     * 주체(Subject) 정보를 담는 중첩 레코드.
     * @param id 주체의 고유 ID (userId 또는 groupId)
     * @param type 주체의 타입 ("USER" 또는 "GROUP")
     */
    @Builder
    public record Subject(Long id, String type) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}