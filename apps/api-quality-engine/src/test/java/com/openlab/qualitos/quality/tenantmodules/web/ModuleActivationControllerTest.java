package com.openlab.qualitos.quality.tenantmodules.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationDto;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationService;
import com.openlab.qualitos.quality.tenantmodules.domain.ActivationStatus;
import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationNotFoundException;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationStateException;
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
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = ModuleActivationController.class)
class ModuleActivationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ModuleActivationService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID ACTOR = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant FUTURE = Instant.parse("2026-06-16T10:00:00Z");

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void catalog_200() throws Exception {
        when(service.listCatalog()).thenReturn(List.of(
                new ModuleActivationDto.CatalogEntryView("pdca", "PDCA", "methodes",
                        BillingTier.FREE, List.of(), true)));
        mockMvc.perform(get("/api/v1/tenant-modules/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("pdca"));
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.listForCurrentTenant()).thenReturn(List.of(view(ActivationStatus.ACTIVE)));
        mockMvc.perform(get("/api/v1/tenant-modules/activations"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new ModuleActivationNotFoundException(ID.toString()));
        mockMvc.perform(get("/api/v1/tenant-modules/activations/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void startTrial_201() throws Exception {
        when(service.startTrial(any())).thenReturn(view(ActivationStatus.TRIAL));
        ModuleActivationWebDto.StartTrialRequest req =
                new ModuleActivationWebDto.StartTrialRequest("kpi", FUTURE);
        mockMvc.perform(post("/api/v1/tenant-modules/activations/trial").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void startTrial_invalidCode_400() throws Exception {
        // H2 : un "actor" dans le corps est ignoré (jamais lu) ; ici on vérifie la
        // validation du moduleCode invariante de cette suppression.
        String body = "{\"moduleCode\":\"BAD\",\"trialEndsAt\":\"" + FUTURE + "\"}";
        mockMvc.perform(post("/api/v1/tenant-modules/activations/trial").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void startTrial_actorInBodyIsIgnored_201() throws Exception {
        // H2 : même si le client envoie un "actor" falsifié dans le corps, la requête
        // reste valide (la propriété inconnue est tolérée) et l'acteur réel est dérivé
        // du JWT côté service — jamais de ce champ.
        when(service.startTrial(any())).thenReturn(view(ActivationStatus.TRIAL));
        String body = "{\"moduleCode\":\"kpi\",\"trialEndsAt\":\"" + FUTURE
                + "\",\"actor\":\"" + UUID.randomUUID() + "\"}";
        mockMvc.perform(post("/api/v1/tenant-modules/activations/trial").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void activate_201() throws Exception {
        when(service.activate(any())).thenReturn(view(ActivationStatus.ACTIVE));
        ModuleActivationWebDto.ActivateRequest req =
                new ModuleActivationWebDto.ActivateRequest("kpi", FUTURE);
        mockMvc.perform(post("/api/v1/tenant-modules/activations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void activate_invalidState_409() throws Exception {
        when(service.activate(any()))
                .thenThrow(new ModuleActivationStateException("tier insufficient"));
        ModuleActivationWebDto.ActivateRequest req =
                new ModuleActivationWebDto.ActivateRequest("blockchain", FUTURE);
        mockMvc.perform(post("/api/v1/tenant-modules/activations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void convert_200() throws Exception {
        when(service.convertTrial(eq(ID), any())).thenReturn(view(ActivationStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/tenant-modules/activations/{id}/convert", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expiresAt\":\"" + FUTURE + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void suspend_200() throws Exception {
        // H2 : plus de corps de requête (l'acteur vient du JWT).
        when(service.suspend(eq(ID), any())).thenReturn(view(ActivationStatus.SUSPENDED));
        mockMvc.perform(post("/api/v1/tenant-modules/activations/{id}/suspend", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void resume_200() throws Exception {
        when(service.resume(eq(ID), any())).thenReturn(view(ActivationStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/tenant-modules/activations/{id}/resume", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void disable_200() throws Exception {
        when(service.disable(eq(ID), any())).thenReturn(view(ActivationStatus.DISABLED));
        mockMvc.perform(post("/api/v1/tenant-modules/activations/{id}/disable", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void expire_200() throws Exception {
        when(service.expire(eq(ID), any())).thenReturn(view(ActivationStatus.EXPIRED));
        mockMvc.perform(post("/api/v1/tenant-modules/activations/{id}/expire", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void changeTier_200() throws Exception {
        when(service.changeTier(eq(ID), any())).thenReturn(view(ActivationStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/tenant-modules/activations/{id}/tier", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newTier\":\"PRO\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void configure_200() throws Exception {
        when(service.configure(eq(ID), any())).thenReturn(view(ActivationStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/tenant-modules/activations/{id}/configure", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configurationJson\":\"{}\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void summary_200() throws Exception {
        when(service.summary()).thenReturn(new ModuleActivationDto.TenantModuleSummary(
                TENANT, BillingTier.STANDARD, 3, 2, 0, 2, 1, 0, 0, List.of()));
        mockMvc.perform(get("/api/v1/tenant-modules/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCount").value(2));
    }

    @Test @WithMockUser
    void isEnabled_200() throws Exception {
        when(service.isEnabled("kpi")).thenReturn(true);
        mockMvc.perform(get("/api/v1/tenant-modules/enabled/{code}", "kpi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test @WithMockUser
    void expireDue_200() throws Exception {
        when(service.expireDue(200)).thenReturn(5);
        mockMvc.perform(post("/api/v1/tenant-modules/expire-due").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(5));
    }

    @Test @WithMockUser
    void expireDue_outOfRange_400() throws Exception {
        mockMvc.perform(post("/api/v1/tenant-modules/expire-due")
                        .param("limit", "0").with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    private ModuleActivationDto.ActivationView view(ActivationStatus status) {
        return new ModuleActivationDto.ActivationView(
                ID, TENANT, "kpi", status, status == ActivationStatus.ACTIVE
                        || status == ActivationStatus.TRIAL,
                BillingTier.STANDARD, null, null, FUTURE,
                Instant.now(), ACTOR, Instant.now(), ACTOR, Instant.now());
    }
}
