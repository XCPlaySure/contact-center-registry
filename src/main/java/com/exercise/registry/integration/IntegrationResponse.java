package com.exercise.registry.integration;

import java.time.Instant;
import java.util.UUID;

public record IntegrationResponse(
    UUID id,
    UUID tenantId,
    String displayName,
    IntegrationProvider provider,
    String credentialRef,
    IntegrationStatus status,
    Instant createdAt,
    Instant updatedAt,
    long version
) {
    public static IntegrationResponse from(Integration integration) {
        return new IntegrationResponse(
            integration.getId(),
            integration.getTenant().getId(),
            integration.getDisplayName(),
            integration.getProvider(),
            integration.getCredentialRef(),
            integration.getStatus(),
            integration.getCreatedAt(),
            integration.getUpdatedAt(),
            integration.getVersion()
        );
    }
}
