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
                        resourceTypeEq(resource, search.getResourceType())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(resource.id.desc())
                .fetch();

        Long total = queryFactory
                .select(resource.count())
                .from(resource)
                .where(
                        keywordContains(resource, search.getKeyword()),
                        resourceTypeEq(resource, search.getResourceType()),
                        isDefinedEq(resource, search.getDefined()) // [신규] isDefined 조건 추가
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, (total != null) ? total : 0);
    }

    private BooleanExpression keywordContains(QManagedResource resource, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return resource.friendlyName.containsIgnoreCase(keyword)
                .or(resource.resourceIdentifier.containsIgnoreCase(keyword))
                .or(resource.serviceOwner.containsIgnoreCase(keyword));
    }

    private BooleanExpression resourceTypeEq(QManagedResource resource, ManagedResource.ResourceType resourceType) {
        return resourceType != null ? resource.resourceType.eq(resourceType) : null;
    }

    private BooleanExpression isDefinedEq(QManagedResource resource, Boolean isDefined) {
        return isDefined != null ? resource.isDefined.eq(isDefined) : null;
    }
}
