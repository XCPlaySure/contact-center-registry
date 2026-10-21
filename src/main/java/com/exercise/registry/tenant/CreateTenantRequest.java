package com.exercise.registry.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
    @NotBlank @Size (max = 120) String name,
    @NotBlank @Size (max = 60) @Pattern (regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") 
    String slug
) { }
