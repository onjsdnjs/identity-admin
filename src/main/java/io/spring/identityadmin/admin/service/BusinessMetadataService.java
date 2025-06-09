package io.spring.identityadmin.admin.service;

import io.spring.identityadmin.entity.*;

import java.util.List;

/**
 * UI가 정책을 설정하는 데 필요한 비즈니스 메타데이터를 제공하는 서비스
 */
public interface BusinessMetadataService {

    /** @return 정책 설정에 사용할 수 있는 모든 비즈니스 자원 목록 */
    List<BusinessResource> getAllBusinessResources();

    List<BusinessAction> getAllBusinessActions();

    /** @return 특정 자원에 대해 수행할 수 있는 모든 비즈니스 행위 목록 */
    List<BusinessAction> getActionsForResource(Long businessResourceId);

    /** @return 정책에 추가할 수 있는 모든 조건 템플릿 목록 */
    List<ConditionTemplate> getAllConditionTemplates();

    /** @return 정책 주체로 설정할 수 있는 모든 사용자 목록 */
    List<Users> getAllUsersForPolicy();

    /** @return 정책 주체로 설정할 수 있는 모든 그룹 목록 */
    List<Group> getAllGroupsForPolicy();
}
