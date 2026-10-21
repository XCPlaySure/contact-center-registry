package com.exercise.registry.integration;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exercise.registry.common.DuplicateResourceException;
import com.exercise.registry.common.ResourceNotFoundException;
import com.exercise.registry.tenant.Tenant;
import com.exercise.registry.tenant.TenantRepository;

@Service 
public class IntegrationService {
    private final IntegrationRepository integrationRepository;
    private final TenantRepository tenantRepository;

    public IntegrationService(IntegrationRepository integrationRepository, 
        TenantRepository tenantRepository) {
        this.integrationRepository = integrationRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional 
    public IntegrationResponse create(UUID tenantId, CreateIntegrationRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));
    
        String displayName = request.displayName().trim();
        if (integrationRepository.existsByTenant_IdAndDisplayName(tenantId, displayName)) {
            throw new DuplicateResourceException("Integration display name already exists for this tenant");
        }

        Integration saved = integrationRepository.save(new Integration(
            tenant,
            displayName,
            request.provider(),
            request.credentialRef().trim()));
        
        return IntegrationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<IntegrationResponse> list(UUID tenantId, IntegrationStatus status,
        Pageable pageable) {
        requireTenant(tenantId);
        Page<Integration> page = status == null
            ? integrationRepository.findAllByTenant_Id(tenantId, pageable)
            : integrationRepository.findAllByTenant_IdAndStatus(tenantId, status, pageable);
        
        return page.map(IntegrationResponse::from);
    }

    @Transactional(readOnly = true)
    public IntegrationResponse get(UUID tenantId, UUID integrationId) {
        return IntegrationResponse.from(findIntegration(tenantId, integrationId));
    }

    @Transactional
    public IntegrationResponse update(UUID tenantId, UUID integrationId,
        UpdateIntegrationRequest request) {
        Integration integration = findIntegration(tenantId, integrationId);
        String displayName = request.displayName().trim();

        boolean nameChanged = !integration.getDisplayName().equals(displayName);
        if (nameChanged && integrationRepository.existsByTenant_IdAndDisplayName(tenantId, displayName)) {
            throw new DuplicateResourceException("Integration display name already exists for this tenant");
        }

        integration.update(displayName, request.provider(), request.credentialRef().trim(), request.status());
        // Flush so @Version and database constraint results are current before building the response.
        integrationRepository.flush();
        return IntegrationResponse.from(integration);
    }

    @Transactional 
    public void delete(UUID tenantId, UUID integrationId) {
        Integration integration = findIntegration(tenantId, integrationId);
        integrationRepository.delete(integration);
    }
    
    private Integration findIntegration(UUID tenantId, UUID integrationId) {
        requireTenant(tenantId);
        return integrationRepository.findByIdAndTenant_Id(integrationId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Integration not found for this tenant"));
    }

    private void requireTenant(UUID tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("Tenant not found with id: " + tenantId);
        }
    }
}
