package io.spring.identityadmin.workflow.wizard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * [신규 DTO]
 * 권한 할당 변경사항(Delta)을 담는 DTO.
 * 시뮬레이션 및 최종 커밋에 사용됩니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentChangeDto {

    // 현재 단계에서는 'added' 필드만 사용하여 최종 할당 목록을 전달합니다.
    private Set<Assignment> added = new HashSet<>();
    private Set<Long> removedGroupIds = new HashSet<>();
    private Set<Long> removedRoleIds = new HashSet<>();

    /**
     * 개별 할당 정보를 나타내는 레코드. 기간제 할당을 지원합니다.
     * @param targetId 할당 대상 엔티티(Group/Role)의 ID
     * @param targetType "GROUP" 또는 "ROLE"
     * @param validUntil (Optional) 할당 만료 일시
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Assignment {
        @NotNull
        private Long targetId;
        @NotBlank
        private String targetType;
        private LocalDateTime validUntil; // Phase 3에서 사용될 필드
    }
}
