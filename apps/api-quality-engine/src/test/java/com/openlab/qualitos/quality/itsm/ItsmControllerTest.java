package com.openlab.qualitos.quality.itsm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
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

@WebMvcTest(controllers = ItsmController.class)
class ItsmControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ItsmConnectorService service;
    ObjectMapper om;

    static final UUID CONN = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.listConnections(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(resp())));
        mockMvc.perform(get("/api/v1/itsm/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(CONN.toString()));
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(service.createConnection(any())).thenReturn(resp());
        ItsmDto.CreateConnectionRequest req = new ItsmDto.CreateConnectionRequest(
                "prod-sn", ItsmProvider.SERVICENOW,
                "https://x.service-now.com", "admin", "supersecret", null, USER);
        mockMvc.perform(post("/api/v1/itsm/connections").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CONN.toString()));
    }

    @Test @WithMockUser
    void create_httpBaseUrl_returns400() throws Exception {
        // baseUrl doit commencer par https:// (SSRF + RGPD/sécurité)
        String body = "{\"name\":\"n\",\"provider\":\"SERVICENOW\",\"baseUrl\":\"http://x\","
                + "\"secret\":\"abcd\",\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/itsm/connections").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingProvider_returns400() throws Exception {
        String body = "{\"name\":\"n\",\"baseUrl\":\"https://x\",\"secret\":\"abcd\","
                + "\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/itsm/connections").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_found() throws Exception {
        when(service.getConnection(CONN)).thenReturn(resp());
        mockMvc.perform(get("/api/v1/itsm/connections/{id}", CONN))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_returns404() throws Exception {
        when(service.getConnection(CONN)).thenThrow(new ItsmConnectionNotFoundException(CONN));
        mockMvc.perform(get("/api/v1/itsm/connections/{id}", CONN))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_patches() throws Exception {
        when(service.updateConnection(any(), any())).thenReturn(resp());
        mockMvc.perform(patch("/api/v1/itsm/connections/{id}", CONN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/itsm/connections/{id}", CONN).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).deleteConnection(CONN);
    }

    @Test @WithMockUser
    void sync_returns200WithReport() throws Exception {
        when(service.syncConnection(CONN)).thenReturn(
                new ItsmDto.SyncReport(CONN, 5, 3, 2, Instant.now(), null));
        mockMvc.perform(post("/api/v1/itsm/connections/{id}/sync", CONN).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFetched").value(5))
                .andExpect(jsonPath("$.newImports").value(3));
    }

    @Test @WithMockUser
    void mappings_listAll() throws Exception {
        when(service.listMappings(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        new ItsmDto.MappingResponse(UUID.randomUUID(), TENANT, CONN,
                                "EXT-1", null, "Open", "1", "Title",
                                "NON_CONFORMITY", null, Instant.now(), Instant.now()))));
        mockMvc.perform(get("/api/v1/itsm/mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].externalId").value("EXT-1"));
    }

    @Test @WithMockUser
    void mappings_filteredByConnection() throws Exception {
        when(service.listMappings(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/v1/itsm/mappings").param("connectionId", CONN.toString()))
                .andExpect(status().isOk());
    }

    private ItsmDto.ConnectionResponse resp() {
        return new ItsmDto.ConnectionResponse(
                CONN, TENANT, "prod-sn", ItsmProvider.SERVICENOW,
                "https://x.service-now.com", "admin", null,
                ConnectionStatus.ACTIVE, 0, null, null, USER, Instant.now(), Instant.now());
    }
}
