package io.spring.identityadmin.iamw;

import io.spring.identityadmin.admin.repository.GroupRepository;
import io.spring.identityadmin.admin.repository.PolicyRepository;
import io.spring.identityadmin.admin.repository.RoleRepository;
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
    private final RoleRepository roleRepository;
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
        Set<String> subjectAuthorities = getAuthoritiesForSubject(subjectId, subjectType);
        if (subjectAuthorities.isEmpty()) {
            return Collections.emptyList();
        }

        List<Policy> allPolicies = policyRepository.findAllWithDetails();

        return allPolicies.stream()
                .filter(policy -> policy.getEffect() == Policy.Effect.ALLOW)
                .filter(policy -> isPolicySatisfiedBy(policy, subjectAuthorities))
                .flatMap(policy -> translatePolicyToEntitlements(policy, subjectId, subjectType)) // 주체 정보 전달
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
     * [완전 재작성] Policy 객체를 분석하여 완전한 정보를 담은 EntitlementDto로 번역합니다.
     * 더 이상 "N/A", "Access Granted"와 같은 placeholder를 사용하지 않습니다.
     *
     * @param policy 번역할 정책 객체
     * @param subjectId 현재 조회중인 주체의 ID
     * @param subjectType 현재 조회중인 주체의 타입
     * @return 정책의 각 Target에 대한 EntitlementDto 스트림
     */
    private Stream<EntitlementDto> translatePolicyToEntitlements(Policy policy, Long subjectId, String subjectType) {

        // 1. 정책의 규칙(Rule)들을 구조화된 노드 트리로 변환
        List<ExpressionNode> ruleNodes = policy.getRules().stream()
                .map(rule -> {
                    List<ExpressionNode> conditionNodes = rule.getConditions().stream()
                            .map(policyTranslator::parseCondition)
                            .collect(Collectors.toList());
                    return new LogicalNode("AND", conditionNodes);
                })
                .collect(Collectors.toList());

        ExpressionNode finalRuleNode = new LogicalNode("OR", ruleNodes);

        // 2. 노드 트리에서 권한(주체/행위)과 조건 설명을 추출
        Set<String> authorities = finalRuleNode.getRequiredAuthorities();
        String conditionDescription = finalRuleNode.getConditionDescription();

        // 3. 주체(Subject)의 사용자 친화적 이름 조회
        String subjectName = getFriendlySubjectName(subjectId, subjectType);
        String subjectTypeStr = "GROUP".equalsIgnoreCase(subjectType) ? "그룹" : "사용자";

        // 4. 행위(Action)의 사용자 친화적 이름 목록 조회
        List<String> actionNames = authorities.stream()
                .filter(auth -> !auth.startsWith("ROLE_"))
                .map(this::getFriendlyPermissionName) // 기술적 권한명을 친화적 이름으로 변환
                .collect(Collectors.toList());

        // 5. 정책이 적용되는 각 타겟(리소스)에 대해 EntitlementDto를 생성
        return policy.getTargets().stream().map(target -> {
            String resourceName = managedResourceRepository.findByResourceIdentifier(target.getTargetIdentifier())
                    .map(m -> m.getFriendlyName())
                    .orElse(target.getTargetIdentifier());

            return new EntitlementDto(
                    policy.getId(),
                    subjectName,
                    subjectTypeStr,
                    resourceName,
                    actionNames,
                    List.of(conditionDescription)
            );
        });
    }

    /**
     * 주체 ID와 타입을 받아 사용자 친화적인 이름을 반환하는 헬퍼 메서드
     */
    private String getFriendlySubjectName(Long subjectId, String subjectType) {
        if ("USER".equalsIgnoreCase(subjectType)) {
            return userRepository.findById(subjectId).map(Users::getName).orElse("알 수 없는 사용자");
        }
        if ("GROUP".equalsIgnoreCase(subjectType)) {
            return groupRepository.findById(subjectId).map(Group::getName).orElse("알 수 없는 그룹");
        }
        return "알 수 없음";
    }

    /**
     * 기술적인 Permission 이름을 받아 사용자 친화적인 이름을 반환하는 헬퍼 메서드
     * 실제 시스템에서는 Permission 엔티티의 description 필드를 활용해야 합니다.
     */
    private String getFriendlyPermissionName(String permissionName) {
        // 예시: "DOCUMENT_READ" -> "문서 읽기"
        // 실제로는 DB의 Permission 테이블을 조회하여 description을 가져오는 로직 필요
        return permissionName;
    }
}
