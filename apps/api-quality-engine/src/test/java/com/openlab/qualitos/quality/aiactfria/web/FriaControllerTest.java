package com.openlab.qualitos.quality.aiactfria.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.aiactfria.application.FriaDto;
import com.openlab.qualitos.quality.aiactfria.application.FriaService;
import com.openlab.qualitos.quality.aiactfria.domain.FriaNotFoundException;
import com.openlab.qualitos.quality.aiactfria.domain.FriaStateException;
import com.openlab.qualitos.quality.aiactfria.domain.FriaStatus;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = FriaController.class)
class FriaControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FriaService service;
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
        when(service.list(null)).thenReturn(List.of(view(FriaStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/ai-act/fria")).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_byStatus_200() throws Exception {
        when(service.list(FriaStatus.APPROVED)).thenReturn(List.of(view(FriaStatus.APPROVED)));
        mockMvc.perform(get("/api/v1/ai-act/fria").param("status", "APPROVED"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_invalidStatus_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/fria").param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void bySystem_200() throws Exception {
        when(service.listByAiSystem(SYS)).thenReturn(List.of(view(FriaStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/ai-act/fria/by-system")
                        .param("aiSystemId", SYS.toString()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_200() throws Exception {
        when(service.get(ID)).thenReturn(view(FriaStatus.DRAFT));
        mockMvc.perform(get("/api/v1/ai-act/fria/{id}", ID)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new FriaNotFoundException(ID));
        mockMvc.perform(get("/api/v1/ai-act/fria/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("REF-1")).thenReturn(view(FriaStatus.DRAFT));
        mockMvc.perform(get("/api/v1/ai-act/fria/by-reference").param("reference", "REF-1"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/fria/by-reference").param("reference", "lowercase"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void draft_201() throws Exception {
        when(service.draft(any())).thenReturn(view(FriaStatus.DRAFT));
        FriaWebDto.DraftRequest req = new FriaWebDto.DraftRequest(
                "REF-1", SYS, "process", null, "categories", "risks",
                null, null, null, USER);
        mockMvc.perform(post("/api/v1/ai-act/fria").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void draft_invalidRef_400() throws Exception {
        String body = "{\"reference\":\"lowercase\",\"aiSystemId\":\"" + SYS + "\","
                + "\"processDescription\":\"p\",\"affectedPersonsCategories\":\"c\","
                + "\"specificRisks\":\"r\",\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/fria").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_missingSystem_400() throws Exception {
        String body = "{\"reference\":\"REF-1\",\"processDescription\":\"p\","
                + "\"affectedPersonsCategories\":\"c\",\"specificRisks\":\"r\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/fria").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_missingActor_400() throws Exception {
        String body = "{\"reference\":\"REF-1\",\"aiSystemId\":\"" + SYS + "\","
                + "\"processDescription\":\"p\",\"affectedPersonsCategories\":\"c\","
                + "\"specificRisks\":\"r\"}";
        mockMvc.perform(post("/api/v1/ai-act/fria").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_duplicate_409() throws Exception {
        when(service.draft(any())).thenThrow(new FriaStateException("Reference already used"));
        FriaWebDto.DraftRequest req = new FriaWebDto.DraftRequest(
                "REF-DUP", SYS, "process", null, "c", "r", null, null, null, USER);
        mockMvc.perform(post("/api/v1/ai-act/fria").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(FriaStatus.DRAFT));
        FriaWebDto.EditRequest req = new FriaWebDto.EditRequest(
                "p2", null, "c", "r", "m", "o", "comp");
        mockMvc.perform(put("/api/v1/ai-act/fria/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void submit_200() throws Exception {
        when(service.submit(eq(ID), any())).thenReturn(view(FriaStatus.SUBMITTED));
        mockMvc.perform(post("/api/v1/ai-act/fria/{id}/submit", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submittedByUserId\":\"" + USER + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void submit_missingMeasures_409() throws Exception {
        when(service.submit(eq(ID), any()))
                .thenThrow(new FriaStateException("mitigationMeasures required"));
        mockMvc.perform(post("/api/v1/ai-act/fria/{id}/submit", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submittedByUserId\":\"" + USER + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void approve_200() throws Exception {
        when(service.approve(eq(ID), any())).thenReturn(view(FriaStatus.APPROVED));
        mockMvc.perform(post("/api/v1/ai-act/fria/{id}/approve", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvedByUserId\":\"" + APPROVER + "\",\"approvalNotes\":\"ok\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void approve_selfApproval_409() throws Exception {
        when(service.approve(eq(ID), any()))
                .thenThrow(new FriaStateException("segregation of duties"));
        mockMvc.perform(post("/api/v1/ai-act/fria/{id}/approve", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvedByUserId\":\"" + USER + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void returnToDraft_200() throws Exception {
        when(service.returnToDraft(eq(ID), any())).thenReturn(view(FriaStatus.DRAFT));
        mockMvc.perform(post("/api/v1/ai-act/fria/{id}/return", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"more detail\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void returnToDraft_blankReason_400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-act/fria/{id}/return", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void archive_200() throws Exception {
        when(service.archive(eq(ID), any())).thenReturn(view(FriaStatus.ARCHIVED));
        mockMvc.perform(post("/api/v1/ai-act/fria/{id}/archive", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"done\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void archive_blankReason_400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-act/fria/{id}/archive", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/ai-act/fria/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private FriaDto.View view(FriaStatus status) {
        return new FriaDto.View(
                ID, TENANT, "REF-1", SYS, "process", "1y", "cat", "risks",
                "m", "o", "comp", status,
                status == FriaStatus.SUBMITTED || status == FriaStatus.APPROVED
                        || status == FriaStatus.ARCHIVED ? NOW : null,
                status == FriaStatus.SUBMITTED || status == FriaStatus.APPROVED
                        || status == FriaStatus.ARCHIVED ? USER : null,
                status == FriaStatus.APPROVED || status == FriaStatus.ARCHIVED ? NOW : null,
                status == FriaStatus.APPROVED || status == FriaStatus.ARCHIVED ? APPROVER : null,
                null,
                status == FriaStatus.ARCHIVED ? NOW : null,
                status == FriaStatus.ARCHIVED ? "done" : null,
                USER, NOW, NOW);
    }
}
