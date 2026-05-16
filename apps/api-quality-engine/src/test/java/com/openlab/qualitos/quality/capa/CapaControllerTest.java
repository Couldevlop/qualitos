package com.openlab.qualitos.quality.capa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
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
@WebMvcTest(controllers = CapaController.class)
class CapaControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CapaService service;
    ObjectMapper om;

    static final UUID CAPA = UUID.randomUUID();
    static final UUID ACTION = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.findAll(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(caseResp(CapaStatus.OPEN))));
        mockMvc.perform(get("/api/v1/capa/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(CAPA.toString()));
    }

    @Test @WithMockUser
    void list_withFilter() throws Exception {
        when(service.findAll(eq(CapaStatus.CLOSED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(caseResp(CapaStatus.CLOSED))));
        mockMvc.perform(get("/api/v1/capa/cases").param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("CLOSED"));
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(service.createCase(any())).thenReturn(caseResp(CapaStatus.OPEN));
        CapaDto.CreateCaseRequest req = new CapaDto.CreateCaseRequest(
                "t", null, CapaType.CORRECTIVE, CapaCriticity.HIGH,
                CapaSourceType.NON_CONFORMITY, null, OWNER, null, null);
        mockMvc.perform(post("/api/v1/capa/cases").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CAPA.toString()));
    }

    @Test @WithMockUser
    void create_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/capa/cases").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CORRECTIVE\",\"criticity\":\"HIGH\",\"sourceType\":\"INTERNAL\",\"ownerId\":\""
                                + OWNER + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/capa/cases").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"criticity\":\"HIGH\",\"sourceType\":\"INTERNAL\",\"ownerId\":\""
                                + OWNER + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_found() throws Exception {
        when(service.findById(CAPA)).thenReturn(caseResp(CapaStatus.OPEN));
        mockMvc.perform(get("/api/v1/capa/cases/{id}", CAPA)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound() throws Exception {
        when(service.findById(CAPA)).thenThrow(new CapaNotFoundException(CAPA));
        mockMvc.perform(get("/api/v1/capa/cases/{id}", CAPA)).andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_success() throws Exception {
        when(service.updateCase(eq(CAPA), any())).thenReturn(caseResp(CapaStatus.OPEN));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}", CAPA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void start_success() throws Exception {
        when(service.startCase(CAPA)).thenReturn(caseResp(CapaStatus.IN_PROGRESS));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/start", CAPA).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test @WithMockUser
    void start_invalid_returns409() throws Exception {
        when(service.startCase(CAPA)).thenThrow(new CapaStateException("nope"));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/start", CAPA).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void resolve_success() throws Exception {
        when(service.resolveCase(CAPA)).thenReturn(caseResp(CapaStatus.RESOLVED));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/resolve", CAPA).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void effectiveness_success() throws Exception {
        when(service.verifyEffectiveness(eq(CAPA), any())).thenReturn(caseResp(CapaStatus.CLOSED));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/effectiveness", CAPA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"effective\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test @WithMockUser
    void effectiveness_missingField_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/effectiveness", CAPA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void reject_success() throws Exception {
        when(service.rejectCase(CAPA)).thenReturn(caseResp(CapaStatus.REJECTED));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/reject", CAPA).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_success() throws Exception {
        doNothing().when(service).deleteCase(CAPA);
        mockMvc.perform(delete("/api/v1/capa/cases/{id}", CAPA).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void delete_closed_returns409() throws Exception {
        doThrow(new CapaStateException("c")).when(service).deleteCase(CAPA);
        mockMvc.perform(delete("/api/v1/capa/cases/{id}", CAPA).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void addAction_success() throws Exception {
        when(service.addAction(eq(CAPA), any())).thenReturn(actionResp());
        CapaDto.ActionRequest req = new CapaDto.ActionRequest("a", null, null, null, null);
        mockMvc.perform(post("/api/v1/capa/cases/{id}/actions", CAPA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ACTION.toString()));
    }

    @Test @WithMockUser
    void addAction_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/capa/cases/{id}/actions", CAPA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void updateAction_success() throws Exception {
        when(service.updateAction(eq(CAPA), eq(ACTION), any())).thenReturn(actionResp());
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/actions/{aid}", CAPA, ACTION).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void updateAction_notFound_returns404() throws Exception {
        when(service.updateAction(eq(CAPA), eq(ACTION), any()))
                .thenThrow(new CapaActionNotFoundException(ACTION));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/actions/{aid}", CAPA, ACTION).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void deleteAction_success() throws Exception {
        doNothing().when(service).deleteAction(CAPA, ACTION);
        mockMvc.perform(delete("/api/v1/capa/cases/{id}/actions/{aid}", CAPA, ACTION).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void create_missingTenant_returns403() throws Exception {
        when(service.createCase(any())).thenThrow(new MissingTenantContextException());
        CapaDto.CreateCaseRequest req = new CapaDto.CreateCaseRequest(
                "t", null, CapaType.CORRECTIVE, CapaCriticity.HIGH,
                CapaSourceType.NON_CONFORMITY, null, OWNER, null, null);
        mockMvc.perform(post("/api/v1/capa/cases").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // helpers
    private CapaDto.CaseResponse caseResp(CapaStatus s) {
        return new CapaDto.CaseResponse(
                CAPA, TENANT, "t", null, CapaType.CORRECTIVE, CapaCriticity.HIGH, s,
                CapaSourceType.NON_CONFORMITY, null, OWNER, null, null,
                null, null, null, null,
                Instant.now(), Instant.now(), List.of());
    }

    private CapaDto.ActionResponse actionResp() {
        return new CapaDto.ActionResponse(
                ACTION, CAPA, "a", null, CapaActionStatus.PENDING,
                null, null, null, Instant.now(), Instant.now());
    }
}
