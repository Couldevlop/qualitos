package com.openlab.qualitos.quality.ehrconnector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.itsm.ConnectionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = EhrController.class)
class EhrControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean EhrConnectorService service;
    ObjectMapper om;

    static final UUID CONN = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void list_returns200() throws Exception {
        when(service.listConnections(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(resp())));
        mockMvc.perform(get("/api/v1/ehr/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(CONN.toString()));
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void create_returns201() throws Exception {
        when(service.createConnection(any())).thenReturn(resp());
        EhrDto.CreateConnectionRequest req = new EhrDto.CreateConnectionRequest(
                "prod-fhir", EhrProvider.FHIR_R5, "https://fhir.example.org/r5",
                EhrAuthMode.BEARER, null, "supersecret", "patient-safety", USER);
        mockMvc.perform(post("/api/v1/ehr/connections").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CONN.toString()));
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void create_httpBaseUrl_returns400() throws Exception {
        String body = "{\"name\":\"n\",\"provider\":\"FHIR_R5\",\"fhirBaseUrl\":\"http://x\","
                + "\"authMode\":\"BEARER\",\"secret\":\"abcd\",\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ehr/connections").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void create_missingProvider_returns400() throws Exception {
        String body = "{\"name\":\"n\",\"fhirBaseUrl\":\"https://x\",\"authMode\":\"BEARER\","
                + "\"secret\":\"abcd\",\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ehr/connections").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void get_notFound_returns404() throws Exception {
        when(service.getConnection(CONN)).thenThrow(new EhrConnectionNotFoundException(CONN));
        mockMvc.perform(get("/api/v1/ehr/connections/{id}", CONN))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void update_patches() throws Exception {
        when(service.updateConnection(any(), any())).thenReturn(resp());
        mockMvc.perform(patch("/api/v1/ehr/connections/{id}", CONN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/ehr/connections/{id}", CONN).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).deleteConnection(CONN);
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void sync_returns200WithReport() throws Exception {
        when(service.sync(CONN)).thenReturn(
                new EhrDto.SyncReport(CONN, 5, 3, 1, 1, Instant.now(), null));
        mockMvc.perform(post("/api/v1/ehr/connections/{id}/sync", CONN).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFetched").value(5))
                .andExpect(jsonPath("$.created").value(3))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.errors").value(1));
    }

    private EhrDto.ConnectionResponse resp() {
        return new EhrDto.ConnectionResponse(
                CONN, TENANT, "prod-fhir", EhrProvider.FHIR_R5,
                "https://fhir.example.org/r5", EhrAuthMode.BEARER, null, "patient-safety",
                ConnectionStatus.ACTIVE, 0, null, null, USER, Instant.now(), Instant.now());
    }
}
