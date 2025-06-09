package io.spring.identityadmin.domain.dto;

import io.spring.identityadmin.entity.policy.Policy;
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
    private List<TargetDto> targets = new ArrayList<>();
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
        private List<String> conditions = new ArrayList<>();
    }
}