package io.spring.identityadmin.security.xacml.pap.service;

import io.spring.identityadmin.admin.repository.BusinessResourceActionRepository;
import io.spring.identityadmin.domain.entity.Users;
import io.spring.identityadmin.domain.entity.business.BusinessResourceAction;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.resource.ManagedResourceRepository;
import io.spring.identityadmin.domain.dto.GrantRequestDto;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.dto.RevokeRequestDto;
import io.spring.identityadmin.security.xacml.pip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 워크벤치 UI로부터의 사용자 친화적 권한 부여/회수 요청을 처리하는 서비스.
 * 사용자 요청을 실제 기술 정책(Policy)으로 변환하는 책임을 진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccessGrantServiceImpl implements AccessGrantService {

    private final PolicyService policyService;
    private final ManagedResourceRepository managedResourceRepository;
    private final BusinessResourceActionRepository businessResourceActionRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    /**
     * GrantRequestDto를 기반으로 새로운 Policy를 생성합니다.
     * @param grantRequest 권한 부여 요청 정보
     * @return 생성된 Policy 엔티티
     */
    @Override
    public Policy grantAccess(GrantRequestDto grantRequest) {
        log.info("Processing access grant request. Reason: {}", grantRequest.grantReason());
        if (CollectionUtils.isEmpty(grantRequest.subjects()) || CollectionUtils.isEmpty(grantRequest.resourceIds())) {
            throw new IllegalArgumentException("Subjects and Resources cannot be empty.");
        }
        PolicyDto policyDto = translateToPolicyDto(grantRequest);
        return policyService.createPolicy(policyDto);
    }

    /**
     * Policy ID를 기반으로 정책을 삭제(권한 회수)합니다.
     * @param revokeRequest 권한 회수 요청 정보
     */
    @Override
    public void revokeAccess(RevokeRequestDto revokeRequest) {
        log.info("Processing access revoke request for policyId: {}. Reason: {}",
                revokeRequest.policyId(), revokeRequest.revokeReason());
        policyService.deletePolicy(revokeRequest.policyId());
    }

    /**
     * 사용자 친화적인 GrantRequestDto를 기술적인 PolicyDto로 변환하는 핵심 메서드.
     */
    private PolicyDto translateToPolicyDto(GrantRequestDto request) {
        // 1. 주체(Subject)에 대한 SpEL 조건 생성 (OR로 연결)
        String subjectExpression = request.subjects().stream()
                .map(subject -> {
                    if ("GROUP".equalsIgnoreCase(subject.type())) {
                        // 그룹에 속한 사용자는 'GROUP_{ID}' 형태의 권한을 갖도록 약속 (CustomUserDetails 수정 필요)
                        return String.format("hasAuthority('GROUP_%d')", subject.id());
                    }
                    if ("USER".equalsIgnoreCase(subject.type())) {
                        String username = userRepository.findById(subject.id()).map(Users::getUsername)
                                .orElseThrow(() -> new IllegalArgumentException("User not found: " + subject.id()));
                        // 사용자 이름 직접 비교
                        return String.format("authentication.name == '%s'", username);
                    }
                    throw new IllegalArgumentException("Invalid subject type: " + subject.type());
                })
                .collect(Collectors.joining(" or "));

        // 2. 행위(Action)에 대한 SpEL 조건 생성 (AND로 연결)
        String actionExpression = "";
        if (!CollectionUtils.isEmpty(request.actionIds())) {
            actionExpression = request.actionIds().stream()
                    .map(actionId -> {
                        // 모든 리소스에 대해 해당 actionId가 유효한지 확인해야 하나, 여기서는 첫번째 리소스 기준으로 매핑 조회
                        Long resourceId = request.resourceIds().getFirst();
                        BusinessResourceAction.BusinessResourceActionId mappingId = new BusinessResourceAction.BusinessResourceActionId(resourceId, actionId);
                        BusinessResourceAction mapping = businessResourceActionRepository.findById(mappingId)
                                .orElseThrow(() -> new IllegalArgumentException("Action ID " + actionId + " is not valid for Resource ID " + resourceId));
                        return String.format("hasAuthority('%s')", mapping.getMappedPermissionName());
                    })
                    .collect(Collectors.joining(" and "));
        }

        // 3. 최종 조건 조합
        List<String> finalConditions = new ArrayList<>();
        if (!subjectExpression.isEmpty()) finalConditions.add("(" + subjectExpression + ")");
        if (!actionExpression.isEmpty()) finalConditions.add("(" + actionExpression + ")");

        // 4. 리소스 타겟(Target) 목록 생성
        List<PolicyDto.TargetDto> targets = request.resourceIds().stream()
                .map(id -> managedResourceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Resource not found: " + id)))
                .map(res -> new PolicyDto.TargetDto(res.getResourceType().name(), res.getResourceIdentifier(), "ALL"))
                .collect(Collectors.toList());

        PolicyDto.RuleDto rule = PolicyDto.RuleDto.builder()
                .description(request.grantReason())
                .conditions(finalConditions).build();

        String policyName = String.format("Workbench-Grant-%s-%s",
                targets.getFirst().getTargetIdentifier().replaceAll("[^a-zA-Z0-9]", ""),
                System.currentTimeMillis());

        return PolicyDto.builder()
                .name(policyName)
                .description(request.grantReason())
                .effect(Policy.Effect.ALLOW)
                .priority(500) // 워크벤치 정책은 중간 우선순위
                .targets(targets)
                .rules(List.of(rule))
                .build();
    }
}
