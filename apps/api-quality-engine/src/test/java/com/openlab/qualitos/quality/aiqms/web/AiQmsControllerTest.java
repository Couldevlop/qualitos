package com.openlab.qualitos.quality.aiqms.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.aiqms.application.AiQmsDto;
import com.openlab.qualitos.quality.aiqms.application.AiQmsService;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsNotFoundException;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsStateException;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = AiQmsController.class)
class AiQmsControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AiQmsService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID APPROVER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(null)).thenReturn(List.of(view(AiQmsStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/ai-act/qms")).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_byStatus_200() throws Exception {
        when(service.list(AiQmsStatus.IN_FORCE)).thenReturn(List.of(view(AiQmsStatus.IN_FORCE)));
        mockMvc.perform(get("/api/v1/ai-act/qms").param("status", "IN_FORCE"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_invalidStatus_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/qms").param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_200() throws Exception {
        when(service.get(ID)).thenReturn(view(AiQmsStatus.DRAFT));
        mockMvc.perform(get("/api/v1/ai-act/qms/{id}", ID)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new AiQmsNotFoundException(ID));
        mockMvc.perform(get("/api/v1/ai-act/qms/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("REF-1")).thenReturn(view(AiQmsStatus.DRAFT));
        mockMvc.perform(get("/api/v1/ai-act/qms/by-reference").param("reference", "REF-1"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/qms/by-reference").param("reference", "lowercase"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void draft_201() throws Exception {
        when(service.draft(any())).thenReturn(view(AiQmsStatus.DRAFT));
        AiQmsWebDto.DraftRequest req = new AiQmsWebDto.DraftRequest(
                "REF-1", "1.0", "Name", null,
                "compliance", "design", "quality", "data", "risk",
                "pmm", "comm", "resource", "supplier", Set.of(), USER);
        mockMvc.perform(post("/api/v1/ai-act/qms").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void draft_invalidVersion_400() throws Exception {
        String body = "{\"reference\":\"REF-1\",\"version\":\"v1\",\"name\":\"n\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/qms").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_invalidRef_400() throws Exception {
        String body = "{\"reference\":\"lowercase\",\"version\":\"1.0\",\"name\":\"n\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/qms").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_missingActor_400() throws Exception {
        String body = "{\"reference\":\"REF-1\",\"version\":\"1.0\",\"name\":\"n\"}";
        mockMvc.perform(post("/api/v1/ai-act/qms").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_duplicate_409() throws Exception {
        when(service.draft(any())).thenThrow(new AiQmsStateException("Reference+version already used"));
        AiQmsWebDto.DraftRequest req = new AiQmsWebDto.DraftRequest(
                "REF-DUP", "1.0", "Name", null,
                "x", "x", "x", "x", "x", "x", "x", "x", "x", Set.of(), USER);
        mockMvc.perform(post("/api/v1/ai-act/qms").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(AiQmsStatus.DRAFT));
        AiQmsWebDto.EditRequest req = new AiQmsWebDto.EditRequest(
                "new", null, "x", "x", "x", "x", "x", "x", "x", null, null, Set.of(SYS));
        mockMvc.perform(put("/api/v1/ai-act/qms/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void approve_200() throws Exception {
        when(service.approve(eq(ID), any())).thenReturn(view(AiQmsStatus.APPROVED));
        mockMvc.perform(post("/api/v1/ai-act/qms/{id}/approve", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submittedByUserId\":\"" + USER + "\","
                                + "\"approvedByUserId\":\"" + APPROVER + "\","
                                + "\"approvalNotes\":\"ok\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void approve_segregationViolation_409() throws Exception {
        when(service.approve(eq(ID), any()))
                .thenThrow(new AiQmsStateException("segregation of duties"));
        mockMvc.perform(post("/api/v1/ai-act/qms/{id}/approve", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submittedByUserId\":\"" + USER + "\","
                                + "\"approvedByUserId\":\"" + USER + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void approve_missingApprover_400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-act/qms/{id}/approve", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submittedByUserId\":\"" + USER + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void putInForce_200() throws Exception {
        when(service.putInForce(ID)).thenReturn(view(AiQmsStatus.IN_FORCE));
        mockMvc.perform(post("/api/v1/ai-act/qms/{id}/put-in-force", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void supersede_200() throws Exception {
        when(service.supersede(eq(ID), any())).thenReturn(view(AiQmsStatus.SUPERSEDED));
        UUID newId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/ai-act/qms/{id}/supersede", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supersededByQmsId\":\"" + newId + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void supersede_missingId_400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-act/qms/{id}/supersede", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void archive_200() throws Exception {
        when(service.archive(eq(ID), any())).thenReturn(view(AiQmsStatus.ARCHIVED));
        mockMvc.perform(post("/api/v1/ai-act/qms/{id}/archive", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"retired\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void archive_blankReason_400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-act/qms/{id}/archive", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/ai-act/qms/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private AiQmsDto.View view(AiQmsStatus status) {
        return new AiQmsDto.View(
                ID, TENANT, "REF-1", "1.0", "Name", "desc",
                "compliance", "design", "quality", "data", "risk",
                "pmm", "comm", "resource", "supplier", Set.of(SYS), status,
                status != AiQmsStatus.DRAFT ? NOW : null,
                status != AiQmsStatus.DRAFT ? USER : null,
                status != AiQmsStatus.DRAFT ? NOW : null,
                status != AiQmsStatus.DRAFT ? APPROVER : null,
                null,
                status == AiQmsStatus.IN_FORCE || status == AiQmsStatus.SUPERSEDED ? NOW : null,
                status == AiQmsStatus.SUPERSEDED || status == AiQmsStatus.ARCHIVED ? NOW : null,
                status == AiQmsStatus.SUPERSEDED ? UUID.randomUUID() : null,
                status == AiQmsStatus.ARCHIVED ? "retired" : null,
                USER, NOW, NOW);
    }
}
