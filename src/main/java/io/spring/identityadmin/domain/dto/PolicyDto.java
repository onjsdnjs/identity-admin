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

    // ✅ 내부 클래스 분리 완료 - 별도 파일로 이동됨
}