package io.spring.identityadmin.domain.dto;

import io.spring.identityadmin.domain.entity.policy.Policy;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class BusinessPolicyDto {
    private String policyName;
    private String description;

    // 주체 (누가): 사용자 ID 목록과 그룹 ID 목록을 명확히 분리
    private List<Long> subjectUserIds;
    private List<Long> subjectGroupIds;

    // 자원 (무엇을): 단일 자원이 아닌 여러 자원에 동시 적용 가능하도록 리스트로 변경
    private List<Long> businessResourceIds;

    // 행위 (어떻게): 여러 행위를 동시에 선택 가능하도록 리스트로 변경
    private List<Long> businessActionIds;

    // 조건 (어떤 상황에서): [핵심 개선] 사용자가 선택한 조건 템플릿과 파라미터를 담는 구조
    // Key: ConditionTemplate ID, Value: 해당 조건에 사용자가 입력한 파라미터 값 리스트
    private Map<Long, List<String>> conditions;

    // 효과 (허용/거부)
    private Policy.Effect effect = Policy.Effect.ALLOW; // 기본값은 허용
}
