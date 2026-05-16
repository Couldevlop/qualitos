package com.openlab.qualitos.quality.aipmm.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.aipmm.application.PmmPlanDto;
import com.openlab.qualitos.quality.aipmm.application.PmmPlanService;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanNotFoundException;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanStateException;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanStatus;
import com.openlab.qualitos.quality.aipmm.domain.PmmReviewFrequency;
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
@WebMvcTest(controllers = PmmPlanController.class)
class PmmPlanControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PmmPlanService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID REVIEWER = UUID.randomUUID();
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
        when(service.list(null)).thenReturn(List.of(view(PmmPlanStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/ai-act/pmm")).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_byStatus_200() throws Exception {
        when(service.list(PmmPlanStatus.ACTIVE)).thenReturn(List.of(view(PmmPlanStatus.ACTIVE)));
        mockMvc.perform(get("/api/v1/ai-act/pmm").param("status", "ACTIVE"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_invalidStatus_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/pmm").param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void bySystem_200() throws Exception {
        when(service.listByAiSystem(SYS)).thenReturn(List.of(view(PmmPlanStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/ai-act/pmm/by-system").param("aiSystemId", SYS.toString()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void overdueReviews_200() throws Exception {
        when(service.listOverdueReviews(200)).thenReturn(List.of(view(PmmPlanStatus.ACTIVE)));
        mockMvc.perform(get("/api/v1/ai-act/pmm/overdue-reviews"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void overdueReviews_outOfRange_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/pmm/overdue-reviews").param("limit", "0"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void get_200() throws Exception {
        when(service.get(ID)).thenReturn(view(PmmPlanStatus.DRAFT));
        mockMvc.perform(get("/api/v1/ai-act/pmm/{id}", ID)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new PmmPlanNotFoundException(ID));
        mockMvc.perform(get("/api/v1/ai-act/pmm/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("REF-1")).thenReturn(view(PmmPlanStatus.DRAFT));
        mockMvc.perform(get("/api/v1/ai-act/pmm/by-reference").param("reference", "REF-1"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/pmm/by-reference").param("reference", "lowercase"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void draft_201() throws Exception {
        when(service.draft(any())).thenReturn(view(PmmPlanStatus.DRAFT));
        PmmPlanWebDto.DraftRequest req = new PmmPlanWebDto.DraftRequest(
                "REF-1", SYS, "Name", null, "metrics", "method",
                PmmReviewFrequency.MONTHLY, "resp", "trigger", "qms-1", USER);
        mockMvc.perform(post("/api/v1/ai-act/pmm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void draft_invalidRef_400() throws Exception {
        String body = "{\"reference\":\"lowercase\",\"aiSystemId\":\"" + SYS + "\","
                + "\"name\":\"n\",\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/pmm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_missingSystem_400() throws Exception {
        String body = "{\"reference\":\"REF-1\",\"name\":\"n\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/pmm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_missingActor_400() throws Exception {
        String body = "{\"reference\":\"REF-1\",\"aiSystemId\":\"" + SYS + "\","
                + "\"name\":\"n\"}";
        mockMvc.perform(post("/api/v1/ai-act/pmm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_duplicate_409() throws Exception {
        when(service.draft(any())).thenThrow(new PmmPlanStateException("Reference already used"));
        PmmPlanWebDto.DraftRequest req = new PmmPlanWebDto.DraftRequest(
                "REF-DUP", SYS, "Name", null, "m", "c",
                PmmReviewFrequency.MONTHLY, null, null, null, USER);
        mockMvc.perform(post("/api/v1/ai-act/pmm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(PmmPlanStatus.DRAFT));
        PmmPlanWebDto.EditRequest req = new PmmPlanWebDto.EditRequest(
                "new", null, "m", "c", PmmReviewFrequency.WEEKLY, null, null, null);
        mockMvc.perform(put("/api/v1/ai-act/pmm/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void activate_200() throws Exception {
        when(service.activate(ID)).thenReturn(view(PmmPlanStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/ai-act/pmm/{id}/activate", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void activate_invalidState_409() throws Exception {
        when(service.activate(ID))
                .thenThrow(new PmmPlanStateException("metricsMonitored required"));
        mockMvc.perform(post("/api/v1/ai-act/pmm/{id}/activate", ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void recordReview_200() throws Exception {
        when(service.recordReview(eq(ID), any())).thenReturn(view(PmmPlanStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/ai-act/pmm/{id}/record-review", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewedByUserId\":\"" + REVIEWER + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void recordReview_missingActor_400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-act/pmm/{id}/record-review", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void suspend_200() throws Exception {
        when(service.suspend(eq(ID), any())).thenReturn(view(PmmPlanStatus.SUSPENDED));
        mockMvc.perform(post("/api/v1/ai-act/pmm/{id}/suspend", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"maintenance\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void suspend_blankReason_400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-act/pmm/{id}/suspend", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void close_200() throws Exception {
        when(service.close(eq(ID), any())).thenReturn(view(PmmPlanStatus.CLOSED));
        mockMvc.perform(post("/api/v1/ai-act/pmm/{id}/close", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"end of life\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void close_blankReason_400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-act/pmm/{id}/close", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/ai-act/pmm/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private PmmPlanDto.View view(PmmPlanStatus status) {
        return new PmmPlanDto.View(
                ID, TENANT, "REF-1", SYS, "Name", "desc",
                "metrics", "method", PmmReviewFrequency.MONTHLY,
                "resp", "trigger", "qms-1", status,
                status != PmmPlanStatus.DRAFT ? NOW : null,
                null, null, null,
                status == PmmPlanStatus.SUSPENDED ? NOW : null,
                status == PmmPlanStatus.SUSPENDED ? "maintenance" : null,
                status == PmmPlanStatus.CLOSED ? NOW : null,
                status == PmmPlanStatus.CLOSED ? "end of life" : null,
                USER, NOW, NOW);
    }
}
