package com.openlab.qualitos.quality.nis2measures.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.nis2measures.application.Nis2MeasureDto;
import com.openlab.qualitos.quality.nis2measures.application.Nis2MeasureService;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureCategory;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureNotFoundException;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureStateException;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureStatus;
import com.openlab.qualitos.quality.nis2measures.domain.ResidualRiskRating;
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
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = Nis2MeasureController.class)
class Nis2MeasureControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean Nis2MeasureService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID REVIEWER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(null)).thenReturn(List.of(view(Nis2MeasureStatus.PLANNED)));
        mockMvc.perform(get("/api/v1/nis2/risk-measures"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void byCategory_200() throws Exception {
        when(service.listByCategory(Nis2MeasureCategory.CRYPTOGRAPHY))
                .thenReturn(List.of(view(Nis2MeasureStatus.IMPLEMENTED)));
        mockMvc.perform(get("/api/v1/nis2/risk-measures/by-category")
                        .param("category", "CRYPTOGRAPHY"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void byCategory_invalidEnum_400() throws Exception {
        mockMvc.perform(get("/api/v1/nis2/risk-measures/by-category")
                        .param("category", "WHATEVER"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new Nis2MeasureNotFoundException(ID));
        mockMvc.perform(get("/api/v1/nis2/risk-measures/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("M-2026-001"))
                .thenReturn(view(Nis2MeasureStatus.VERIFIED));
        mockMvc.perform(get("/api/v1/nis2/risk-measures/by-reference")
                        .param("reference", "M-2026-001"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/nis2/risk-measures/by-reference")
                        .param("reference", "lowercase"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void reviewOverdue_200() throws Exception {
        when(service.reviewOverdue(100))
                .thenReturn(List.of(view(Nis2MeasureStatus.VERIFIED)));
        mockMvc.perform(get("/api/v1/nis2/risk-measures/review-overdue"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void reviewOverdue_outOfRange_400() throws Exception {
        mockMvc.perform(get("/api/v1/nis2/risk-measures/review-overdue")
                        .param("limit", "0"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void plan_201() throws Exception {
        when(service.plan(any())).thenReturn(view(Nis2MeasureStatus.PLANNED));
        Nis2MeasureWebDto.PlanRequest req = new Nis2MeasureWebDto.PlanRequest(
                "M-2026-001", Nis2MeasureCategory.MFA_AND_COMMUNICATIONS,
                "MFA admin", null, USER, 2, ResidualRiskRating.LOW, null, 365,
                Set.of(), Set.of(), Set.of(), null, USER);
        mockMvc.perform(post("/api/v1/nis2/risk-measures").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void plan_invalidRef_400() throws Exception {
        String body = "{\"reference\":\"lowercase\",\"category\":\"CRYPTOGRAPHY\","
                + "\"title\":\"t\",\"maturityLevel\":2,\"residualRiskRating\":\"LOW\","
                + "\"reviewIntervalDays\":365,\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/nis2/risk-measures").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void plan_maturityOutOfRange_400() throws Exception {
        String body = "{\"reference\":\"M-1\",\"category\":\"CRYPTOGRAPHY\","
                + "\"title\":\"t\",\"maturityLevel\":6,\"residualRiskRating\":\"LOW\","
                + "\"reviewIntervalDays\":365,\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/nis2/risk-measures").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void plan_reviewIntervalOutOfRange_400() throws Exception {
        String body = "{\"reference\":\"M-1\",\"category\":\"CRYPTOGRAPHY\","
                + "\"title\":\"t\",\"maturityLevel\":2,\"residualRiskRating\":\"LOW\","
                + "\"reviewIntervalDays\":29,\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/nis2/risk-measures").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void plan_missingCategory_400() throws Exception {
        String body = "{\"reference\":\"M-1\",\"title\":\"t\",\"maturityLevel\":2,"
                + "\"residualRiskRating\":\"LOW\",\"reviewIntervalDays\":365,"
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/nis2/risk-measures").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void plan_criticalWithoutJustification_409() throws Exception {
        when(service.plan(any())).thenThrow(new Nis2MeasureStateException("CRITICAL requires"));
        Nis2MeasureWebDto.PlanRequest req = new Nis2MeasureWebDto.PlanRequest(
                "M-1", Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                USER, 1, ResidualRiskRating.CRITICAL, null, 365,
                Set.of(), Set.of(), Set.of(), null, USER);
        mockMvc.perform(post("/api/v1/nis2/risk-measures").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(Nis2MeasureStatus.PLANNED));
        Nis2MeasureWebDto.EditRequest req = new Nis2MeasureWebDto.EditRequest(
                "Updated", null, USER, 3, ResidualRiskRating.MEDIUM, null, 180,
                Set.of(), Set.of(), Set.of(), null);
        mockMvc.perform(put("/api/v1/nis2/risk-measures/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void start_200() throws Exception {
        when(service.startImplementation(ID)).thenReturn(view(Nis2MeasureStatus.IN_PROGRESS));
        mockMvc.perform(post("/api/v1/nis2/risk-measures/{id}/start", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void implemented_200() throws Exception {
        when(service.markImplemented(ID)).thenReturn(view(Nis2MeasureStatus.IMPLEMENTED));
        mockMvc.perform(post("/api/v1/nis2/risk-measures/{id}/implemented", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void verify_200() throws Exception {
        when(service.verify(eq(ID), any())).thenReturn(view(Nis2MeasureStatus.VERIFIED));
        mockMvc.perform(post("/api/v1/nis2/risk-measures/{id}/verify", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewedByUserId\":\"" + REVIEWER + "\",\"reviewedAt\":\""
                                + NOW + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void verify_missingReviewer_400() throws Exception {
        mockMvc.perform(post("/api/v1/nis2/risk-measures/{id}/verify", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewedAt\":\"" + NOW + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void review_200() throws Exception {
        when(service.review(eq(ID), any())).thenReturn(view(Nis2MeasureStatus.VERIFIED));
        mockMvc.perform(post("/api/v1/nis2/risk-measures/{id}/review", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewedByUserId\":\"" + REVIEWER + "\",\"reviewedAt\":\""
                                + NOW + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void deprecate_200() throws Exception {
        when(service.deprecate(ID)).thenReturn(view(Nis2MeasureStatus.DEPRECATED));
        mockMvc.perform(post("/api/v1/nis2/risk-measures/{id}/deprecate", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/nis2/risk-measures/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private Nis2MeasureDto.View view(Nis2MeasureStatus status) {
        return new Nis2MeasureDto.View(
                ID, TENANT, "M-2026-001",
                Nis2MeasureCategory.MFA_AND_COMMUNICATIONS, "MFA admin", null,
                status, USER, 2, ResidualRiskRating.LOW, null, 365,
                NOW, status == Nis2MeasureStatus.DEPRECATED ? NOW.plusSeconds(60) : null,
                status == Nis2MeasureStatus.VERIFIED ? NOW : null,
                status == Nis2MeasureStatus.VERIFIED ? REVIEWER : null,
                status == Nis2MeasureStatus.VERIFIED ? NOW.plusSeconds(86400L * 365) : null,
                Set.of(), Set.of(), Set.of(), null, USER, NOW, NOW,
                false, false);
    }
}
