package com.openlab.qualitos.quality.consent.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.consent.application.ConsentDto;
import com.openlab.qualitos.quality.consent.application.ConsentService;
import com.openlab.qualitos.quality.consent.domain.ConsentNotFoundException;
import com.openlab.qualitos.quality.consent.domain.ConsentSource;
import com.openlab.qualitos.quality.consent.domain.ConsentStateException;
import com.openlab.qualitos.quality.consent.domain.ConsentStatus;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ConsentController.class)
class ConsentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ConsentService service;
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
    void grant_201_andHidesPII() throws Exception {
        when(service.grant(any())).thenReturn(view(ConsentStatus.GRANTED, true));
        ConsentWebDto.GrantRequest req = new ConsentWebDto.GrantRequest(
                "user@example.com", "u***@e.com",
                "marketing", "v1", ConsentSource.WEB_FORM,
                "https://evidence", "1.2.3.4", "UA", USER, null);
        mockMvc.perform(post("/api/v1/gdpr/consents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectIdentifierHash").exists())
                // OWASP A02 : la PII en clair ne doit JAMAIS apparaître dans la réponse.
                .andExpect(jsonPath("$.subjectIdentifier").doesNotExist());
    }

    @Test @WithMockUser
    void grant_blankIdentifier_400() throws Exception {
        String body = "{\"subjectIdentifier\":\"\",\"purposeCode\":\"marketing\","
                + "\"purposeVersion\":\"v1\",\"source\":\"WEB_FORM\",\"grantedByUserId\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/consents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void grant_invalidPurposeCode_400() throws Exception {
        String body = "{\"subjectIdentifier\":\"x\",\"purposeCode\":\"BAD CODE\","
                + "\"purposeVersion\":\"v1\",\"source\":\"WEB_FORM\",\"grantedByUserId\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/consents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void grant_invalidSource_400() throws Exception {
        String body = "{\"subjectIdentifier\":\"x\",\"purposeCode\":\"marketing\","
                + "\"purposeVersion\":\"v1\",\"source\":\"FAX\",\"grantedByUserId\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/consents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void grant_missingActor_400() throws Exception {
        String body = "{\"subjectIdentifier\":\"x\",\"purposeCode\":\"marketing\","
                + "\"purposeVersion\":\"v1\",\"source\":\"WEB_FORM\"}";
        mockMvc.perform(post("/api/v1/gdpr/consents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void withdraw_200() throws Exception {
        when(service.withdraw(eq(ID), any()))
                .thenReturn(view(ConsentStatus.WITHDRAWN, false));
        mockMvc.perform(post("/api/v1/gdpr/consents/{id}/withdraw", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actorUserId\":\"" + USER + "\",\"reason\":\"r\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void withdraw_invalidState_409() throws Exception {
        when(service.withdraw(eq(ID), any()))
                .thenThrow(new ConsentStateException("already withdrawn"));
        mockMvc.perform(post("/api/v1/gdpr/consents/{id}/withdraw", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actorUserId\":\"" + USER + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new ConsentNotFoundException(ID));
        mockMvc.perform(get("/api/v1/gdpr/consents/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void search_200() throws Exception {
        when(service.findBySubject("user@example.com"))
                .thenReturn(List.of(view(ConsentStatus.GRANTED, true)));
        mockMvc.perform(get("/api/v1/gdpr/consents/search")
                        .param("subjectIdentifier", "user@example.com"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void search_blank_400() throws Exception {
        mockMvc.perform(get("/api/v1/gdpr/consents/search")
                        .param("subjectIdentifier", " "))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void active_found_200() throws Exception {
        when(service.findActiveByPurpose("user@example.com", "marketing"))
                .thenReturn(Optional.of(view(ConsentStatus.GRANTED, true)));
        mockMvc.perform(get("/api/v1/gdpr/consents/active")
                        .param("subjectIdentifier", "user@example.com")
                        .param("purposeCode", "marketing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test @WithMockUser
    void active_notFound_404() throws Exception {
        when(service.findActiveByPurpose("user@example.com", "marketing"))
                .thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/gdpr/consents/active")
                        .param("subjectIdentifier", "user@example.com")
                        .param("purposeCode", "marketing"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void active_invalidPurpose_400() throws Exception {
        mockMvc.perform(get("/api/v1/gdpr/consents/active")
                        .param("subjectIdentifier", "x")
                        .param("purposeCode", "BAD CODE"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void byPurpose_200() throws Exception {
        when(service.listByPurpose("marketing"))
                .thenReturn(List.of(view(ConsentStatus.GRANTED, true)));
        mockMvc.perform(get("/api/v1/gdpr/consents/by-purpose")
                        .param("purposeCode", "marketing"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void expireDue_200() throws Exception {
        when(service.expireDue(200)).thenReturn(3);
        mockMvc.perform(post("/api/v1/gdpr/consents/expire-due").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(3));
    }

    @Test @WithMockUser
    void expireDue_outOfRange_400() throws Exception {
        mockMvc.perform(post("/api/v1/gdpr/consents/expire-due")
                        .param("limit", "0").with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    private ConsentDto.View view(ConsentStatus status, boolean active) {
        return new ConsentDto.View(
                ID, TENANT,
                "a".repeat(64), "u***@e.com",
                "marketing", "v1",
                ConsentSource.WEB_FORM, "https://evidence", "1.2.3.4", "UA",
                USER, NOW, null,
                status, status == ConsentStatus.WITHDRAWN ? NOW.plusSeconds(60) : null,
                status == ConsentStatus.WITHDRAWN ? USER : null,
                status == ConsentStatus.WITHDRAWN ? "user request" : null,
                NOW, active);
    }
}
