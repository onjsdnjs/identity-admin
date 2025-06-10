package io.spring.identityadmin.iamw;

import io.spring.identityadmin.admin.repository.GroupRepository;
import io.spring.identityadmin.admin.repository.PermissionRepository;
import io.spring.identityadmin.admin.repository.PolicyRepository;
import io.spring.identityadmin.admin.service.PolicyService;
import io.spring.identityadmin.domain.dto.EntitlementDto;
import io.spring.identityadmin.domain.dto.GrantRequestDto;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.dto.RevokeRequestDto;
import io.spring.identityadmin.entity.Group;
import io.spring.identityadmin.entity.Permission;
import io.spring.identityadmin.entity.Users;
import io.spring.identityadmin.entity.policy.Policy;
import io.spring.identityadmin.repository.UserRepository;
import io.spring.identityadmin.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AccessGrantServiceImpl implements AccessGrantService {

    private final PolicyService policyService;
    private final ManagedResourceRepository managedResourceRepository;
    private final PermissionRepository permissionRepository; // 행위(Permission) 조회를 위해

    @Override
    public List<Policy> grantAccess(GrantRequestDto grantRequest) {
        log.info("Processing access grant request. Reason: {}", grantRequest.grantReason());
        PolicyDto policyDto = translateToPolicyDto(grantRequest);

        // 여러 리소스에 대해 각각 정책을 생성해야 할 수도 있음. 여기서는 단일 정책으로 가정.
        Policy createdPolicy = policyService.createPolicy(policyDto);
        return List.of(createdPolicy);
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
                .map(subject -> {
                    // 예시: subject.type()이 "ROLE" 또는 "GROUP" 이라면
                    return String.format("hasAuthority('GROUP_%d')", subject.id()); // ID 기반으로 권한 표현
                })
                .collect(Collectors.joining(" or "));

        // 2. 행위(Action)에 대한 SpEL 조건 생성
        String actionExpression = request.actions().stream()
                .map(actionName -> {
                    Permission perm = permissionRepository.findByName(actionName)
                            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + actionName));
                    return String.format("hasAuthority('%s')", perm.getName());
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
                .priority(500) // 워크벤치 정책은 우선순위를 중간 정도로 설정
                .targets(targets)
                .rules(List.of(rule))
                .build();
    }


}
