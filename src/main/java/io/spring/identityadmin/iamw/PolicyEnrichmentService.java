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
        if (policy == null) {
            return;
        }

        // PolicyTranslator를 사용하여 정책을 최종 ExpressionNode 트리로 파싱
        ExpressionNode rootNode = policyTranslator.parsePolicy(policy);

        // 파싱된 노드에서 사람이 읽을 수 있는 설명 전체를 가져옴
        String description = rootNode.getConditionDescription();

        policy.setFriendlyDescription(description);
    }
}
