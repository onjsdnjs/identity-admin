package io.spring.identityadmin.admin.service;

import io.spring.identityadmin.entity.*;
import java.util.List;

public interface BusinessMetadataService {

    List<BusinessResource> getAllBusinessResources();

    List<BusinessAction> getAllBusinessActions();

    List<BusinessAction> getActionsForResource(Long businessResourceId);

    List<ConditionTemplate> getAllConditionTemplates();

    List<Users> getAllUsersForPolicy();

    List<Group> getAllGroupsForPolicy();

    /**
     * [추가] 워크벤치의 '역할 기준 탐색'을 위한 모든 역할 목록을 조회합니다.
     * @return Role 엔티티 리스트
     */
    List<Role> getAllRoles();
}
