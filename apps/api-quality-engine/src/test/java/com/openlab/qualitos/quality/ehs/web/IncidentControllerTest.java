package com.openlab.qualitos.quality.ehs.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.ehs.application.IncidentDto;
import com.openlab.qualitos.quality.ehs.application.IncidentService;
import com.openlab.qualitos.quality.ehs.domain.IncidentNotFoundException;
import com.openlab.qualitos.quality.ehs.domain.IncidentRepository;
import com.openlab.qualitos.quality.ehs.domain.IncidentSeverity;
import com.openlab.qualitos.quality.ehs.domain.IncidentStateException;
import com.openlab.qualitos.quality.ehs.domain.IncidentStatus;
import com.openlab.qualitos.quality.ehs.domain.IncidentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = IncidentController.class)
class IncidentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean IncidentService service;
    ObjectMapper om;

    static final UUID ID = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new IncidentRepository.PagedResult<>(List.of(view()), 1L, 0, 50));
        mockMvc.perform(get("/api/v1/ehs/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test @WithMockUser
    void report_201() throws Exception {
        when(service.report(any())).thenReturn(view());
        IncidentWebDto.ReportRequest req = new IncidentWebDto.ReportRequest(
                "EHS-1", "Fall", "desc", IncidentType.INJURY,
                IncidentSeverity.HIGH, null, "Loc", USER);
        mockMvc.perform(post("/api/v1/ehs/incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void report_invalidCode_400() throws Exception {
        String body = "{\"code\":\"BAD CODE!\",\"title\":\"t\",\"type\":\"INJURY\","
                + "\"reportedBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ehs/incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void report_missingType_400() throws Exception {
        String body = "{\"code\":\"EHS-1\",\"title\":\"t\",\"reportedBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ehs/incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new IncidentNotFoundException(ID));
        mockMvc.perform(get("/api/v1/ehs/incidents/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(any(), any())).thenReturn(view());
        mockMvc.perform(patch("/api/v1/ehs/incidents/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"renamed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void investigate_200() throws Exception {
        when(service.investigate(eq(ID), any())).thenReturn(view());
        mockMvc.perform(post("/api/v1/ehs/incidents/{id}/investigate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerUserId\":\"" + USER + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void investigate_invalidState_409() throws Exception {
        when(service.investigate(eq(ID), any()))
                .thenThrow(new IncidentStateException("nope"));
        mockMvc.perform(post("/api/v1/ehs/incidents/{id}/investigate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void mitigate_missingRootCause_400() throws Exception {
        String body = "{\"correctiveActions\":\"ca\"}";
        mockMvc.perform(post("/api/v1/ehs/incidents/{id}/mitigate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void mitigate_200() throws Exception {
        when(service.mitigate(eq(ID), any())).thenReturn(view());
        mockMvc.perform(post("/api/v1/ehs/incidents/{id}/mitigate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rootCause\":\"rc\",\"correctiveActions\":\"ca\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void close_200() throws Exception {
        when(service.close(ID)).thenReturn(view());
        mockMvc.perform(post("/api/v1/ehs/incidents/{id}/close", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void cancel_200() throws Exception {
        when(service.cancel(ID)).thenReturn(view());
        mockMvc.perform(post("/api/v1/ehs/incidents/{id}/cancel", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void linkCapa_200() throws Exception {
        when(service.linkCapa(eq(ID), any())).thenReturn(view());
        mockMvc.perform(post("/api/v1/ehs/incidents/{id}/link-capa", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capaCaseId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void linkCapa_missingId_400() throws Exception {
        mockMvc.perform(post("/api/v1/ehs/incidents/{id}/link-capa", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void linkNc_200() throws Exception {
        when(service.linkNc(eq(ID), any())).thenReturn(view());
        mockMvc.perform(post("/api/v1/ehs/incidents/{id}/link-nc", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ncId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/ehs/incidents/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).delete(ID);
    }

    @Test @WithMockUser
    void statistics_200() throws Exception {
        when(service.statistics()).thenReturn(new IncidentService.Statistics(
                TENANT, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L));
        mockMvc.perform(get("/api/v1/ehs/incidents/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.injuries").value(6));
    }

    private IncidentDto.IncidentView view() {
        return new IncidentDto.IncidentView(
                ID, TENANT, "EHS-1", "Fall", "desc",
                IncidentType.INJURY, IncidentSeverity.MEDIUM, IncidentStatus.REPORTED,
                Instant.now(), Instant.now(), null, null,
                "Loc", null, null, null, null,
                null, null, USER, USER, Instant.now(), Instant.now());
    }
}
