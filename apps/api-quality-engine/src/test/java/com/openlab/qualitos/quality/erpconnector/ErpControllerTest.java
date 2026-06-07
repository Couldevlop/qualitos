package com.openlab.qualitos.quality.erpconnector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
@WebMvcTest(controllers = ErpController.class)
class ErpControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ErpConnectorService service;
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
        mockMvc.perform(get("/api/v1/erp/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(CONN.toString()));
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(service.createConnection(any())).thenReturn(resp());
        ErpDto.CreateConnectionRequest req = new ErpDto.CreateConnectionRequest(
                "prod-sap", ErpProvider.SAP, "https://sap.example.com", "svc",
                "supersecret", "1000", USER);
        mockMvc.perform(post("/api/v1/erp/connections").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CONN.toString()))
                .andExpect(jsonPath("$.provider").value("SAP"));
    }

    @Test @WithMockUser
    void create_httpBaseUrl_returns400() throws Exception {
        String body = "{\"name\":\"n\",\"provider\":\"SAP\",\"baseUrl\":\"http://x\","
                + "\"secret\":\"abcd\",\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/erp/connections").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingProvider_returns400() throws Exception {
        String body = "{\"name\":\"n\",\"baseUrl\":\"https://x\",\"secret\":\"abcd\","
                + "\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/erp/connections").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_notFound_returns404() throws Exception {
        when(service.getConnection(CONN)).thenThrow(new ErpConnectionNotFoundException(CONN));
        mockMvc.perform(get("/api/v1/erp/connections/{id}", CONN))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_patches() throws Exception {
        when(service.updateConnection(any(), any())).thenReturn(resp());
        mockMvc.perform(patch("/api/v1/erp/connections/{id}", CONN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/erp/connections/{id}", CONN).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).deleteConnection(CONN);
    }

    @Test @WithMockUser
    void sync_returns200WithReport() throws Exception {
        when(service.sync(CONN)).thenReturn(
                new ErpDto.SyncReport(CONN, 4, 1, 3, 2, Instant.now(), null));
        mockMvc.perform(post("/api/v1/erp/connections/{id}/sync", CONN).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suppliersImported").value(4))
                .andExpect(jsonPath("$.kpisImported").value(3))
                .andExpect(jsonPath("$.kpisIgnored").value(2));
    }

    private ErpDto.ConnectionResponse resp() {
        return new ErpDto.ConnectionResponse(
                CONN, TENANT, "prod-sap", ErpProvider.SAP,
                "https://sap.example.com", "svc", "1000",
                ErpConnectionStatus.ACTIVE, 0, null, null, USER, Instant.now(), Instant.now());
    }
}
