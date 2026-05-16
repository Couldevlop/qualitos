package com.openlab.qualitos.quality.processoragreements.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.processoragreements.application.ProcessorAgreementDto;
import com.openlab.qualitos.quality.processoragreements.application.ProcessorAgreementService;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementNotFoundException;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementStateException;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementStatus;
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

@WebMvcTest(controllers = ProcessorAgreementController.class)
class ProcessorAgreementControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ProcessorAgreementService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(null)).thenReturn(List.of(view(ProcessorAgreementStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/gdpr/processor-agreements"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new ProcessorAgreementNotFoundException(ID));
        mockMvc.perform(get("/api/v1/gdpr/processor-agreements/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("DPA-2026-001"))
                .thenReturn(view(ProcessorAgreementStatus.ACTIVE));
        mockMvc.perform(get("/api/v1/gdpr/processor-agreements/by-reference")
                        .param("reference", "DPA-2026-001"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/gdpr/processor-agreements/by-reference")
                        .param("reference", "lowercase"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void create_201() throws Exception {
        when(service.create(any())).thenReturn(view(ProcessorAgreementStatus.DRAFT));
        ProcessorAgreementWebDto.CreateRequest req = new ProcessorAgreementWebDto.CreateRequest(
                "DPA-2026-001", "Acme Corp", null, "ops@acme.com", null, "US",
                "Cloud hosting", Set.of(), Set.of(), Set.of(),
                null, null, null, null, null,
                null, 72, false, null, null, USER);
        mockMvc.perform(post("/api/v1/gdpr/processor-agreements").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_invalidRef_400() throws Exception {
        String body = "{\"reference\":\"lowercase\",\"processorName\":\"Acme\","
                + "\"servicesDescription\":\"S\",\"breachNotificationCommitmentHours\":72,"
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/processor-agreements").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_breachHoursOutOfRange_400() throws Exception {
        String body = "{\"reference\":\"DPA-1\",\"processorName\":\"Acme\","
                + "\"servicesDescription\":\"S\",\"breachNotificationCommitmentHours\":0,"
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/processor-agreements").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingActor_400() throws Exception {
        String body = "{\"reference\":\"DPA-1\",\"processorName\":\"Acme\","
                + "\"servicesDescription\":\"S\",\"breachNotificationCommitmentHours\":72}";
        mockMvc.perform(post("/api/v1/gdpr/processor-agreements").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_invalidCountry_400() throws Exception {
        String body = "{\"reference\":\"DPA-1\",\"processorName\":\"Acme\","
                + "\"processorCountry\":\"USA\","
                + "\"servicesDescription\":\"S\",\"breachNotificationCommitmentHours\":72,"
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/processor-agreements").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_duplicate_409() throws Exception {
        when(service.create(any()))
                .thenThrow(new ProcessorAgreementStateException("Reference already used"));
        ProcessorAgreementWebDto.CreateRequest req = new ProcessorAgreementWebDto.CreateRequest(
                "DPA-DUP", "Acme", null, null, null, null,
                "S", Set.of(), Set.of(), Set.of(),
                null, null, null, null, null,
                null, 72, false, null, null, USER);
        mockMvc.perform(post("/api/v1/gdpr/processor-agreements").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(ProcessorAgreementStatus.DRAFT));
        ProcessorAgreementWebDto.EditRequest req = new ProcessorAgreementWebDto.EditRequest(
                "Updated", null, "ops@acme.com", null, "US", "S",
                Set.of(), Set.of(), Set.of(),
                null, null, NOW, NOW, null, null, 24, true, null, null);
        mockMvc.perform(put("/api/v1/gdpr/processor-agreements/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void activate_200() throws Exception {
        when(service.activate(ID)).thenReturn(view(ProcessorAgreementStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/gdpr/processor-agreements/{id}/activate", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void activate_invalidState_409() throws Exception {
        when(service.activate(ID))
                .thenThrow(new ProcessorAgreementStateException("signedAt required"));
        mockMvc.perform(post("/api/v1/gdpr/processor-agreements/{id}/activate", ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void terminate_200() throws Exception {
        when(service.terminate(eq(ID), any()))
                .thenReturn(view(ProcessorAgreementStatus.TERMINATED));
        mockMvc.perform(post("/api/v1/gdpr/processor-agreements/{id}/terminate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"end of contract\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void terminate_blankReason_400() throws Exception {
        mockMvc.perform(post("/api/v1/gdpr/processor-agreements/{id}/terminate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/gdpr/processor-agreements/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void expireDue_200() throws Exception {
        when(service.expireDue(200)).thenReturn(2);
        mockMvc.perform(post("/api/v1/gdpr/processor-agreements/expire-due").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(2));
    }

    @Test @WithMockUser
    void expireDue_outOfRange_400() throws Exception {
        mockMvc.perform(post("/api/v1/gdpr/processor-agreements/expire-due")
                        .param("limit", "0").with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    private ProcessorAgreementDto.View view(ProcessorAgreementStatus status) {
        return new ProcessorAgreementDto.View(
                ID, TENANT, "DPA-2026-001",
                "Acme Corp", null, "ops@acme.com", null, "US", "Cloud",
                Set.of(), Set.of(), Set.of(),
                null, null,
                status == ProcessorAgreementStatus.ACTIVE ? NOW : null,
                status == ProcessorAgreementStatus.ACTIVE ? NOW : null,
                null, null, 72, false, null, null,
                status,
                status == ProcessorAgreementStatus.TERMINATED ? NOW : null,
                status == ProcessorAgreementStatus.TERMINATED ? "end" : null,
                USER, NOW, NOW, false);
    }
}
