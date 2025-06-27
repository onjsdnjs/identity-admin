package io.spring.iam.admin.studio.dto;

import io.spring.iam.domain.dto.PolicyDto;

/**
 * 정책 시뮬레이션 실행을 요청하기 위한 DTO 입니다.
 */
public record SimulationRequestDto(
        ActionType actionType,
        PolicyDto policyDraft // 생성, 수정, 삭제 대상이 되는 정책의 데이터
) {
    public enum ActionType {
        CREATE,
        UPDATE,
        DELETE
    }
}