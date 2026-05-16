package com.openlab.qualitos.quality.dpoappointments.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.dpoappointments.application.DpoAppointmentDto;
import com.openlab.qualitos.quality.dpoappointments.application.DpoAppointmentService;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentNotFoundException;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentStateException;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentStatus;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoType;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DpoAppointmentController.class)
class DpoAppointmentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DpoAppointmentService service;
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
        when(service.list(null)).thenReturn(List.of(view(DpoAppointmentStatus.PROPOSED)));
        mockMvc.perform(get("/api/v1/gdpr/dpo-appointments"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new DpoAppointmentNotFoundException(ID));
        mockMvc.perform(get("/api/v1/gdpr/dpo-appointments/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("DPO-2026-001"))
                .thenReturn(view(DpoAppointmentStatus.ACTIVE));
        mockMvc.perform(get("/api/v1/gdpr/dpo-appointments/by-reference")
                        .param("reference", "DPO-2026-001"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void findActive_200() throws Exception {
        when(service.findActiveByScope("GROUP"))
                .thenReturn(Optional.of(view(DpoAppointmentStatus.ACTIVE)));
        mockMvc.perform(get("/api/v1/gdpr/dpo-appointments/active")
                        .param("scope", "GROUP"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void findActive_missing_404() throws Exception {
        when(service.findActiveByScope("GROUP")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/gdpr/dpo-appointments/active")
                        .param("scope", "GROUP"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void findActive_invalidScope_400() throws Exception {
        mockMvc.perform(get("/api/v1/gdpr/dpo-appointments/active")
                        .param("scope", "lowercase"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void propose_201() throws Exception {
        when(service.propose(any())).thenReturn(view(DpoAppointmentStatus.PROPOSED));
        DpoAppointmentWebDto.ProposeRequest req = new DpoAppointmentWebDto.ProposeRequest(
                "DPO-2026-001", "Jane Doe", "dpo@example.com", null,
                DpoType.INTERNAL, null, null, "GROUP", Set.of(), USER);
        mockMvc.perform(post("/api/v1/gdpr/dpo-appointments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void propose_invalidEmail_400() throws Exception {
        String body = "{\"reference\":\"DPO-1\",\"dpoFullName\":\"J\","
                + "\"dpoEmail\":\"not-an-email\","
                + "\"dpoType\":\"INTERNAL\",\"scope\":\"GROUP\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/dpo-appointments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void propose_invalidScope_400() throws Exception {
        String body = "{\"reference\":\"DPO-1\",\"dpoFullName\":\"J\","
                + "\"dpoEmail\":\"dpo@x.com\",\"dpoType\":\"INTERNAL\","
                + "\"scope\":\"lowercase\",\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/dpo-appointments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void propose_missingActor_400() throws Exception {
        String body = "{\"reference\":\"DPO-1\",\"dpoFullName\":\"J\","
                + "\"dpoEmail\":\"dpo@x.com\",\"dpoType\":\"INTERNAL\","
                + "\"scope\":\"GROUP\"}";
        mockMvc.perform(post("/api/v1/gdpr/dpo-appointments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void propose_externalWithoutCompany_409() throws Exception {
        when(service.propose(any())).thenThrow(new DpoAppointmentStateException(
                "EXTERNAL DPO requires externalCompanyName"));
        DpoAppointmentWebDto.ProposeRequest req = new DpoAppointmentWebDto.ProposeRequest(
                "DPO-1", "J", "dpo@x.com", null,
                DpoType.EXTERNAL, null, null, "GROUP", Set.of(), USER);
        mockMvc.perform(post("/api/v1/gdpr/dpo-appointments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(DpoAppointmentStatus.PROPOSED));
        DpoAppointmentWebDto.EditRequest req = new DpoAppointmentWebDto.EditRequest(
                "Updated", "u@x.com", null, DpoType.INTERNAL, null, null, Set.of());
        mockMvc.perform(put("/api/v1/gdpr/dpo-appointments/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void activate_200() throws Exception {
        when(service.activate(eq(ID), any())).thenReturn(view(DpoAppointmentStatus.ACTIVE));
        DpoAppointmentWebDto.ActivateRequest req = new DpoAppointmentWebDto.ActivateRequest(
                NOW, NOW, "CNIL-2026-001");
        mockMvc.perform(post("/api/v1/gdpr/dpo-appointments/{id}/activate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void activate_missingRef_400() throws Exception {
        mockMvc.perform(post("/api/v1/gdpr/dpo-appointments/{id}/activate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"effectiveFrom\":\"" + NOW + "\","
                                + "\"regulatorNotifiedAt\":\"" + NOW + "\","
                                + "\"regulatorNotificationReference\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void end_200() throws Exception {
        when(service.end(eq(ID), any())).thenReturn(view(DpoAppointmentStatus.ENDED));
        mockMvc.perform(post("/api/v1/gdpr/dpo-appointments/{id}/end", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Fin mandat\",\"effectiveTo\":\"" + NOW + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void end_blankReason_400() throws Exception {
        mockMvc.perform(post("/api/v1/gdpr/dpo-appointments/{id}/end", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\",\"effectiveTo\":\"" + NOW + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void cancel_200() throws Exception {
        when(service.cancel(eq(ID), any())).thenReturn(view(DpoAppointmentStatus.CANCELLED));
        mockMvc.perform(post("/api/v1/gdpr/dpo-appointments/{id}/cancel", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Annulé\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void cancel_invalidState_409() throws Exception {
        when(service.cancel(eq(ID), any()))
                .thenThrow(new DpoAppointmentStateException("not allowed"));
        mockMvc.perform(post("/api/v1/gdpr/dpo-appointments/{id}/cancel", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/gdpr/dpo-appointments/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private DpoAppointmentDto.View view(DpoAppointmentStatus status) {
        return new DpoAppointmentDto.View(
                ID, TENANT, "DPO-2026-001",
                "Jane Doe", "dpo@example.com", null,
                DpoType.INTERNAL, null, null,
                "GROUP",
                status == DpoAppointmentStatus.ACTIVE
                        || status == DpoAppointmentStatus.ENDED ? NOW : null,
                status == DpoAppointmentStatus.ENDED ? NOW.plusSeconds(60) : null,
                status == DpoAppointmentStatus.ACTIVE
                        || status == DpoAppointmentStatus.ENDED ? NOW : null,
                status == DpoAppointmentStatus.ACTIVE
                        || status == DpoAppointmentStatus.ENDED ? "CNIL-2026-001" : null,
                Set.of(),
                status,
                status == DpoAppointmentStatus.ENDED || status == DpoAppointmentStatus.CANCELLED
                        ? "reason" : null,
                USER, NOW, NOW);
    }
}
