package com.exercise.registry.integration;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationRepository extends JpaRepository<Integration, UUID> {
    Page<Integration> findAllByTenant_Id(UUID tenantId, Pageable pageable);

    Page<Integration> findAllByTenant_IdAndStatus(
        UUID tenantId, IntegrationStatus status, Pageable pageable
    );

    Optional<Integration> findByIdAndTenant_Id(UUID id, UUID tenantId);

    boolean existsByTenant_IdAndDisplayName(UUID tenantId, String displayName);
}
