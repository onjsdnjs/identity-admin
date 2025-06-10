package io.spring.identityadmin.admin.service;

import io.spring.identityadmin.domain.dto.BusinessActionDto;
import io.spring.identityadmin.domain.dto.GroupMetadataDto;
import io.spring.identityadmin.domain.dto.RoleMetadataDto;
import io.spring.identityadmin.domain.dto.UserMetadataDto;
import io.spring.identityadmin.entity.*;
import java.util.List;
import java.util.Map;

public interface BusinessMetadataService {

    List<BusinessResource> getAllBusinessResources();

    List<BusinessActionDto> getAllBusinessActions();

    List<BusinessActionDto> getActionsForResource(Long businessResourceId);

    List<ConditionTemplate> getAllConditionTemplates();

    List<UserMetadataDto> getAllUsersForPolicy();

    List<GroupMetadataDto> getAllGroupsForPolicy();

    Map<String, Object> getAllUsersAndGroups();
    /**
     * [추가] 워크벤치의 '역할 기준 탐색'을 위한 모든 역할 목록을 조회합니다.
     * @return Role 엔티티 리스트
     */
    List<RoleMetadataDto> getAllRoles();
}
