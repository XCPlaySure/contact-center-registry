package com.exercise.registry.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.exercise.registry.common.DuplicateResourceException;
import com.exercise.registry.common.ResourceNotFoundException;
import com.exercise.registry.tenant.Tenant;
import com.exercise.registry.tenant.TenantRepository;

@ExtendWith(MockitoExtension.class)
public class IntegrationServiceTest {
    @Mock
    private IntegrationRepository integrationRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private IntegrationService integrationService;

    private final Tenant tenant = new Tenant("Acme", "acme");

    private CreateIntegrationRequest request() {
        return new CreateIntegrationRequest("Primary Voice", IntegrationProvider.TWILIO, "vault://demo/twilio");
    }

    @Test
    void createRejectsUnknownTenantBeforeSaving() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            integrationService.create(tenantId, request());
        });
        verifyNoInteractions(integrationRepository);
    }

    @Test
    void createSavesActiveIntegrationForExistingTenant() {
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(integrationRepository.existsByTenant_IdAndDisplayName(tenant.getId(), "Primary Voice"))
            .thenReturn(false);
        when(integrationRepository.save(any(Integration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IntegrationResponse response = integrationService.create(tenant.getId(), request());

        assertNotNull(response.id());
        assertEquals(tenant.getId(), response.tenantId());
        assertEquals("Primary Voice", response.displayName());
        assertEquals(IntegrationProvider.TWILIO, response.provider());
        assertEquals("vault://demo/twilio", response.credentialRef());
        assertEquals(IntegrationStatus.ACTIVE, response.status());
        assertEquals(0, response.version());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }

    @Test
    void createRejectsDuplicateDisplayNameForTenant() {
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(integrationRepository.existsByTenant_IdAndDisplayName(tenant.getId(), "Primary Voice"))
            .thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            integrationService.create(tenant.getId(), request());
        });
        verify(integrationRepository, never()).save(any(Integration.class));
    }

    @Test
    void listReturnsAllIntegrationsWhenStatusMissing() {
        UUID tenantId = tenant.getId();
        when(tenantRepository.existsById(tenantId)).thenReturn(true);
        Integration integration = new Integration(tenant, "Primary Voice", IntegrationProvider.TWILIO, "vault://x");
        when(integrationRepository.findAllByTenant_Id(eq(tenantId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(integration)));

        Page<IntegrationResponse> page = integrationService.list(tenantId, null, Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
        assertEquals("Primary Voice", page.getContent().get(0).displayName());
    }

    @Test
    void listFiltersByStatusWhenProvided() {
        UUID tenantId = tenant.getId();
        when(tenantRepository.existsById(tenantId)).thenReturn(true);
        Integration integration = new Integration(tenant, "Primary Voice", IntegrationProvider.TWILIO, "vault://x");
        integration.update("Primary Voice", IntegrationProvider.TWILIO, "vault://x", IntegrationStatus.DISABLED);
        when(integrationRepository.findAllByTenant_IdAndStatus(eq(tenantId), eq(IntegrationStatus.DISABLED),
            any(Pageable.class))).thenReturn(new PageImpl<>(List.of(integration)));

        Page<IntegrationResponse> page = integrationService.list(tenantId, IntegrationStatus.DISABLED, Pageable.unpaged());

        assertEquals(IntegrationStatus.DISABLED, page.getContent().get(0).status());
    }

    @Test
    void listRejectsUnknownTenant() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.existsById(tenantId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> {
            integrationService.list(tenantId, null, Pageable.unpaged());
        });
    }

    @Test
    void getReturnsIntegrationForTenant() {
        UUID tenantId = tenant.getId();
        Integration integration = new Integration(tenant, "Primary Voice", IntegrationProvider.TWILIO, "vault://x");
        when(tenantRepository.existsById(tenantId)).thenReturn(true);
        when(integrationRepository.findByIdAndTenant_Id(integration.getId(), tenantId))
            .thenReturn(Optional.of(integration));

        IntegrationResponse response = integrationService.get(tenantId, integration.getId());

        assertEquals(integration.getId(), response.id());
        assertEquals("Primary Voice", response.displayName());
    }

    @Test
    void getRejectsUnknownTenant() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.existsById(tenantId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> {
            integrationService.get(tenantId, UUID.randomUUID());
        });
    }

    @Test
    void getRejectsMissingIntegration() {
        UUID tenantId = tenant.getId();
        when(tenantRepository.existsById(tenantId)).thenReturn(true);
        when(integrationRepository.findByIdAndTenant_Id(any(UUID.class), eq(tenantId))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            integrationService.get(tenantId, UUID.randomUUID());
        });
    }

    @Test
    void updateChangesFieldsAndFlushes() {
        UUID tenantId = tenant.getId();
        Integration integration = new Integration(tenant, "Primary Voice", IntegrationProvider.TWILIO, "vault://x");
        when(tenantRepository.existsById(tenantId)).thenReturn(true);
        when(integrationRepository.findByIdAndTenant_Id(integration.getId(), tenantId))
            .thenReturn(Optional.of(integration));
        when(integrationRepository.existsByTenant_IdAndDisplayName(tenantId, "Secondary Voice")).thenReturn(false);

        UpdateIntegrationRequest update = new UpdateIntegrationRequest("Secondary Voice", IntegrationProvider.FIVE9,
            "vault://demo/five9", IntegrationStatus.DISABLED);
        IntegrationResponse response = integrationService.update(tenantId, integration.getId(), update);

        assertEquals("Secondary Voice", response.displayName());
        assertEquals(IntegrationProvider.FIVE9, response.provider());
        assertEquals("vault://demo/five9", response.credentialRef());
        assertEquals(IntegrationStatus.DISABLED, response.status());
        assertEquals(0, response.version());
        verify(integrationRepository).flush();
    }

    @Test
    void updateAcceptsUnchangedDisplayNameWithoutDuplicateCheck() {
        UUID tenantId = tenant.getId();
        Integration integration = new Integration(tenant, "Primary Voice", IntegrationProvider.TWILIO, "vault://x");
        when(tenantRepository.existsById(tenantId)).thenReturn(true);
        when(integrationRepository.findByIdAndTenant_Id(integration.getId(), tenantId))
            .thenReturn(Optional.of(integration));

        UpdateIntegrationRequest update = new UpdateIntegrationRequest("Primary Voice", IntegrationProvider.TWILIO,
            "vault://x", IntegrationStatus.DISABLED);
        IntegrationResponse response = integrationService.update(tenantId, integration.getId(), update);

        assertEquals(IntegrationStatus.DISABLED, response.status());
        verify(integrationRepository, never())
            .existsByTenant_IdAndDisplayName(any(UUID.class), any(String.class));
    }

    @Test
    void updateRejectsConflictingNewDisplayName() {
        UUID tenantId = tenant.getId();
        Integration integration = new Integration(tenant, "Primary Voice", IntegrationProvider.TWILIO, "vault://x");
        when(tenantRepository.existsById(tenantId)).thenReturn(true);
        when(integrationRepository.findByIdAndTenant_Id(integration.getId(), tenantId))
            .thenReturn(Optional.of(integration));
        when(integrationRepository.existsByTenant_IdAndDisplayName(tenantId, "Secondary Voice")).thenReturn(true);

        UpdateIntegrationRequest update = new UpdateIntegrationRequest("Secondary Voice", IntegrationProvider.FIVE9,
            "vault://y", IntegrationStatus.ACTIVE);

        assertThrows(DuplicateResourceException.class, () -> {
            integrationService.update(tenantId, integration.getId(), update);
        });
    }

    @Test
    void updateRejectsUnknownTenant() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.existsById(tenantId)).thenReturn(false);

        UpdateIntegrationRequest update = new UpdateIntegrationRequest("Voice", IntegrationProvider.TWILIO, "vault://x",
            IntegrationStatus.ACTIVE);

        assertThrows(ResourceNotFoundException.class, () -> {
            integrationService.update(tenantId, UUID.randomUUID(), update);
        });
    }

    @Test
    void updateRejectsMissingIntegration() {
        UUID tenantId = tenant.getId();
        when(tenantRepository.existsById(tenantId)).thenReturn(true);
        when(integrationRepository.findByIdAndTenant_Id(any(UUID.class), eq(tenantId))).thenReturn(Optional.empty());

        UpdateIntegrationRequest update = new UpdateIntegrationRequest("Voice", IntegrationProvider.TWILIO, "vault://x",
            IntegrationStatus.ACTIVE);

        assertThrows(ResourceNotFoundException.class, () -> {
            integrationService.update(tenantId, UUID.randomUUID(), update);
        });
    }

    @Test
    void deleteRemovesIntegrationForTenant() {
        UUID tenantId = tenant.getId();
        Integration integration = new Integration(tenant, "Primary Voice", IntegrationProvider.TWILIO, "vault://x");
        when(tenantRepository.existsById(tenantId)).thenReturn(true);
        when(integrationRepository.findByIdAndTenant_Id(integration.getId(), tenantId))
            .thenReturn(Optional.of(integration));

        integrationService.delete(tenantId, integration.getId());

        verify(integrationRepository).delete(integration);
    }

    @Test
    void deleteRejectsUnknownTenant() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.existsById(tenantId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> {
            integrationService.delete(tenantId, UUID.randomUUID());
        });
    }

    @Test
    void deleteRejectsMissingIntegration() {
        UUID tenantId = tenant.getId();
        when(tenantRepository.existsById(tenantId)).thenReturn(true);
        when(integrationRepository.findByIdAndTenant_Id(any(UUID.class), eq(tenantId))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            integrationService.delete(tenantId, UUID.randomUUID());
        });
    }

    @Test
    void createTrimsDisplayNameAndCredentialRef() {
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(integrationRepository.existsByTenant_IdAndDisplayName(tenant.getId(), "Primary Voice"))
            .thenReturn(false);
        when(integrationRepository.save(any(Integration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateIntegrationRequest padded = new CreateIntegrationRequest("  Primary Voice  ", IntegrationProvider.GENESYS_CLOUD,
            "  vault://demo  ");
        IntegrationResponse response = integrationService.create(tenant.getId(), padded);

        assertEquals("Primary Voice", response.displayName());
        assertEquals("vault://demo", response.credentialRef());
        assertNotNull(response.id());
    }
}
