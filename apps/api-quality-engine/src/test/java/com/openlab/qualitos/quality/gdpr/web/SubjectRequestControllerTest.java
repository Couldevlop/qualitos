package com.openlab.qualitos.quality.gdpr.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.gdpr.application.SubjectRequestDto;
import com.openlab.qualitos.quality.gdpr.application.SubjectRequestService;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestNotFoundException;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestStateException;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestStatus;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestType;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SubjectRequestController.class)
class SubjectRequestControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SubjectRequestService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID HANDLER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(null)).thenReturn(List.of(view(SubjectRequestStatus.RECEIVED)));
        mockMvc.perform(get("/api/v1/gdpr/subject-requests"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_withStatus_200() throws Exception {
        when(service.list(SubjectRequestStatus.IN_PROGRESS))
                .thenReturn(List.of(view(SubjectRequestStatus.IN_PROGRESS)));
        mockMvc.perform(get("/api/v1/gdpr/subject-requests").param("status", "IN_PROGRESS"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new SubjectRequestNotFoundException(ID));
        mockMvc.perform(get("/api/v1/gdpr/subject-requests/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void receive_201() throws Exception {
        when(service.receive(any())).thenReturn(view(SubjectRequestStatus.RECEIVED));
        SubjectRequestWebDto.ReceiveRequest req = new SubjectRequestWebDto.ReceiveRequest(
                SubjectRequestType.ACCESS, "user@example.com", "u***@e.com", USER);
        mockMvc.perform(post("/api/v1/gdpr/subject-requests").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                // OWASP A02 : la réponse ne contient JAMAIS l'identifiant en clair.
                .andExpect(jsonPath("$.subjectIdentifierHash").exists())
                .andExpect(jsonPath("$.subjectIdentifier").doesNotExist());
    }

    @Test @WithMockUser
    void receive_blankIdentifier_400() throws Exception {
        String body = "{\"type\":\"ACCESS\",\"subjectIdentifier\":\"\",\"requestedByUserId\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/subject-requests").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void receive_missingType_400() throws Exception {
        String body = "{\"subjectIdentifier\":\"x\",\"requestedByUserId\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/subject-requests").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void receive_invalidType_400() throws Exception {
        String body = "{\"type\":\"UNKNOWN\",\"subjectIdentifier\":\"x\",\"requestedByUserId\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/subject-requests").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void start_200() throws Exception {
        when(service.startProcessing(eq(ID), any())).thenReturn(view(SubjectRequestStatus.IN_PROGRESS));
        mockMvc.perform(post("/api/v1/gdpr/subject-requests/{id}/start", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handledByUserId\":\"" + HANDLER + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void complete_200() throws Exception {
        when(service.complete(eq(ID), any())).thenReturn(view(SubjectRequestStatus.COMPLETED));
        mockMvc.perform(post("/api/v1/gdpr/subject-requests/{id}/complete", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\":\"done\",\"handledByUserId\":\""
                                + HANDLER + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void complete_invalidState_409() throws Exception {
        when(service.complete(eq(ID), any()))
                .thenThrow(new SubjectRequestStateException("not in progress"));
        mockMvc.perform(post("/api/v1/gdpr/subject-requests/{id}/complete", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\":\"done\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void reject_200() throws Exception {
        when(service.reject(eq(ID), any())).thenReturn(view(SubjectRequestStatus.REJECTED));
        mockMvc.perform(post("/api/v1/gdpr/subject-requests/{id}/reject", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"out of scope\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void extend_200() throws Exception {
        when(service.extendDeadline(eq(ID), any())).thenReturn(view(SubjectRequestStatus.RECEIVED));
        mockMvc.perform(post("/api/v1/gdpr/subject-requests/{id}/extend", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newDeadline\":\"2026-07-15T10:00:00Z\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void extend_invalidState_409() throws Exception {
        when(service.extendDeadline(eq(ID), any()))
                .thenThrow(new SubjectRequestStateException("already extended"));
        mockMvc.perform(post("/api/v1/gdpr/subject-requests/{id}/extend", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newDeadline\":\"2026-07-15T10:00:00Z\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void overdue_200() throws Exception {
        when(service.overdue(100)).thenReturn(List.of(view(SubjectRequestStatus.RECEIVED)));
        mockMvc.perform(get("/api/v1/gdpr/subject-requests/overdue"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void overdue_outOfRange_400() throws Exception {
        mockMvc.perform(get("/api/v1/gdpr/subject-requests/overdue").param("limit", "0"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void search_200() throws Exception {
        when(service.findBySubjectIdentifier("user@example.com"))
                .thenReturn(List.of(view(SubjectRequestStatus.RECEIVED)));
        mockMvc.perform(get("/api/v1/gdpr/subject-requests/search")
                        .param("subjectIdentifier", "user@example.com"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void search_blank_400() throws Exception {
        mockMvc.perform(get("/api/v1/gdpr/subject-requests/search")
                        .param("subjectIdentifier", "  "))
                .andExpect(status().is4xxClientError());
    }

    private SubjectRequestDto.View view(SubjectRequestStatus status) {
        return new SubjectRequestDto.View(
                ID, TENANT, SubjectRequestType.ACCESS,
                "h".repeat(64), "u***@e.com",
                status, NOW, NOW.plusSeconds(86400L * 30), false,
                null, null, null, null, null,
                USER, HANDLER, NOW, false);
    }
}
