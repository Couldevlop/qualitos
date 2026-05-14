package com.openlab.qualitos.core.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.core.common.GlobalExceptionHandler;
import com.openlab.qualitos.core.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("TenantController")
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantService tenantService;

    // JwtDecoder est requis par SecurityConfig (oauth2ResourceServer)
    // TenantJwtFilter est créé via SecurityConfig.tenantJwtFilter(JwtDecoder)
    @MockBean
    private JwtDecoder jwtDecoder;

    private static final UUID TENANT_ID = UUID.randomUUID();

    private TenantDto.Response sampleResponse() {
        return new TenantDto.Response(
                TENANT_ID, "acme-corp", "ACME Corp", Tenant.Plan.PRO,
                true, Instant.now(), Instant.now());
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor superAdminJwt() {
        return jwt().authorities(
                new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")
        );
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return jwt().authorities(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
    }

    @Nested
    @DisplayName("GET /api/v1/tenants")
    class ListTenants {

        @Test
        @DisplayName("Returns 200 with page of tenants for SUPER_ADMIN")
        void returns200ForSuperAdmin() throws Exception {
            given(tenantService.findAll(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(sampleResponse())));

            mockMvc.perform(get("/api/v1/tenants").with(superAdminJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].slug").value("acme-corp"));
        }

        @Test
        @DisplayName("Returns 403 for ADMIN role")
        void returns403ForAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/tenants").with(adminJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 when unauthenticated")
        void returns401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/tenants"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tenants/{id}")
    class GetTenant {

        @Test
        @DisplayName("Returns 200 with tenant for SUPER_ADMIN")
        void returns200ForSuperAdmin() throws Exception {
            given(tenantService.findById(TENANT_ID)).willReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/tenants/{id}", TENANT_ID).with(superAdminJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TENANT_ID.toString()))
                    .andExpect(jsonPath("$.slug").value("acme-corp"))
                    .andExpect(jsonPath("$.plan").value("PRO"));
        }

        @Test
        @DisplayName("Returns 404 when tenant not found")
        void returns404WhenNotFound() throws Exception {
            given(tenantService.findById(TENANT_ID))
                    .willThrow(new TenantNotFoundException(TENANT_ID));

            mockMvc.perform(get("/api/v1/tenants/{id}", TENANT_ID).with(superAdminJwt()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tenants/by-slug/{slug}")
    class GetTenantBySlug {

        @Test
        @DisplayName("Returns 200 with tenant for SUPER_ADMIN")
        void returns200ForSuperAdmin() throws Exception {
            given(tenantService.findBySlug("acme-corp")).willReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/tenants/by-slug/acme-corp").with(superAdminJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value("acme-corp"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tenants")
    class CreateTenant {

        @Test
        @DisplayName("Returns 201 with Location header when created")
        void returns201WithLocation() throws Exception {
            TenantDto.CreateRequest request = new TenantDto.CreateRequest(
                    "new-tenant", "New Tenant", Tenant.Plan.STARTER);

            TenantDto.Response response = new TenantDto.Response(
                    TENANT_ID, "new-tenant", "New Tenant", Tenant.Plan.STARTER,
                    true, Instant.now(), Instant.now());

            given(tenantService.create(any())).willReturn(response);

            mockMvc.perform(post("/api/v1/tenants")
                            .with(superAdminJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.slug").value("new-tenant"));
        }

        @Test
        @DisplayName("Returns 400 when slug is invalid")
        void returns400WhenSlugInvalid() throws Exception {
            // Slug avec majuscules et espaces — viole @Pattern
            String invalidBody = """
                    {"slug": "INVALID SLUG!", "name": "Name"}
                    """;

            mockMvc.perform(post("/api/v1/tenants")
                            .with(superAdminJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 409 when slug already exists")
        void returns409WhenConflict() throws Exception {
            TenantDto.CreateRequest request = new TenantDto.CreateRequest(
                    "existing-slug", "Name", null);
            given(tenantService.create(any()))
                    .willThrow(new TenantAlreadyExistsException("existing-slug"));

            mockMvc.perform(post("/api/v1/tenants")
                            .with(superAdminJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/tenants/{id}")
    class UpdateTenant {

        @Test
        @DisplayName("Returns 200 with updated tenant")
        void returns200WhenUpdated() throws Exception {
            TenantDto.UpdateRequest request = new TenantDto.UpdateRequest(
                    "Updated Name", Tenant.Plan.ENTERPRISE, true);
            TenantDto.Response updated = new TenantDto.Response(
                    TENANT_ID, "acme-corp", "Updated Name", Tenant.Plan.ENTERPRISE,
                    true, Instant.now(), Instant.now());

            given(tenantService.update(eq(TENANT_ID), any())).willReturn(updated);

            mockMvc.perform(put("/api/v1/tenants/{id}", TENANT_ID)
                            .with(superAdminJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Name"))
                    .andExpect(jsonPath("$.plan").value("ENTERPRISE"));
        }

        @Test
        @DisplayName("Returns 400 when name is blank")
        void returns400WhenNameBlank() throws Exception {
            String body = """
                    {"name": ""}
                    """;

            mockMvc.perform(put("/api/v1/tenants/{id}", TENANT_ID)
                            .with(superAdminJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/tenants/{id}")
    class DeactivateTenant {

        @Test
        @DisplayName("Returns 204 when deactivated")
        void returns204WhenDeactivated() throws Exception {
            doNothing().when(tenantService).deactivate(TENANT_ID);

            mockMvc.perform(delete("/api/v1/tenants/{id}", TENANT_ID).with(superAdminJwt()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Returns 404 when tenant not found")
        void returns404WhenNotFound() throws Exception {
            org.mockito.Mockito.doThrow(new TenantNotFoundException(TENANT_ID))
                    .when(tenantService).deactivate(TENANT_ID);

            mockMvc.perform(delete("/api/v1/tenants/{id}", TENANT_ID).with(superAdminJwt()))
                    .andExpect(status().isNotFound());
        }
    }
}
