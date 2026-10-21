package com.exercise.registry.integration;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exercise.registry.common.ResourceNotFoundException;
import com.exercise.registry.tenant.TenantRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IntegrationServiceTest {
    @Mock 
    private IntegrationRepository integrationRepository;

    @Mock 
    private TenantRepository tenantRepository;

    @InjectMocks 
    private IntegrationService integrationService;

    @Test 
    void createRejectsUnknownTenantBeforeSaving() {
        UUID tenantId = UUID.randomUUID();
        CreateIntegrationRequest request = new CreateIntegrationRequest(
            "Primary Voice", IntegrationProvider.TWILIO, "vault://demo/twilio"
        );
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            integrationService.create(tenantId, request);
        });
        verifyNoInteractions(integrationRepository);
    }
}
