package com.openlab.qualitos.quality.privacynotices.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.privacynotices.application.PrivacyNoticeDto;
import com.openlab.qualitos.quality.privacynotices.application.PrivacyNoticeService;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeNotFoundException;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeStateException;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeStatus;
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
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = PrivacyNoticeController.class)
class PrivacyNoticeControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PrivacyNoticeService service;
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
        when(service.list(null)).thenReturn(List.of(view(PrivacyNoticeStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/gdpr/privacy-notices"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_byStatus_200() throws Exception {
        when(service.list(PrivacyNoticeStatus.PUBLISHED))
                .thenReturn(List.of(view(PrivacyNoticeStatus.PUBLISHED)));
        mockMvc.perform(get("/api/v1/gdpr/privacy-notices")
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new PrivacyNoticeNotFoundException(ID));
        mockMvc.perform(get("/api/v1/gdpr/privacy-notices/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void findPublished_found_200() throws Exception {
        when(service.findPublished("PN-CUSTOMERS", "fr"))
                .thenReturn(Optional.of(view(PrivacyNoticeStatus.PUBLISHED)));
        mockMvc.perform(get("/api/v1/gdpr/privacy-notices/published")
                        .param("reference", "PN-CUSTOMERS").param("language", "fr"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void findPublished_missing_404() throws Exception {
        when(service.findPublished("PN-X", "fr")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/gdpr/privacy-notices/published")
                        .param("reference", "PN-X").param("language", "fr"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void findPublished_invalidLanguage_400() throws Exception {
        mockMvc.perform(get("/api/v1/gdpr/privacy-notices/published")
                        .param("reference", "PN-1").param("language", "FR"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void versions_200() throws Exception {
        when(service.versions("PN-CUSTOMERS"))
                .thenReturn(List.of(view(PrivacyNoticeStatus.PUBLISHED),
                        view(PrivacyNoticeStatus.ARCHIVED)));
        mockMvc.perform(get("/api/v1/gdpr/privacy-notices/versions")
                        .param("reference", "PN-CUSTOMERS"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void create_201() throws Exception {
        when(service.create(any())).thenReturn(view(PrivacyNoticeStatus.DRAFT));
        PrivacyNoticeWebDto.CreateRequest req = new PrivacyNoticeWebDto.CreateRequest(
                "PN-CUSTOMERS", "1.0", "fr", "Mention",
                "résumé", "contenu", Set.of(),
                null, null, null, USER);
        mockMvc.perform(post("/api/v1/gdpr/privacy-notices").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_invalidLanguage_400() throws Exception {
        String body = "{\"reference\":\"PN-1\",\"version\":\"1.0\",\"language\":\"FR\","
                + "\"title\":\"T\",\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/privacy-notices").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_invalidReference_400() throws Exception {
        String body = "{\"reference\":\"lowercase\",\"version\":\"1.0\",\"language\":\"fr\","
                + "\"title\":\"T\",\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/privacy-notices").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_blankTitle_400() throws Exception {
        String body = "{\"reference\":\"PN-1\",\"version\":\"1.0\",\"language\":\"fr\","
                + "\"title\":\"\",\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/privacy-notices").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingActor_400() throws Exception {
        String body = "{\"reference\":\"PN-1\",\"version\":\"1.0\",\"language\":\"fr\","
                + "\"title\":\"T\"}";
        mockMvc.perform(post("/api/v1/gdpr/privacy-notices").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_duplicateVersion_409() throws Exception {
        when(service.create(any()))
                .thenThrow(new PrivacyNoticeStateException("Version already exists"));
        PrivacyNoticeWebDto.CreateRequest req = new PrivacyNoticeWebDto.CreateRequest(
                "PN-DUP", "1.0", "fr", "T", null, null, Set.of(),
                null, null, null, USER);
        mockMvc.perform(post("/api/v1/gdpr/privacy-notices").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(PrivacyNoticeStatus.DRAFT));
        PrivacyNoticeWebDto.EditRequest req = new PrivacyNoticeWebDto.EditRequest(
                "Updated", "s", "c", Set.of(), null, null, null);
        mockMvc.perform(put("/api/v1/gdpr/privacy-notices/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void publish_200() throws Exception {
        when(service.publish(eq(ID), any())).thenReturn(view(PrivacyNoticeStatus.PUBLISHED));
        mockMvc.perform(post("/api/v1/gdpr/privacy-notices/{id}/publish", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishedByUserId\":\"" + USER + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void publish_missingActor_400() throws Exception {
        mockMvc.perform(post("/api/v1/gdpr/privacy-notices/{id}/publish", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void publish_notDraft_409() throws Exception {
        when(service.publish(eq(ID), any()))
                .thenThrow(new PrivacyNoticeStateException("Only DRAFT notices can be published"));
        mockMvc.perform(post("/api/v1/gdpr/privacy-notices/{id}/publish", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishedByUserId\":\"" + USER + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void archive_200() throws Exception {
        when(service.archive(ID)).thenReturn(view(PrivacyNoticeStatus.ARCHIVED));
        mockMvc.perform(post("/api/v1/gdpr/privacy-notices/{id}/archive", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/gdpr/privacy-notices/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private PrivacyNoticeDto.View view(PrivacyNoticeStatus status) {
        return new PrivacyNoticeDto.View(
                ID, TENANT, "PN-CUSTOMERS", "1.0", "fr",
                "Mention", "résumé", "contenu",
                Set.of(), null, null, null,
                status,
                status == PrivacyNoticeStatus.PUBLISHED ? NOW : null,
                status == PrivacyNoticeStatus.ARCHIVED ? NOW.plusSeconds(60) : null,
                status == PrivacyNoticeStatus.PUBLISHED ? NOW : null,
                status == PrivacyNoticeStatus.PUBLISHED ? USER : null,
                USER, NOW, NOW);
    }
}
