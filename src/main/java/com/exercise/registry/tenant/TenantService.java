package com.exercise.registry.tenant;

import com.exercise.registry.common.DuplicateResourceException;
import com.exercise.registry.common.ResourceNotFoundException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service 
public class TenantService {
    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional 
    public TenantResponse create(CreateTenantRequest request) {
        String slug = request.slug().trim().toLowerCase(Locale.ROOT);
        if (tenantRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Tenant slug already exists: " + slug);
        }

        Tenant savedTenant = tenantRepository.save(new Tenant(request.name().trim(), slug));
        return TenantResponse.from(savedTenant);
    }

    @Transactional(readOnly = true)
    public TenantResponse get(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));
        return TenantResponse.from(tenant);
    }
}
