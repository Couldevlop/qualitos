package com.openlab.qualitos.quality.audit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("web")
@WebMvcTest(controllers = AuditFindingNcController.class)
class AuditFindingNcControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuditFindingNcService service;

    static final UUID PLAN = UUID.randomUUID();
    static final UUID FINDING = UUID.randomUUID();
    static final UUID NC = UUID.randomUUID();
    static final String URL = "/api/v1/audits/plans/{planId}/findings/{findingId}/nc";

    private static String corps() {
        return "{\"title\":\"Enregistrements de formation non signés\","
                + "\"category\":\"DOCUMENTATION\",\"subcategoryCode\":\"DOC-SIGN\"}";
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void declareLEcartDuConstat() throws Exception {
        when(service.open(eq(PLAN), eq(FINDING), any(AuditDto.DeclareNcRequest.class)))
                .thenReturn(new AuditFindingNcService.FindingNc(
                        FINDING, NC, "NC-2026-0042", "AUD-2026-0003 / constat 2"));

        mockMvc.perform(post(URL, PLAN, FINDING).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ncId").value(NC.toString()))
                .andExpect(jsonPath("$.ncReference").value("NC-2026-0042"))
                .andExpect(jsonPath("$.sourceRef").value("AUD-2026-0003 / constat 2"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void unCorpsSansCategorieEstRefuseAvantLeService() throws Exception {
        // La catégorie n'est pas devinable depuis le constat : un écart sans
        // catégorie ne se range nulle part dans le registre.
        mockMvc.perform(post(URL, PLAN, FINDING).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).open(any(), any(), any());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void unConstatDejaInstruitRend409() throws Exception {
        when(service.open(eq(PLAN), eq(FINDING), any()))
                .thenThrow(new AuditStateException("This finding already has its non-conformity"));
        mockMvc.perform(post(URL, PLAN, FINDING).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "AUDITOR")
    void lAuditeurDeclareLEcartQuIlReleve() throws Exception {
        // Déclarer l'écart relève du droit `nc.create`, que l'auditeur porte :
        // c'est lui qui constate, et le registre doit pouvoir l'enregistrer au
        // moment de l'audit plutôt qu'à la relecture du rapport.
        when(service.open(eq(PLAN), eq(FINDING), any()))
                .thenReturn(new AuditFindingNcService.FindingNc(FINDING, NC, "NC-1", "AUD-1 / constat 1"));
        mockMvc.perform(post(URL, PLAN, FINDING).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser(roles = "EXTERNAL_AUDITOR")
    void unAuditeurTiersNInscritRienAuRegistre() throws Exception {
        mockMvc.perform(post(URL, PLAN, FINDING).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isForbidden());
        verify(service, never()).open(any(), any(), any());
    }
}
