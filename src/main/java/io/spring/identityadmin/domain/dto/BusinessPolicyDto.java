package io.spring.identityadmin.domain.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class BusinessPolicyDto {
    private String policyName;
    private String description;

    // 주체 (누가)
    private List<Long> subjectUserIds;
    private List<Long> subjectGroupIds;

    // 행위 (어떻게)
    private Long businessActionId;

    // 자원 (무엇을)
    private Long businessResourceId;

    // 조건 (어떤 상황에서)
    // Key: ConditionTemplate ID, Value: 조건에 필요한 파라미터(들)
    private Map<Long, List<String>> conditions;
}
