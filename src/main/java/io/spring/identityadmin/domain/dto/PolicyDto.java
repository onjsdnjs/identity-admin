package io.spring.identityadmin.domain.dto;

import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.domain.entity.policy.PolicyCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDto {
    private Long id;
    private String name;
    private String description;
    private Policy.Effect effect;
    private int priority;

    @Builder.Default
    private List<TargetDto> targets = new ArrayList<>();

    @Builder.Default
    private List<RuleDto> rules = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TargetDto {
        private String targetType;
        private String targetIdentifier;
        private String httpMethod;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RuleDto {
        private String description;

        // [최종 수정] conditions 필드의 타입을 List<ConditionDto>로 변경
        @Builder.Default
        private List<ConditionDto> conditions = new ArrayList<>();
    }

    /**
     * [최종 수정] 개별 조건의 상세 정보(표현식, 인가 시점)를 모두 담는 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConditionDto {
        private String expression;
        private PolicyCondition.AuthorizationPhase authorizationPhase;
    }
}