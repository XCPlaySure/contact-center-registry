package com.exercise.registry.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateIntegrationRequest(
    @NotBlank @Size (max = 120) String displayName,
    @NotNull IntegrationProvider provider,
    @NotBlank @Size (max = 255) String credentialRef,
    @NotNull IntegrationStatus status
) { }
