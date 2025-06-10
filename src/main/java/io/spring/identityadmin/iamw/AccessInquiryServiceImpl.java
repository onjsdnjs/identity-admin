package io.spring.identityadmin.iamw;

import io.spring.identityadmin.admin.repository.GroupRepository;
import io.spring.identityadmin.admin.repository.PolicyRepository;
import io.spring.identityadmin.domain.dto.EntitlementDto;
import io.spring.identityadmin.entity.Group;
import io.spring.identityadmin.entity.ManagedResource;
import io.spring.identityadmin.entity.Users;
import io.spring.identityadmin.entity.policy.Policy;
import io.spring.identityadmin.repository.UserRepository;
import io.spring.identityadmin.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessInquiryServiceImpl implements AccessInquiryService {

    private final PolicyRepository policyRepository;
    private final ManagedResourceRepository managedResourceRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PolicyTranslator policyTranslator;

    private static final Pattern AUTHORITY_PATTERN = Pattern.compile("hasAuthority\\('([^']+)'\\)|hasRole\\('([^']+)'\\)");

    @Override
    public List<EntitlementDto> getEntitlementsForResource(Long resourceId) {
        ManagedResource resource = managedResourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with ID: " + resourceId));

        // PolicyRepository의 default 메서드를 호출
        List<Policy> relatedPolicies = policyRepository.findPoliciesMatchingUrl(resource.getResourceIdentifier());

        return relatedPolicies.stream()
                .flatMap(policy -> policyTranslator.translate(policy, resource.getFriendlyName()))
                .collect(Collectors.toList());
    }

    /**
     * 특정 주체가 접근할 수 있는 모든 리소스와 권한을 조회합니다.
     */
    @Override
    public List<EntitlementDto> getEntitlementsForSubject(Long subjectId, String subjectType) {
        // 1. 주체의 모든 권한(GrantedAuthority)을 Set<String> 형태로 가져옵니다.
        Set<String> subjectAuthorities = getAuthoritiesForSubject(subjectId, subjectType);
        if (subjectAuthorities.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 시스템의 모든 정책을 조회합니다.
        List<Policy> allPolicies = policyRepository.findAllWithDetails();

        // 3. 각 정책에 대해 주체가 권한을 만족하는지 검사하고, 만족하면 EntitlementDto를 생성합니다.
        return allPolicies.stream()
                .filter(policy -> policy.getEffect() == Policy.Effect.ALLOW) // 허용 정책만 검토
                .filter(policy -> isPolicySatisfiedBy(policy, subjectAuthorities))
                .flatMap(this::translatePolicyToEntitlements)
                .collect(Collectors.toList());
    }

    /**
     * 주어진 주체 ID와 타입에 따라 모든 권한(Role, Permission)을 문자열 Set으로 반환합니다.
     */
    private Set<String> getAuthoritiesForSubject(Long subjectId, String subjectType) {
        if ("USER".equalsIgnoreCase(subjectType)) {
            Users user = userRepository.findByIdWithGroupsRolesAndPermissions(subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + subjectId));
            // CustomUserDetails의 권한 계산 로직을 재사용합니다.
            return new CustomUserDetails(user).getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
        } else if ("GROUP".equalsIgnoreCase(subjectType)) {
            Group group = groupRepository.findByIdWithRoles(subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + subjectId));

            // 그룹에 직접 연결된 Role과 그 Role에 속한 Permission들을 수집합니다.
            return group.getGroupRoles().stream()
                    .flatMap(groupRole -> {
                        Stream<String> roleAuthority = Stream.of("ROLE_" + groupRole.getRole().getRoleName());
                        Stream<String> permissionAuthorities = groupRole.getRole().getRolePermissions().stream()
                                .map(rolePermission -> rolePermission.getPermission().getName());
                        return Stream.concat(roleAuthority, permissionAuthorities);
                    })
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    /**
     * 주체가 가진 권한으로 특정 정책의 모든 조건을 만족시킬 수 있는지 확인합니다.
     * (하나의 Rule 내 모든 Condition은 AND 관계로 가정)
     */
    private boolean isPolicySatisfiedBy(Policy policy, Set<String> subjectAuthorities) {
        // 정책의 규칙(Rule)들 중 하나라도 만족하면 true (OR 관계)
        return policy.getRules().stream().anyMatch(rule -> {
            // 규칙 내의 모든 조건(Condition)을 만족해야 함 (AND 관계)
            return rule.getConditions().stream().allMatch(condition -> {
                String expression = condition.getExpression();
                Matcher matcher = AUTHORITY_PATTERN.matcher(expression);
                if (matcher.matches()) {
                    String authority = matcher.group(1) != null ? matcher.group(1) : "ROLE_" + matcher.group(2);
                    return subjectAuthorities.contains(authority);
                }
                // hasAuthority/hasRole이 아닌 복잡한 SpEL은 현재 구현에서 판별 불가 (true로 간주하거나, 별도 로직 필요)
                // 여기서는 기본적인 권한 검사만 처리하므로, 그 외 조건은 무시(true)하거나 엄격하게는 false 처리.
                // 지금은 무시(true)하여 권한 기반 정책만으로 만족 여부 판단.
                return true;
            });
        });
    }

    /**
     * 만족된 정책을 기반으로, 해당 정책의 Target들을 EntitlementDto 스트림으로 변환합니다.
     */
    private Stream<EntitlementDto> translatePolicyToEntitlements(Policy policy) {
        return policy.getTargets().stream().map(target -> {
            // PolicyTranslator를 사용하여 주체, 행위, 조건 등을 해석하여 DTO를 채웁니다.
            // 여기서는 단순화된 예시를 보여줍니다.
            String resourceName = managedResourceRepository.findByResourceIdentifier(target.getTargetIdentifier())
                    .map(m -> m.getFriendlyName())
                    .orElse(target.getTargetIdentifier());

            // 실제로는 policyTranslator를 통해 더 정교하게 변환해야 함
            return new EntitlementDto(
                    "N/A", "N/A", // 이 정보는 주체 기준 조회이므로, 정책만 보고는 알기 어려움.
                    resourceName,
                    List.of("Access Granted"), // 단순화된 행위
                    List.of(), // 단순화된 조건
                    policy.getId()
            );
        });
    }
}
