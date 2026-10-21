package com.exercise.registry.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.exercise.registry.common.DuplicateResourceException;
import com.exercise.registry.common.GlobalExceptionHandler;
import com.exercise.registry.common.ResourceNotFoundException;
import com.exercise.registry.tenant.Tenant;

@ExtendWith(MockitoExtension.class)
public class IntegrationControllerTest {
    @Mock
    private IntegrationService integrationService;

    private MockMvc mockMvc;
    private final Tenant tenant = new Tenant("Acme", "acme");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new IntegrationController(integrationService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    private String createBody() {
        return "{\"displayName\": \"Primary Voice\", \"provider\": \"TWILIO\", \"credentialRef\": \"vault://x\"}";
    }

    private Integration integration() {
        return new Integration(tenant, "Primary Voice", IntegrationProvider.TWILIO, "vault://x");
    }

    @Test
    void createReturns201WithIntegrationBody() throws Exception {
        Integration integration = integration();
        when(integrationService.create(eq(tenant.getId()), any(CreateIntegrationRequest.class)))
            .thenReturn(IntegrationResponse.from(integration));

        mockMvc.perform(post("/api/v1/tenants/{tenantId}/integrations", tenant.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.displayName").value("Primary Voice"))
            .andExpect(jsonPath("$.provider").value("TWILIO"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.tenantId").value(tenant.getId().toString()));
    }

    @Test
    void createRejectsUnknownProviderWith400() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/{tenantId}/integrations", tenant.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\": \"X\", \"provider\": \"NOPE\", \"credentialRef\": \"v\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Request body is invalid or contains an unsupported enum value"));
    }

    @Test
    void createRejectsMissingFieldsWith400() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/{tenantId}/integrations", tenant.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.displayName").exists())
            .andExpect(jsonPath("$.details.provider").exists())
            .andExpect(jsonPath("$.details.credentialRef").exists());
    }

    @Test
    void createMapsUnknownTenantTo404() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(integrationService.create(eq(tenantId), any(CreateIntegrationRequest.class)))
            .thenThrow(new ResourceNotFoundException("Tenant not found with id: " + tenantId));

        mockMvc.perform(post("/api/v1/tenants/{tenantId}/integrations", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody()))
            .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsPageSortedByCreatedAtDesc() throws Exception {
        Page<IntegrationResponse> page = new PageImpl<>(List.of(IntegrationResponse.from(integration())),
            PageRequest.of(0, 20), 1);
        when(integrationService.list(eq(tenant.getId()), eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/integrations", tenant.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].displayName").value("Primary Voice"));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(integrationService).list(eq(tenant.getId()), eq(null), captor.capture());
        Pageable pageable = captor.getValue();
        assertEquals(20, pageable.getPageSize());
        assertEquals("createdAt", pageable.getSort().stream()
            .findFirst().map(Sort.Order::getProperty).orElse(null));
        assertEquals(Sort.Direction.DESC, pageable.getSort().stream()
            .findFirst().map(Sort.Order::getDirection).orElse(null));
    }

    @Test
    void listPassesStatusFilterToService() throws Exception {
        when(integrationService.list(eq(tenant.getId()), eq(IntegrationStatus.ACTIVE), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/integrations", tenant.getId())
                .param("status", "ACTIVE"))
            .andExpect(status().isOk());

        verify(integrationService).list(eq(tenant.getId()), eq(IntegrationStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    void listRejectsUnknownStatusWith400() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/{tenantId}/integrations", tenant.getId())
                .param("status", "NOPE"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.status").value("Unsupported value"));
    }

    @Test
    void listMapsUnknownTenantTo404() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(integrationService.list(eq(tenantId), eq(null), any(Pageable.class)))
            .thenThrow(new ResourceNotFoundException("Tenant not found with id: " + tenantId));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/integrations", tenantId))
            .andExpect(status().isNotFound());
    }

    @Test
    void getReturns200() throws Exception {
        Integration integration = integration();
        when(integrationService.get(tenant.getId(), integration.getId()))
            .thenReturn(IntegrationResponse.from(integration));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/integrations/{integrationId}", tenant.getId(),
                integration.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(integration.getId().toString()));
    }

    @Test
    void getMapsMissingIntegrationTo404() throws Exception {
        UUID integrationId = UUID.randomUUID();
        when(integrationService.get(tenant.getId(), integrationId))
            .thenThrow(new ResourceNotFoundException("Integration not found for this tenant"));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/integrations/{integrationId}", tenant.getId(), integrationId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Integration not found for this tenant"));
    }

    @Test
    void updateReturns200() throws Exception {
        Integration integration = integration();
        when(integrationService.update(eq(tenant.getId()), eq(integration.getId()), any(UpdateIntegrationRequest.class)))
            .thenReturn(IntegrationResponse.from(integration));

        String body = "{\"displayName\": \"Primary Voice\", \"provider\": \"GENESYS_CLOUD\", "
            + "\"credentialRef\": \"vault://x\", \"status\": \"DISABLED\"}";
        mockMvc.perform(put("/api/v1/tenants/{tenantId}/integrations/{integrationId}", tenant.getId(),
                integration.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(integration.getId().toString()));
    }

    @Test
    void updateMapsDuplicateNameTo409() throws Exception {
        UUID integrationId = UUID.randomUUID();
        when(integrationService.update(eq(tenant.getId()), eq(integrationId), any(UpdateIntegrationRequest.class)))
            .thenThrow(new DuplicateResourceException("Integration display name already exists for this tenant"));

        String body = "{\"displayName\": \"Other\", \"provider\": \"TWILIO\", "
            + "\"credentialRef\": \"vault://x\", \"status\": \"ACTIVE\"}";
        mockMvc.perform(put("/api/v1/tenants/{tenantId}/integrations/{integrationId}", tenant.getId(), integrationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateMapsOptimisticLockingTo409() throws Exception {
        UUID integrationId = UUID.randomUUID();
        when(integrationService.update(eq(tenant.getId()), eq(integrationId), any(UpdateIntegrationRequest.class)))
            .thenThrow(new OptimisticLockingFailureException("stale"));

        String body = "{\"displayName\": \"Other\", \"provider\": \"TWILIO\", "
            + "\"credentialRef\": \"vault://x\", \"status\": \"ACTIVE\"}";
        mockMvc.perform(put("/api/v1/tenants/{tenantId}/integrations/{integrationId}", tenant.getId(), integrationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message")
                .value("The resource changed while this request was being processed; reload and retry"));
    }

    @Test
    void updateRejectsMissingStatusWith400() throws Exception {
        UUID integrationId = UUID.randomUUID();
        String body = "{\"displayName\": \"Other\", \"provider\": \"TWILIO\", \"credentialRef\": \"vault://x\"}";
        mockMvc.perform(put("/api/v1/tenants/{tenantId}/integrations/{integrationId}", tenant.getId(), integrationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.status").exists());
    }

    @Test
    void deleteReturns204() throws Exception {
        Integration integration = integration();

        mockMvc.perform(delete("/api/v1/tenants/{tenantId}/integrations/{integrationId}", tenant.getId(),
                integration.getId()))
            .andExpect(status().isNoContent());

        verify(integrationService).delete(tenant.getId(), integration.getId());
    }

    @Test
    void deleteMapsMissingIntegrationTo404() throws Exception {
        UUID integrationId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Integration not found for this tenant"))
            .when(integrationService).delete(tenant.getId(), integrationId);

        mockMvc.perform(delete("/api/v1/tenants/{tenantId}/integrations/{integrationId}", tenant.getId(),
                integrationId))
            .andExpect(status().isNotFound());
    }

    @Test
    void createMapsDatabaseConflictTo409() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(integrationService.create(eq(tenantId), any(CreateIntegrationRequest.class)))
            .thenThrow(new DataIntegrityViolationException("constraint"));

        mockMvc.perform(post("/api/v1/tenants/{tenantId}/integrations", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message")
                .value("The request conflicts with an existing database constraint"));
    }

    @Test
    void getRejectsNonUuidIntegrationIdWith400() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/{tenantId}/integrations/{integrationId}", tenant.getId(), "nope"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.integrationId").value("Unsupported value"));
    }
}
