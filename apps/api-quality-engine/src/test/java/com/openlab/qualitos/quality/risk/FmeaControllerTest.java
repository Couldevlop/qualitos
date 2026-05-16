package com.openlab.qualitos.quality.risk;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = FmeaController.class)
class FmeaControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FmeaService service;
    ObjectMapper om;

    static final UUID PROJ = UUID.randomUUID();
    static final UUID ITEM = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.listProjects(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projResp())));
        mockMvc.perform(get("/api/v1/fmea/projects"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(service.createProject(any())).thenReturn(projResp());
        FmeaDto.CreateProjectRequest req = new FmeaDto.CreateProjectRequest(
                "p1", "Project 1", null, FmeaType.PROCESS_FMEA, null, null, USER);
        mockMvc.perform(post("/api/v1/fmea/projects").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_missingType_returns400() throws Exception {
        String body = "{\"code\":\"p1\",\"name\":\"n\",\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/fmea/projects").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_invalidCode_returns400() throws Exception {
        String body = "{\"code\":\"bad code!\",\"name\":\"n\",\"type\":\"PROCESS_FMEA\",\"createdBy\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/fmea/projects").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_thresholdOutOfRange_returns400() throws Exception {
        String body = "{\"code\":\"p1\",\"name\":\"n\",\"type\":\"PROCESS_FMEA\","
                + "\"criticalRpnThreshold\":99999,\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/fmea/projects").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_found() throws Exception {
        when(service.getProject(PROJ)).thenReturn(projResp());
        mockMvc.perform(get("/api/v1/fmea/projects/{id}", PROJ))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_returns404() throws Exception {
        when(service.getProject(PROJ)).thenThrow(new FmeaProjectNotFoundException(PROJ));
        mockMvc.perform(get("/api/v1/fmea/projects/{id}", PROJ))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_patches() throws Exception {
        when(service.updateProject(any(), any())).thenReturn(projResp());
        mockMvc.perform(patch("/api/v1/fmea/projects/{id}", PROJ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/fmea/projects/{id}", PROJ).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).deleteProject(PROJ);
    }

    @Test @WithMockUser
    void activate_returns200() throws Exception {
        when(service.activate(PROJ)).thenReturn(projResp());
        mockMvc.perform(post("/api/v1/fmea/projects/{id}/activate", PROJ).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void invalidTransition_returns409() throws Exception {
        when(service.activate(PROJ)).thenThrow(new FmeaStateException("Cannot reactivate ARCHIVED"));
        mockMvc.perform(post("/api/v1/fmea/projects/{id}/activate", PROJ).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void reopen_returns200() throws Exception {
        when(service.reopen(PROJ)).thenReturn(projResp());
        mockMvc.perform(post("/api/v1/fmea/projects/{id}/reopen", PROJ).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void archive_returns200() throws Exception {
        when(service.archive(PROJ)).thenReturn(projResp());
        mockMvc.perform(post("/api/v1/fmea/projects/{id}/archive", PROJ).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void statistics_returns200() throws Exception {
        when(service.statistics(PROJ)).thenReturn(
                new FmeaDto.ProjectStatistics(PROJ, 5L, 1L, 280, 84.0, 100));
        mockMvc.perform(get("/api/v1/fmea/projects/{id}/statistics", PROJ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.criticalItems").value(1));
    }

    @Test @WithMockUser
    void listItems_returns200() throws Exception {
        when(service.listItems(eq(PROJ), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(itemResp())));
        mockMvc.perform(get("/api/v1/fmea/projects/{projectId}/items", PROJ))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void addItem_returns201() throws Exception {
        when(service.addItem(eq(PROJ), any())).thenReturn(itemResp());
        FmeaDto.CreateItemRequest req = new FmeaDto.CreateItemRequest(
                "fn", "fm", "fe", null, null, 7, 3, 4, null, null, null, null, null, null);
        mockMvc.perform(post("/api/v1/fmea/projects/{projectId}/items", PROJ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addItem_severityOutOfRange_returns400() throws Exception {
        // severity=11 doit échouer la validation @Max(10)
        String body = "{\"severity\":11,\"occurrence\":3,\"detection\":4}";
        mockMvc.perform(post("/api/v1/fmea/projects/{projectId}/items", PROJ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void addItem_missingSeverity_returns400() throws Exception {
        String body = "{\"occurrence\":3,\"detection\":4}";
        mockMvc.perform(post("/api/v1/fmea/projects/{projectId}/items", PROJ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void updateItem_patches() throws Exception {
        when(service.updateItem(eq(PROJ), eq(ITEM), any())).thenReturn(itemResp());
        mockMvc.perform(patch("/api/v1/fmea/projects/{projectId}/items/{itemId}", PROJ, ITEM)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":9}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void deleteItem_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/fmea/projects/{projectId}/items/{itemId}", PROJ, ITEM)
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).deleteItem(PROJ, ITEM);
    }

    @Test @WithMockUser
    void item_notFound_returns404() throws Exception {
        when(service.updateItem(eq(PROJ), eq(ITEM), any()))
                .thenThrow(new FmeaItemNotFoundException(ITEM));
        mockMvc.perform(patch("/api/v1/fmea/projects/{projectId}/items/{itemId}", PROJ, ITEM)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":5}"))
                .andExpect(status().isNotFound());
    }

    private FmeaDto.ProjectResponse projResp() {
        return new FmeaDto.ProjectResponse(
                PROJ, TENANT, "p1", "Project 1", null,
                FmeaType.PROCESS_FMEA, FmeaStatus.DRAFT, 100, 1,
                null, null, USER, Instant.now(), Instant.now());
    }

    private FmeaDto.ItemResponse itemResp() {
        return new FmeaDto.ItemResponse(
                ITEM, TENANT, PROJ, 1,
                "fn", "fm", "fe", null, null,
                7, 3, 4, 84,
                null, null, null,
                null, null, null, null,
                false,
                Instant.now(), Instant.now());
    }
}
