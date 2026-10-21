package com.exercise.registry.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exercise.registry.common.DuplicateResourceException;
import com.exercise.registry.common.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
public class TenantServiceTest {
    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void createPersistsTrimmedNameAndSlug() {
        when(tenantRepository.existsBySlug("acme")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantResponse response = tenantService.create(new CreateTenantRequest("  Acme Support  ", " acme "));

        assertNotNull(response.id());
        assertEquals("Acme Support", response.name());
        assertEquals("acme", response.slug());
        assertNotNull(response.createdAt());
    }

    @Test
    void createRejectsDuplicateSlugBeforeSaving() {
        when(tenantRepository.existsBySlug("acme")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            tenantService.create(new CreateTenantRequest("Acme", "acme"));
        });
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void getReturnsTenantById() {
        Tenant tenant = new Tenant("Acme", "acme");
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));

        TenantResponse response = tenantService.get(tenant.getId());

        assertEquals(tenant.getId(), response.id());
        assertEquals("Acme", response.name());
        assertEquals("acme", response.slug());
    }

    @Test
    void getThrowsWhenTenantMissing() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tenantService.get(tenantId));
    }
}
