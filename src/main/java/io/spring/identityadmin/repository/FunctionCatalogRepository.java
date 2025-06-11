package io.spring.identityadmin.repository;

import io.spring.identityadmin.domain.entity.FunctionCatalog;
import io.spring.identityadmin.domain.entity.ManagedResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FunctionCatalogRepository extends JpaRepository<FunctionCatalog, Long> {

    Optional<FunctionCatalog> findByManagedResource(ManagedResource managedResource);

    /**
     * [신규 및 오류 수정] '미확인 기능 등록' 화면을 위해 UNCONFIRMED 상태의 기능과 관련 리소스를 함께 조회합니다.
     */
    @Query("SELECT fc FROM FunctionCatalog fc " +
            "JOIN FETCH fc.managedResource " +
            "WHERE fc.status = io.spring.identityadmin.domain.entity.FunctionCatalog.CatalogStatus.UNCONFIRMED " +
            "ORDER BY fc.id ASC")
    List<FunctionCatalog> findUnconfirmedFunctionsWithDetails();

    /**
     * [신규 및 오류 수정] '기능 카탈로그 관리' 화면을 위해 UNCONFIRMED 상태가 아닌 모든 기능과 관련 리소스, 그룹을 함께 조회합니다.
     */
    @Query("SELECT fc FROM FunctionCatalog fc " +
            "JOIN FETCH fc.managedResource " +
            "LEFT JOIN FETCH fc.functionGroup " +
            "WHERE fc.status <> io.spring.identityadmin.domain.entity.catalog.FunctionCatalog.CatalogStatus.UNCONFIRMED " +
            "ORDER BY fc.functionGroup.name, fc.friendlyName ASC")
    List<FunctionCatalog> findAllManageableWithDetails();

    /**
     * [신규] '권한 정의' 화면을 위해 ACTIVE 상태인 모든 기능을 조회합니다.
     */
    @Query("SELECT fc FROM FunctionCatalog fc " +
            "WHERE fc.status = io.spring.identityadmin.domain.entity.catalog.FunctionCatalog.CatalogStatus.ACTIVE")
    List<FunctionCatalog> findAllActiveFunctions();
}
