package com.openlab.qualitos.quality.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = WorkflowController.class)
class WorkflowControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean WorkflowService service;
    ObjectMapper om;

    static final UUID WF = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID ACTOR = UUID.randomUUID();

    static final String VALID_BPMN =
            "<?xml version=\"1.0\"?><bpmn:definitions xmlns:bpmn=\"http://x\"><bpmn:process/></bpmn:definitions>";

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.findAll(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary(WorkflowStatus.DRAFT))));
        mockMvc.perform(get("/api/v1/workflow/definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(WF.toString()))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    @Test @WithMockUser
    void list_withStatusFilter() throws Exception {
        when(service.findAll(eq(WorkflowStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary(WorkflowStatus.PUBLISHED))));
        mockMvc.perform(get("/api/v1/workflow/definitions").param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"));
    }

    @Test @WithMockUser
    void get_found() throws Exception {
        when(service.findById(WF)).thenReturn(resp(WorkflowStatus.DRAFT));
        mockMvc.perform(get("/api/v1/workflow/definitions/{id}", WF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bpmnXml").value(VALID_BPMN));
    }

    @Test @WithMockUser
    void get_notFound_returns404() throws Exception {
        when(service.findById(WF)).thenThrow(new WorkflowNotFoundException(WF));
        mockMvc.perform(get("/api/v1/workflow/definitions/{id}", WF))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(service.create(any())).thenReturn(resp(WorkflowStatus.DRAFT));
        WorkflowDto.CreateRequest req = new WorkflowDto.CreateRequest("CAPA", "desc", VALID_BPMN);
        mockMvc.perform(post("/api/v1/workflow/definitions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test @WithMockUser
    void create_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/workflow/definitions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bpmnXml\":\"" + VALID_BPMN.replace("\"", "\\\"") + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingBpmn_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/workflow/definitions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CAPA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_blankBpmn_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/workflow/definitions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CAPA\",\"bpmnXml\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void update_success() throws Exception {
        when(service.update(eq(WF), any())).thenReturn(resp(WorkflowStatus.DRAFT));
        mockMvc.perform(put("/api/v1/workflow/definitions/{id}", WF).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void update_published_returns409() throws Exception {
        when(service.update(eq(WF), any())).thenThrow(new WorkflowStateException("not draft"));
        mockMvc.perform(put("/api/v1/workflow/definitions/{id}", WF).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void publish_success() throws Exception {
        when(service.publish(WF)).thenReturn(resp(WorkflowStatus.PUBLISHED));
        mockMvc.perform(post("/api/v1/workflow/definitions/{id}/publish", WF).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test @WithMockUser
    void publish_invalid_returns409() throws Exception {
        when(service.publish(WF)).thenThrow(new WorkflowStateException("not draft"));
        mockMvc.perform(post("/api/v1/workflow/definitions/{id}/publish", WF).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void archive_success() throws Exception {
        when(service.archive(WF)).thenReturn(resp(WorkflowStatus.ARCHIVED));
        mockMvc.perform(post("/api/v1/workflow/definitions/{id}/archive", WF).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test @WithMockUser
    void delete_returns204() throws Exception {
        doNothing().when(service).delete(WF);
        mockMvc.perform(delete("/api/v1/workflow/definitions/{id}", WF).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void delete_notFound_returns404() throws Exception {
        doThrow(new WorkflowNotFoundException(WF)).when(service).delete(WF);
        mockMvc.perform(delete("/api/v1/workflow/definitions/{id}", WF).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void create_missingTenant_returns403() throws Exception {
        when(service.create(any())).thenThrow(new MissingTenantContextException());
        WorkflowDto.CreateRequest req = new WorkflowDto.CreateRequest("CAPA", null, VALID_BPMN);
        mockMvc.perform(post("/api/v1/workflow/definitions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // helpers
    private WorkflowDto.Response resp(WorkflowStatus s) {
        return new WorkflowDto.Response(
                WF, TENANT, "wf", "desc", VALID_BPMN, s, 1,
                ACTOR, ACTOR, Instant.now(), Instant.now());
    }

    private WorkflowDto.Summary summary(WorkflowStatus s) {
        return new WorkflowDto.Summary(
                WF, TENANT, "wf", "desc", s, 1,
                ACTOR, ACTOR, Instant.now(), Instant.now());
    }
}
