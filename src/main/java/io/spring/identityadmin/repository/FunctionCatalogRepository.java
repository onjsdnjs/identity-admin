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

    @Query("SELECT fc FROM FunctionCatalog fc JOIN FETCH fc.managedResource WHERE fc.status = 'UNCONFIRMED'")
    List<FunctionCatalog> findUnconfirmedFunctions();
}
