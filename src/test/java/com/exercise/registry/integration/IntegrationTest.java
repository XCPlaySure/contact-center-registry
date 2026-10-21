package com.exercise.registry.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exercise.registry.tenant.Tenant;

public class IntegrationTest {
    private final Tenant tenant = new Tenant("Acme", "acme");

    @Test
    void defaultConstructorLeavesFieldsUnset() {
        Integration integration = new Integration();

        assertNull(integration.getId());
        assertNull(integration.getTenant());
        assertNull(integration.getDisplayName());
        assertNull(integration.getProvider());
        assertNull(integration.getCredentialRef());
        assertNull(integration.getStatus());
        assertNull(integration.getCreatedAt());
        assertNull(integration.getUpdatedAt());
    }

    @Test
    void constructorAssignsAllFieldsActive() {
        Integration integration = new Integration(tenant, "Primary Voice", IntegrationProvider.TWILIO, "vault://x");

        assertNotNull(integration.getId());
        assertSame(tenant, integration.getTenant());
        assertEquals("Primary Voice", integration.getDisplayName());
        assertEquals(IntegrationProvider.TWILIO, integration.getProvider());
        assertEquals("vault://x", integration.getCredentialRef());
        assertEquals(IntegrationStatus.ACTIVE, integration.getStatus());
        assertNotNull(integration.getCreatedAt());
        assertEquals(integration.getCreatedAt(), integration.getUpdatedAt());
        assertEquals(0, integration.getVersion());
    }

    @Test
    void updateMutatesFieldsAndBumpsUpdatedAt() {
        Integration integration = new Integration(tenant, "Primary Voice", IntegrationProvider.TWILIO, "vault://x");

        integration.update("Secondary Voice", IntegrationProvider.AMAZON_CONNECT, "vault://y",
            IntegrationStatus.DISABLED);

        assertEquals("Secondary Voice", integration.getDisplayName());
        assertEquals(IntegrationProvider.AMAZON_CONNECT, integration.getProvider());
        assertEquals("vault://y", integration.getCredentialRef());
        assertEquals(IntegrationStatus.DISABLED, integration.getStatus());
        assertTrue(!integration.getUpdatedAt().isBefore(integration.getCreatedAt()));
        assertEquals(0, integration.getVersion());
    }

    @Test
    void responseMapsAllFields() {
        Integration integration = new Integration(tenant, "Primary Voice", IntegrationProvider.FIVE9, "vault://x");

        IntegrationResponse response = IntegrationResponse.from(integration);

        assertEquals(integration.getId(), response.id());
        assertEquals(tenant.getId(), response.tenantId());
        assertEquals("Primary Voice", response.displayName());
        assertEquals(IntegrationProvider.FIVE9, response.provider());
        assertEquals("vault://x", response.credentialRef());
        assertEquals(IntegrationStatus.ACTIVE, response.status());
        assertEquals(integration.getCreatedAt(), response.createdAt());
        assertEquals(integration.getUpdatedAt(), response.updatedAt());
        assertEquals(integration.getVersion(), response.version());
    }
}
