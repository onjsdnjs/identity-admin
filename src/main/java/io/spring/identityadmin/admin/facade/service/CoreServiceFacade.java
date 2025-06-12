package io.spring.identityadmin.admin.facade.service;

import io.spring.identityadmin.domain.dto.*;
import io.spring.identityadmin.domain.entity.*;
import io.spring.identityadmin.domain.entity.policy.Policy;

import java.util.List;
import java.util.Set;

/**
 * <strong>[명칭 수정 및 역할 재정의]</strong><br>
 * 핵심 도메인 서비스 퍼사드 (Core Service Facade).<br>
 * 새로운 워크플로우/오케스트레이션 계층과 기존의 세분화된 도메인 서비스 계층 사이의 상호작용을 단순화하는 퍼사드(Facade) 역할을 합니다.
 * 상위 계층은 이 인터페이스를 통해 하위 도메인 서비스들의 복잡성을 알 필요 없이 필요한 기능만을 호출합니다.
 */
public interface CoreServiceFacade {

    /**
     * 퍼사드를 통해 최종적으로 정책을 생성합니다.
     * 내부적으로 DefaultPolicyService를 호출하여 실제 저장을 위임합니다.
     * @param policy 생성할 Policy 엔티티
     * @return 저장된 Policy 엔티티
     */
    Policy createPolicy(Policy policy);

    /**
     * 퍼사드를 통해 사용자를 생성합니다.
     * 내부적으로 UserAdminService를 호출합니다.
     * @param userDto 생성할 사용자의 데이터
     * @return 생성된 Users 엔티티
     */
    Users createUser(UserDto userDto);

    /**
     * 퍼사드를 통해 그룹을 조회합니다.
     * @param groupId 조회할 그룹의 ID
     * @return 조회된 Group 엔티티
     */
    Group getGroupById(Long groupId);

    /**
     * 퍼사드를 통해 특정 Permission ID 목록에 해당하는 Permission 엔티티 목록을 조회합니다.
     * @param permissionIds 조회할 Permission ID 목록
     * @return 조회된 Permission 엔티티 목록
     */
    List<Permission> findPermissionsByIds(Set<Long> permissionIds);

    /**
     * 퍼사드를 통해 특정 사용자 ID에 해당하는 사용자 이름을 조회합니다.
     * @param userId 조회할 사용자의 ID
     * @return 사용자 이름 (Username)
     */
    String findUsernameById(Long userId);
}

