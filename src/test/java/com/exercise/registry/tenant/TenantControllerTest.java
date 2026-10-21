package com.exercise.registry.tenant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.exercise.registry.common.GlobalExceptionHandler;
import com.exercise.registry.common.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
public class TenantControllerTest {
    @Mock
    private TenantService tenantService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TenantController(tenantService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void createReturns201WithTenantBody() throws Exception {
        Tenant tenant = new Tenant("Acme Support", "acme-support");
        when(tenantService.create(any(CreateTenantRequest.class))).thenReturn(TenantResponse.from(tenant));

        mockMvc.perform(post("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Acme Support\", \"slug\": \"acme-support\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Acme Support"))
            .andExpect(jsonPath("$.slug").value("acme-support"))
            .andExpect(jsonPath("$.id").value(tenant.getId().toString()))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createRejectsMissingSlugWith400() throws Exception {
        mockMvc.perform(post("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Acme Support\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.details.slug").exists());
    }

    @Test
    void createRejectsBlankNameWith400() throws Exception {
        mockMvc.perform(post("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"  \", \"slug\": \"acme\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.name").exists());
    }

    @Test
    void createRejectsInvalidSlugFormatWith400() throws Exception {
        mockMvc.perform(post("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Acme\", \"slug\": \"Acme_Support\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.slug").exists());
    }

    @Test
    void createRejectsMalformedJsonWith400() throws Exception {
        mockMvc.perform(post("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not-json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Request body is invalid or contains an unsupported enum value"));
    }

    @Test
    void getReturns200() throws Exception {
        Tenant tenant = new Tenant("Acme", "acme");
        when(tenantService.get(tenant.getId())).thenReturn(TenantResponse.from(tenant));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}", tenant.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("acme"));
    }

    @Test
    void getMapsMissingTenantTo404() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.get(tenantId))
            .thenThrow(new ResourceNotFoundException("Tenant not found with ID: " + tenantId));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}", tenantId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Tenant not found with ID: " + tenantId))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.path").value("/api/v1/tenants/" + tenantId))
            .andExpect(jsonPath("$.details").exists());
    }

    @Test
    void getRejectsNonUuidPathVariableWith400() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/{tenantId}", "notauuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.tenantId").value("Unsupported value"));
    }

    @Test
    void createDelegatesToService() throws Exception {
        Tenant tenant = new Tenant("Acme", "acme");
        when(tenantService.create(any(CreateTenantRequest.class))).thenReturn(TenantResponse.from(tenant));

        mockMvc.perform(post("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Acme\", \"slug\": \"acme\"}"))
            .andExpect(status().isCreated());

        verify(tenantService).create(any(CreateTenantRequest.class));
    }
}
