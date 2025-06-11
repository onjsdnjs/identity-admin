package io.spring.identityadmin.repository;

import io.spring.identityadmin.domain.entity.business.BusinessResourceAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * BusinessResource와 BusinessAction 간의 매핑 정보를 담고 있는
 * BusinessResourceAction 엔티티에 대한 데이터 접근 리포지토리.
 */
@Repository
public interface BusinessResourceActionRepository extends JpaRepository<BusinessResourceAction, BusinessResourceAction.BusinessResourceActionId> {
}
