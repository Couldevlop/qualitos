package com.openlab.qualitos.quality.ropa.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.ropa.application.ProcessingActivityDto;
import com.openlab.qualitos.quality.ropa.application.ProcessingActivityService;
import com.openlab.qualitos.quality.ropa.domain.LawfulBasis;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityNotFoundException;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityStateException;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityStatus;
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
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = ProcessingActivityController.class)
class ProcessingActivityControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ProcessingActivityService service;
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
        when(service.list(null)).thenReturn(List.of(view(ProcessingActivityStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/gdpr/processing-activities"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_byStatus_200() throws Exception {
        when(service.list(ProcessingActivityStatus.ACTIVE))
                .thenReturn(List.of(view(ProcessingActivityStatus.ACTIVE)));
        mockMvc.perform(get("/api/v1/gdpr/processing-activities")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new ProcessingActivityNotFoundException(ID));
        mockMvc.perform(get("/api/v1/gdpr/processing-activities/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void create_201() throws Exception {
        when(service.create(any())).thenReturn(view(ProcessingActivityStatus.DRAFT));
        ProcessingActivityWebDto.CreateRequest req = new ProcessingActivityWebDto.CreateRequest(
                "ROPA-2026-001", "Customer CRM", "Manage customers",
                LawfulBasis.CONTRACT, null,
                "Acme Corp", "dpo@acme.com", null, null, null,
                Set.of("customers"), Set.of("identity"),
                false, null, Set.of("staff"), Set.of(), null,
                Set.of(), null, null, USER);
        mockMvc.perform(post("/api/v1/gdpr/processing-activities").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_lowercaseReference_400() throws Exception {
        String body = "{\"reference\":\"ropa-bad\",\"name\":\"n\",\"purposes\":\"p\","
                + "\"lawfulBasis\":\"CONTRACT\","
                + "\"controllerName\":\"c\",\"controllerContact\":\"x\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/processing-activities").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_blankPurposes_400() throws Exception {
        String body = "{\"reference\":\"ROPA-1\",\"name\":\"n\",\"purposes\":\"\","
                + "\"lawfulBasis\":\"CONTRACT\","
                + "\"controllerName\":\"c\",\"controllerContact\":\"x\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/processing-activities").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingActor_400() throws Exception {
        String body = "{\"reference\":\"ROPA-1\",\"name\":\"n\",\"purposes\":\"p\","
                + "\"lawfulBasis\":\"CONTRACT\","
                + "\"controllerName\":\"c\",\"controllerContact\":\"x\"}";
        mockMvc.perform(post("/api/v1/gdpr/processing-activities").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_invalidLawfulBasis_400() throws Exception {
        String body = "{\"reference\":\"ROPA-1\",\"name\":\"n\",\"purposes\":\"p\","
                + "\"lawfulBasis\":\"WHATEVER\","
                + "\"controllerName\":\"c\",\"controllerContact\":\"x\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/processing-activities").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_duplicateReference_409() throws Exception {
        when(service.create(any()))
                .thenThrow(new ProcessingActivityStateException("Reference already used"));
        ProcessingActivityWebDto.CreateRequest req = new ProcessingActivityWebDto.CreateRequest(
                "ROPA-DUP", "n", "p", LawfulBasis.CONTRACT, null,
                "c", "c@x", null, null, null,
                Set.of(), Set.of(), false, null,
                Set.of(), Set.of(), null, Set.of(), null, null, USER);
        mockMvc.perform(post("/api/v1/gdpr/processing-activities").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(ProcessingActivityStatus.DRAFT));
        ProcessingActivityWebDto.EditRequest req = new ProcessingActivityWebDto.EditRequest(
                "Updated", "New purposes", LawfulBasis.CONTRACT, null,
                "c", "c@x", null, null, null,
                Set.of(), Set.of(), false, null,
                Set.of(), Set.of(), null, Set.of(), null, null);
        mockMvc.perform(put("/api/v1/gdpr/processing-activities/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void edit_onActive_409() throws Exception {
        when(service.edit(eq(ID), any()))
                .thenThrow(new ProcessingActivityStateException("Only DRAFT can be edited"));
        ProcessingActivityWebDto.EditRequest req = new ProcessingActivityWebDto.EditRequest(
                "X", "p", LawfulBasis.CONTRACT, null, "c", "c@x", null, null, null,
                Set.of(), Set.of(), false, null,
                Set.of(), Set.of(), null, Set.of(), null, null);
        mockMvc.perform(put("/api/v1/gdpr/processing-activities/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void activate_200() throws Exception {
        when(service.activate(ID)).thenReturn(view(ProcessingActivityStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/gdpr/processing-activities/{id}/activate", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void archive_200() throws Exception {
        when(service.archive(ID)).thenReturn(view(ProcessingActivityStatus.ARCHIVED));
        mockMvc.perform(post("/api/v1/gdpr/processing-activities/{id}/archive", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/gdpr/processing-activities/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("ROPA-2026-001"))
                .thenReturn(view(ProcessingActivityStatus.ACTIVE));
        mockMvc.perform(get("/api/v1/gdpr/processing-activities/by-reference")
                        .param("reference", "ROPA-2026-001"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/gdpr/processing-activities/by-reference")
                        .param("reference", "lowercase-bad"))
                .andExpect(status().is4xxClientError());
    }

    private ProcessingActivityDto.View view(ProcessingActivityStatus status) {
        return new ProcessingActivityDto.View(
                ID, TENANT, "ROPA-2026-001", "Customer CRM",
                "Manage customer relationship",
                LawfulBasis.CONTRACT, null,
                "Acme Corp", "dpo@acme.com", null, null, null,
                Set.of("customers"), Set.of("identity"),
                false, null, Set.of("staff"), Set.of(), null,
                Set.of(), null, null,
                status,
                status == ProcessingActivityStatus.ACTIVE ? NOW : null,
                status == ProcessingActivityStatus.ARCHIVED ? NOW.plusSeconds(60) : null,
                USER, NOW, NOW);
    }
}
