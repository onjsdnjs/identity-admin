package io.spring.identityadmin.iamw;

import io.spring.identityadmin.entity.ManagedResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagedResourceRepository extends JpaRepository<ManagedResource, Long>, ManagedResourceRepositoryCustom {

    Optional<ManagedResource> findByResourceIdentifier(String resourceIdentifier);
}