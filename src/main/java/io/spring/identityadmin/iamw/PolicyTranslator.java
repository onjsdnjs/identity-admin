package io.spring.identityadmin.iamw;

import io.spring.identityadmin.domain.dto.EntitlementDto;
import io.spring.identityadmin.entity.policy.Policy;
import io.spring.identityadmin.entity.policy.PolicyCondition;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 기술적인 Policy 엔티티를 사용자 친화적인 EntitlementDto로 변환(해석)하는 헬퍼 클래스.
 */
@Component
public class PolicyTranslator {

    // hasAuthority('PERMISSION_NAME') 또는 hasRole('ROLE_NAME') 형태의 SpEL을 파싱하는 정규식
    private static final Pattern AUTHORITY_PATTERN = Pattern.compile("hasAuthority\\('([^']+)'\\)|hasRole\\('([^']+)'\\)");

    public Stream<EntitlementDto> translate(Policy policy, String resourceName) {
        // 실제 구현에서는 policy의 규칙(Rule)과 조건(Condition)을 복합적으로 분석해야 합니다.
        // 여기서는 하나의 규칙에 모든 조건이 AND로 연결되어 있다고 가정합니다.

        return policy.getRules().stream().map(rule -> {
            List<String> subjects = new ArrayList<>();
            List<String> actions = new ArrayList<>();
            List<String> conditions = new ArrayList<>();

            for (PolicyCondition condition : rule.getConditions()) {
                String expression = condition.getExpression();
                Matcher matcher = AUTHORITY_PATTERN.matcher(expression);

                if (matcher.matches()) {
                    String authority = matcher.group(1) != null ? matcher.group(1) : "ROLE_" + matcher.group(2);

                    // 예시: 'GROUP_', 'USER_' prefix로 주체/행위를 구분한다고 가정
                    if (authority.startsWith("GROUP_") || authority.startsWith("ROLE_")) {
                        subjects.add(authority);
                    } else {
                        actions.add(authority); // 그 외는 행위(Permission)로 간주
                    }
                } else {
                    conditions.add(expression); // 매칭되지 않는 SpEL은 그대로 조건으로 표시
                }
            }

            // 실제로는 subjects 리스트의 각 항목(e.g., 'GROUP_ADMINS')을 DB에서 조회하여
            // '관리자 그룹'과 같은 사용자 친화적 이름으로 변환해야 합니다.
            String subjectName = subjects.isEmpty() ? "Unknown Subject" : String.join(", ", subjects);

            return new EntitlementDto(
                    "GROUP", // 예시
                    subjectName,
                    resourceName,
                    actions,
                    conditions,
                    policy.getId()
            );
        });
    }
}
