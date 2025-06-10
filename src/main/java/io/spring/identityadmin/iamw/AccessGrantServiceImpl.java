package io.spring.identityadmin.iamw;

import io.spring.identityadmin.admin.repository.BusinessActionRepository;
import io.spring.identityadmin.admin.repository.BusinessResourceActionRepository;
import io.spring.identityadmin.admin.service.PolicyService;
import io.spring.identityadmin.domain.dto.GrantRequestDto;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.dto.RevokeRequestDto;
import io.spring.identityadmin.entity.BusinessResourceAction;
import io.spring.identityadmin.entity.policy.Policy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccessGrantServiceImpl implements AccessGrantService {

    private final PolicyService policyService;
    private final ManagedResourceRepository managedResourceRepository;
    private final BusinessActionRepository businessActionRepository;
    private final BusinessResourceActionRepository businessResourceActionRepository;

    @Override
    public Policy grantAccess(GrantRequestDto grantRequest) {
        log.info("Processing access grant request. Reason: {}", grantRequest.grantReason());
        PolicyDto policyDto = translateToPolicyDto(grantRequest);
        return policyService.createPolicy(policyDto);
    }

    @Override
    public void revokeAccess(RevokeRequestDto revokeRequest) {
        log.info("Processing access revoke request for policyId: {}. Reason: {}",
                revokeRequest.policyId(), revokeRequest.revokeReason());
        policyService.deletePolicy(revokeRequest.policyId());
    }

    private PolicyDto translateToPolicyDto(GrantRequestDto request) {
        // 1. 주체(Subject)에 대한 SpEL 조건 생성
        String subjectExpression = request.subjects().stream()
                .map(subject -> String.format("hasAuthority('%s_%d')", subject.type(), subject.id()))
                .collect(Collectors.joining(" or "));

        // 2. 행위(Action)에 대한 SpEL 조건 생성
        // Business* 테이블을 조회하여 사용자 친화적 행위(ID)를 기술적 권한(Permission Name)으로 변환
        String actionExpression = request.actionIds().stream()
                .map(actionId -> {
                    BusinessResourceAction.BusinessResourceActionId mappingId = new BusinessResourceAction.BusinessResourceActionId(request.resourceIds().get(0), actionId); // 현재는 첫번째 리소스 기준으로 가정
                    BusinessResourceAction mapping = businessResourceActionRepository.findById(mappingId)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid action for the given resource."));
                    return String.format("hasAuthority('%s')", mapping.getMappedPermissionName());
                })
                .collect(Collectors.joining(" and "));

        List<String> finalConditions = new ArrayList<>();
        if (!subjectExpression.isEmpty()) finalConditions.add("(" + subjectExpression + ")");
        if (!actionExpression.isEmpty()) finalConditions.add("(" + actionExpression + ")");

        // 3. 리소스 타겟(Target) 목록 생성
        List<PolicyDto.TargetDto> targets = request.resourceIds().stream()
                .map(id -> managedResourceRepository.findById(id).orElseThrow())
                .map(res -> new PolicyDto.TargetDto(res.getResourceType().name(), res.getResourceIdentifier(), "ALL"))
                .collect(Collectors.toList());

        PolicyDto.RuleDto rule = PolicyDto.RuleDto.builder()
                .description(request.grantReason())
                .conditions(finalConditions).build();

        return PolicyDto.builder()
                .name(String.format("Workbench-Grant-%s", System.currentTimeMillis()))
                .effect(Policy.Effect.ALLOW)
                .priority(500)
                .targets(targets)
                .rules(List.of(rule))
                .build();
    }
}
