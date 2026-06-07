package com.openlab.qualitos.quality.commconnector;

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
@WebMvcTest(controllers = CommController.class)
class CommControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CommConnectorService service;
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
        when(service.listConnections(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(resp())));
        mockMvc.perform(get("/api/v1/comm/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(CONN.toString()));
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(service.createConnection(any())).thenReturn(resp());
        CommDto.CreateConnectionRequest req = new CommDto.CreateConnectionRequest(
                "Slack Qualité", CommProvider.SLACK, "https://hooks.slack.com/x", "#qualite", USER);
        mockMvc.perform(post("/api/v1/comm/connections").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CONN.toString()))
                // la réponse ne doit jamais exposer l'URL webhook
                .andExpect(jsonPath("$.webhookUrl").doesNotExist());
    }

    @Test @WithMockUser
    void create_httpWebhookUrl_returns400() throws Exception {
        String body = "{\"name\":\"n\",\"provider\":\"SLACK\",\"webhookUrl\":\"http://x/y\",\"createdBy\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/comm/connections").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingProvider_returns400() throws Exception {
        String body = "{\"name\":\"n\",\"webhookUrl\":\"https://x/y\",\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/comm/connections").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_notFound_returns404() throws Exception {
        when(service.getConnection(CONN)).thenThrow(new CommConnectionNotFoundException(CONN));
        mockMvc.perform(get("/api/v1/comm/connections/{id}", CONN))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_patches() throws Exception {
        when(service.updateConnection(any(), any())).thenReturn(resp());
        mockMvc.perform(patch("/api/v1/comm/connections/{id}", CONN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/comm/connections/{id}", CONN).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).deleteConnection(CONN);
    }

    @Test @WithMockUser
    void test_returns200WithResult() throws Exception {
        when(service.test(CONN)).thenReturn(new CommDto.TestResult(CONN, true, null));
        mockMvc.perform(post("/api/v1/comm/connections/{id}/test", CONN).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test @WithMockUser
    void test_failureMappedAsResult() throws Exception {
        when(service.test(CONN)).thenReturn(new CommDto.TestResult(CONN, false, "HTTP 500"));
        mockMvc.perform(post("/api/v1/comm/connections/{id}/test", CONN).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("HTTP 500"));
    }

    private CommDto.ConnectionResponse resp() {
        return new CommDto.ConnectionResponse(
                CONN, TENANT, "Slack Qualité", CommProvider.SLACK, "#qualite",
                ConnectionStatus.ACTIVE, 0, null, null, USER, Instant.now(), Instant.now());
    }
}
