package com.openlab.qualitos.quality.crossbordertransfers.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.crossbordertransfers.application.CrossBorderTransferDto;
import com.openlab.qualitos.quality.crossbordertransfers.application.CrossBorderTransferService;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferNotFoundException;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferStateException;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferStatus;
import com.openlab.qualitos.quality.crossbordertransfers.domain.TransferMechanism;
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

@WebMvcTest(controllers = CrossBorderTransferController.class)
class CrossBorderTransferControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CrossBorderTransferService service;
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
        when(service.list(null)).thenReturn(List.of(view(CrossBorderTransferStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/gdpr/cross-border-transfers"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new CrossBorderTransferNotFoundException(ID));
        mockMvc.perform(get("/api/v1/gdpr/cross-border-transfers/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("CBT-2026-001"))
                .thenReturn(view(CrossBorderTransferStatus.ACTIVE));
        mockMvc.perform(get("/api/v1/gdpr/cross-border-transfers/by-reference")
                        .param("reference", "CBT-2026-001"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/gdpr/cross-border-transfers/by-reference")
                        .param("reference", "lowercase"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void create_201() throws Exception {
        when(service.create(any())).thenReturn(view(CrossBorderTransferStatus.DRAFT));
        CrossBorderTransferWebDto.CreateRequest req = new CrossBorderTransferWebDto.CreateRequest(
                "CBT-2026-001", "Acme Cloud Inc", null, "ops@acme.com",
                Set.of("US"), TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC 2021", null, null, Set.of(), Set.of(), Set.of(), USER);
        mockMvc.perform(post("/api/v1/gdpr/cross-border-transfers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_invalidRef_400() throws Exception {
        String body = "{\"reference\":\"lowercase\",\"recipientName\":\"A\","
                + "\"mechanism\":\"STANDARD_CONTRACTUAL_CLAUSES\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/cross-border-transfers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingMechanism_400() throws Exception {
        String body = "{\"reference\":\"CBT-1\",\"recipientName\":\"A\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/cross-border-transfers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_blankRecipient_400() throws Exception {
        String body = "{\"reference\":\"CBT-1\",\"recipientName\":\"\","
                + "\"mechanism\":\"STANDARD_CONTRACTUAL_CLAUSES\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/cross-border-transfers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingActor_400() throws Exception {
        String body = "{\"reference\":\"CBT-1\",\"recipientName\":\"A\","
                + "\"mechanism\":\"STANDARD_CONTRACTUAL_CLAUSES\"}";
        mockMvc.perform(post("/api/v1/gdpr/cross-border-transfers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_invalidMechanism_400() throws Exception {
        String body = "{\"reference\":\"CBT-1\",\"recipientName\":\"A\","
                + "\"mechanism\":\"WHATEVER\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/cross-border-transfers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_duplicate_409() throws Exception {
        when(service.create(any()))
                .thenThrow(new CrossBorderTransferStateException("Reference already used"));
        CrossBorderTransferWebDto.CreateRequest req = new CrossBorderTransferWebDto.CreateRequest(
                "CBT-DUP", "A", null, null, Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC", null, null, Set.of(), Set.of(), Set.of(), USER);
        mockMvc.perform(post("/api/v1/gdpr/cross-border-transfers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(CrossBorderTransferStatus.DRAFT));
        CrossBorderTransferWebDto.EditRequest req = new CrossBorderTransferWebDto.EditRequest(
                "Updated", null, null, Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC", null, null, Set.of(), Set.of(), Set.of());
        mockMvc.perform(put("/api/v1/gdpr/cross-border-transfers/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void activate_200() throws Exception {
        when(service.activate(ID)).thenReturn(view(CrossBorderTransferStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/gdpr/cross-border-transfers/{id}/activate", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void activate_invalidState_409() throws Exception {
        when(service.activate(ID))
                .thenThrow(new CrossBorderTransferStateException("safeguardsDescription required"));
        mockMvc.perform(post("/api/v1/gdpr/cross-border-transfers/{id}/activate", ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void suspend_200() throws Exception {
        when(service.suspend(eq(ID), any())).thenReturn(view(CrossBorderTransferStatus.SUSPENDED));
        mockMvc.perform(post("/api/v1/gdpr/cross-border-transfers/{id}/suspend", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Audit\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void suspend_blankReason_400() throws Exception {
        mockMvc.perform(post("/api/v1/gdpr/cross-border-transfers/{id}/suspend", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void terminate_200() throws Exception {
        when(service.terminate(eq(ID), any()))
                .thenReturn(view(CrossBorderTransferStatus.TERMINATED));
        mockMvc.perform(post("/api/v1/gdpr/cross-border-transfers/{id}/terminate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Fin\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/gdpr/cross-border-transfers/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private CrossBorderTransferDto.View view(CrossBorderTransferStatus status) {
        return new CrossBorderTransferDto.View(
                ID, TENANT, "CBT-2026-001",
                "Acme Cloud Inc", null, "ops@acme.com",
                Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC 2021", null, null,
                Set.of(), Set.of(), Set.of(),
                status,
                status == CrossBorderTransferStatus.ACTIVE
                        || status == CrossBorderTransferStatus.SUSPENDED
                        || status == CrossBorderTransferStatus.TERMINATED ? NOW : null,
                status == CrossBorderTransferStatus.TERMINATED ? NOW.plusSeconds(60) : null,
                status == CrossBorderTransferStatus.SUSPENDED ? NOW : null,
                status == CrossBorderTransferStatus.SUSPENDED ? "Audit" : null,
                status == CrossBorderTransferStatus.TERMINATED ? "Fin" : null,
                USER, NOW, NOW);
    }
}
