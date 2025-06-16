package io.spring.identityadmin.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.spring.identityadmin.domain.dto.ResourceSearchCriteria;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.domain.entity.QManagedResource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * [수정됨]
 * 사유: ResourceSearchCriteria에 추가된 serviceOwner, status 필터를 처리하기 위해
 *      where절에 새로운 BooleanExpression들을 추가합니다.
 */
@Repository
@RequiredArgsConstructor
public class ManagedResourceRepositoryCustomImpl implements ManagedResourceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ManagedResource> findByCriteria(ResourceSearchCriteria search, Pageable pageable) {
        QManagedResource resource = QManagedResource.managedResource;

        List<ManagedResource> content = queryFactory
                .selectFrom(resource)
                .where(
                        keywordContains(resource, search.getKeyword()),
                        serviceOwnerEq(resource, search.getServiceOwner()),
                        statusEq(resource, search.getStatus())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(resource.createdAt.desc())
                .fetch();

        Long total = queryFactory
                .select(resource.count())
                .from(resource)
                .where(
                        keywordContains(resource, search.getKeyword()),
                        serviceOwnerEq(resource, search.getServiceOwner()),
                        statusEq(resource, search.getStatus())
                )
                .fetchOne();
        return null;
    }

    private BooleanExpression keywordContains(QManagedResource resource, String keyword) {
        if (!StringUtils.hasText(keyword)) return null;
        return resource.friendlyName.containsIgnoreCase(keyword)
                .or(resource.resourceIdentifier.containsIgnoreCase(keyword))
                .or(resource.description.containsIgnoreCase(keyword));
    }

    private BooleanExpression serviceOwnerEq(QManagedResource resource, String serviceOwner) {
        return StringUtils.hasText(serviceOwner) ? resource.serviceOwner.eq(serviceOwner) : null;
    }

    private BooleanExpression statusEq(QManagedResource resource, ManagedResource.Status status) {
        return status != null ? resource.status.eq(status) : null;
    }
}