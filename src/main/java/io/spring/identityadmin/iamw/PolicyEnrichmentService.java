package io.spring.identityadmin.iamw;

import io.spring.identityadmin.entity.policy.Policy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Policy 엔티티에 사용자 친화적인 추가 정보(예: 번역된 설명)를 생성하여 채워주는 서비스
 */
@Service
@RequiredArgsConstructor
public class PolicyEnrichmentService {

    private final PolicyTranslator policyTranslator;

    /**
     * 주어진 Policy 객체의 규칙(SpEL)을 분석하여,
     * 사람이 읽을 수 있는 설명(friendlyDescription)을 생성하고 엔티티에 설정합니다.
     * @param policy 정보를 채울 Policy 엔티티
     */
    public void enrichPolicyWithFriendlyDescription(Policy policy) {
        if (policy == null || policy.getRules() == null || policy.getRules().isEmpty()) {
            policy.setFriendlyDescription("정의된 규칙 없음");
            return;
        }

        // PolicyTranslator를 사용하여 정책을 EntitlementDto로 변환 (메모리상에서만)
        // DTO 에서 최종 설명 문자열을 조합하여 가져온다.
        String description = policyTranslator.translate(policy, "") // resourceName은 필요 없으므로 비워둠
                .map(dto -> {
                    String subjectPart = "주체(" + dto.subjectName() + ")";
                    String actionPart = "행위(" + String.join(", ", dto.actions()) + ")";
                    String conditionPart = "조건(" + String.join(" ", dto.conditions()) + ")";
                    return String.join(" | ", subjectPart, actionPart, conditionPart);
                })
                .findFirst()
                .orElse("규칙 분석 실패");

        policy.setFriendlyDescription(description);
    }
}
