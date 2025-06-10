package io.spring.identityadmin.iamw;

import io.spring.identityadmin.admin.repository.GroupRepository;
import io.spring.identityadmin.admin.repository.PolicyRepository;
import io.spring.identityadmin.admin.repository.PermissionRepository;
import io.spring.identityadmin.admin.repository.RoleRepository;
import io.spring.identityadmin.domain.dto.EntitlementDto;
import io.spring.identityadmin.entity.Group;
import io.spring.identityadmin.entity.Role;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * '통합 워크벤치'의 핵심 조회 서비스.
 * 리소스 중심, 주체 중심의 접근 권한 현황을 조회하는 모든 책임을 진다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessInquiryServiceImpl implements AccessInquiryService {

    private final PolicyRepository policyRepository;
    private final ManagedResourceRepository managedResourceRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PolicyTranslator policyTranslator;
    private final List<SubjectAuthorityResolver> authorityResolvers;

    /**
     * 특정 리소스에 대한 모든 권한 부여(Entitlement) 현황을 조회합니다.
     * @param resourceId 조회할 리소스의 ID
     * @return 해당 리소스에 대한 Entitlement DTO 목록
     */
    @Override
    public List<EntitlementDto> getEntitlementsForResource(Long resourceId) {
        var resource = managedResourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with ID: " + resourceId));

        List<Policy> relatedPolicies = policyRepository.findPoliciesMatchingUrl(resource.getResourceIdentifier());

        return relatedPolicies.stream()
                .flatMap(policy -> translatePolicyToEntitlements(policy, resource.getFriendlyName()))
                .collect(Collectors.toList());
    }

    /**
     * 특정 주체(사용자/그룹)가 부여받은 모든 권한 현황을 조회합니다.
     * @param subjectId 조회할 주체의 ID
     * @param subjectType 주체의 타입 ("USER" or "GROUP")
     * @return 해당 주체에 대한 Entitlement DTO 목록
     */
    @Override
    public List<EntitlementDto> getEntitlementsForSubject(Long subjectId, String subjectType) {
        Set<String> subjectAuthorities = getAuthoritiesForSubject(subjectId, subjectType);
        if (subjectAuthorities.isEmpty()) {
            return Collections.emptyList();
        }

        List<Policy> allPolicies = policyRepository.findAllWithDetails();
        String subjectName = getFriendlySubjectName(subjectId, subjectType);
        String friendlySubjectType = "GROUP".equalsIgnoreCase(subjectType) ? "그룹" : "사용자";

        return allPolicies.stream()
                .filter(policy -> policy.getEffect() == Policy.Effect.ALLOW)
                .filter(policy -> isPolicySatisfiedBy(policy, subjectAuthorities))
                .flatMap(policy -> policy.getTargets().stream()
                        .map(target -> {
                            String resourceName = managedResourceRepository.findByResourceIdentifier(target.getTargetIdentifier())
                                    .map(m -> m.getFriendlyName())
                                    .orElse(target.getTargetIdentifier());

                            ExpressionNode rootNode = policyTranslator.parsePolicy(policy);
                            Set<String> actionAuthorities = rootNode.getRequiredAuthorities().stream()
                                    .filter(auth -> !auth.startsWith("ROLE_") && !auth.startsWith("GROUP_"))
                                    .collect(Collectors.toSet());

                            List<String> actionNames = actionAuthorities.stream()
                                    .map(this::getFriendlyPermissionName)
                                    .collect(Collectors.toList());

                            return new EntitlementDto(
                                    policy.getId(),
                                    subjectName,
                                    friendlySubjectType,
                                    resourceName,
                                    actionNames,
                                    List.of(rootNode.getConditionDescription())
                            );
                        }))
                .collect(Collectors.toList());
    }

    private String getFriendlySubjectName(Long subjectId, String subjectType) {
        if ("USER".equalsIgnoreCase(subjectType)) {
            return userRepository.findById(subjectId).map(Users::getName).orElse("알 수 없는 사용자 (ID: " + subjectId + ")");
        }
        if ("GROUP".equalsIgnoreCase(subjectType)) {
            return groupRepository.findById(subjectId).map(Group::getName).orElse("알 수 없는 그룹 (ID: " + subjectId + ")");
        }
        return "알 수 없는 주체";
    }

    /**
     * Policy 객체를 EntitlementDto 스트림으로 번역합니다. (리소스 기준 조회용)
     */
    private Stream<EntitlementDto> translatePolicyToEntitlements(Policy policy, String resourceName) {
        ExpressionNode rootNode = policyTranslator.parsePolicy(policy);
        Set<String> authorities = rootNode.getRequiredAuthorities();

        List<String> subjectNames = authorities.stream()
                .filter(auth -> auth.startsWith("ROLE_") || auth.startsWith("GROUP_"))
                .map(this::getFriendlySubjectNameFromAuthority)
                .collect(Collectors.toList());

        List<String> actionNames = authorities.stream()
                .filter(auth -> !auth.startsWith("ROLE_") && !auth.startsWith("GROUP_"))
                .map(this::getFriendlyPermissionName)
                .collect(Collectors.toList());

        String finalSubjectName = subjectNames.isEmpty() ? "인증된 사용자" : String.join(", ", subjectNames);

        return Stream.of(new EntitlementDto(
                policy.getId(),
                finalSubjectName,
                "역할/그룹",
                resourceName,
                actionNames,
                List.of(rootNode.getConditionDescription())
        ));
    }

    private Set<String> getAuthoritiesForSubject(Long subjectId, String subjectType) {
        SubjectAuthorityResolver resolver = authorityResolvers.stream()
                .filter(r -> r.supports(subjectType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported subject type: " + subjectType));

        return resolver.resolveAuthorities(subjectId).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private boolean isPolicySatisfiedBy(Policy policy, Set<String> subjectAuthorities) {
        ExpressionNode rootNode = policyTranslator.parsePolicy(policy);
        Set<String> requiredAuthorities = rootNode.getRequiredAuthorities();
        boolean authsMet = subjectAuthorities.containsAll(requiredAuthorities);
        boolean authStateMet = !rootNode.requiresAuthentication() || subjectAuthorities.stream().anyMatch(a -> !a.equals("ROLE_ANONYMOUS"));
        return authsMet && authStateMet;
    }

    private String getFriendlySubjectNameFromAuthority(String authority) {
        if (authority.startsWith("ROLE_")) {
            return roleRepository.findByRoleName(authority.substring(5))
                    .map(Role::getRoleDesc)
                    .orElse(authority);
        }
        if (authority.startsWith("GROUP_")) {
            try {
                Long groupId = Long.parseLong(authority.substring(6));
                return groupRepository.findById(groupId)
                        .map(Group::getName)
                        .orElse(authority);
            } catch (NumberFormatException e) {
                return authority;
            }
        }
        return authority;
    }

    private String getFriendlyPermissionName(String permissionName) {
        return permissionRepository.findByName(permissionName)
                .map(p -> p.getDescription())
                .orElse(permissionName);
    }
}