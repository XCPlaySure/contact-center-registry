package com.exercise.registry.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class TenantTest {

    @Test
    void defaultConstructorLeavesFieldsUnset() {
        Tenant tenant = new Tenant();

        assertNull(tenant.getId());
        assertNull(tenant.getName());
        assertNull(tenant.getSlug());
        assertNull(tenant.getCreatedAt());
    }

    @Test
    void constructorAssignsIdNameSlugAndTimestamp() {
        Tenant tenant = new Tenant("Acme", "acme");

        assertNotNull(tenant.getId());
        assertEquals("Acme", tenant.getName());
        assertEquals("acme", tenant.getSlug());
        assertNotNull(tenant.getCreatedAt());
    }

    @Test
    void responseMapsAllFields() {
        Tenant tenant = new Tenant("Acme", "acme");

        TenantResponse response = TenantResponse.from(tenant);

        assertEquals(tenant.getId(), response.id());
        assertEquals("Acme", response.name());
        assertEquals("acme", response.slug());
        assertEquals(tenant.getCreatedAt(), response.createdAt());
    }
}
