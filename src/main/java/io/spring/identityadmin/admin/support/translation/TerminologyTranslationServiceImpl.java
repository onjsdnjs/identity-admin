package io.spring.identityadmin.admin.support.translation;

import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.repository.PermissionRepository;
import io.spring.identityadmin.security.xacml.pdp.translator.PolicyTranslator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TerminologyTranslationServiceImpl implements TerminologyTranslationService {

    private final PermissionRepository permissionRepository;
    private final PolicyTranslator policyTranslator;

    /**
     * [최종 구현] Permission의 이름을 기반으로 사용자 친화적인 설명을 생성합니다.
     */
    @Override
    public String generatePermissionDescription(String permissionName) {
        return permissionRepository.findByName(permissionName)
                .map(p -> p.getDescription())
                .orElse(permissionName);
    }

    /**
     * [최종 구현] Policy 객체를 분석하여 자연어 요약을 생성합니다.
     */
    @Override
    public String summarizePolicy(Policy policy) {
        if (policy.getFriendlyDescription() != null && !policy.getFriendlyDescription().isEmpty()) {
            return policy.getFriendlyDescription();
        }
        // PolicyTranslator를 사용하여 SpEL을 자연어로 변환
        return policyTranslator.parsePolicy(policy).getConditionDescription();
    }
}