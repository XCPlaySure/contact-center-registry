package com.exercise.registry.tenant;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    // JpaRepository already provides save, findById, findAll, existsById, and delete methods.
    boolean existsBySlug(String slug);
    Optional<Tenant> findBySlug(String slug);
}
