package com.exercise.registry.integration;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController 
@RequestMapping("/api/v1/tenants/{tenantId}/integrations")
public class IntegrationController {
    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @PostMapping 
    @ResponseStatus(HttpStatus.CREATED)
    public IntegrationResponse create(
        @PathVariable UUID tenantId,
        @Valid @RequestBody CreateIntegrationRequest request
    ) {
        return integrationService.create(tenantId, request);
    }

    @GetMapping 
    public Page<IntegrationResponse> list(
        @PathVariable UUID tenantId,
        @RequestParam(required = false) IntegrationStatus status,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        return integrationService.list(tenantId, status, pageable);
    }

    @GetMapping("/{integrationId}")
    public IntegrationResponse get(
        @PathVariable UUID tenantId,
        @PathVariable UUID integrationId
    ) {
        return integrationService.get(tenantId, integrationId);
    }

    @PutMapping("/{integrationId}")
    public IntegrationResponse update(
        @PathVariable UUID tenantId,
        @PathVariable UUID integrationId,
        @Valid @RequestBody UpdateIntegrationRequest request
    ) {
        return integrationService.update(tenantId, integrationId, request);
    }

    @DeleteMapping("/{integrationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable UUID tenantId,
        @PathVariable UUID integrationId
    ) {
        integrationService.delete(tenantId, integrationId);
    }
}
