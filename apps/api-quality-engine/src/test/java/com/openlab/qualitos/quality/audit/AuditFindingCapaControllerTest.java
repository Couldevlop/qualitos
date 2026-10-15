package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.capa.CapaDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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
@WebMvcTest(controllers = AuditFindingCapaController.class)
class AuditFindingCapaControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuditFindingCapaService service;

    static final UUID PLAN = UUID.randomUUID();
    static final UUID FINDING = UUID.randomUUID();
    static final UUID CAPA = UUID.randomUUID();
    static final String URL = "/api/v1/audits/plans/{planId}/findings/{findingId}/capa";

    private static String corps() {
        return "{\"title\":\"Reprendre la procédure\",\"type\":\"CORRECTIVE\",\"criticity\":\"HIGH\","
                + "\"ownerId\":\"" + UUID.randomUUID() + "\",\"ownerName\":\"a.diallo@client.test\","
                + "\"dueDate\":\"" + LocalDate.now().plusDays(10) + "\"}";
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void ouvreLaCapaDuConstat() throws Exception {
        when(service.open(eq(PLAN), eq(FINDING), any(CapaDto.OpenCaseRequest.class)))
                .thenReturn(new AuditFindingCapaService.FindingCapa(FINDING, CAPA, "AUD-2026-0003 / constat 2"));

        mockMvc.perform(post(URL, PLAN, FINDING).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.capaId").value(CAPA.toString()))
                .andExpect(jsonPath("$.sourceRef").value("AUD-2026-0003 / constat 2"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void unCorpsIncompletEstRefuseAvantLeService() throws Exception {
        mockMvc.perform(post(URL, PLAN, FINDING).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).open(any(), any(), any());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void unConstatDejaTraiteRend409() throws Exception {
        when(service.open(eq(PLAN), eq(FINDING), any()))
                .thenThrow(new AuditStateException("This finding already has its CAPA"));
        mockMvc.perform(post(URL, PLAN, FINDING).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "AUDITOR")
    void releverUnConstatNeSuffitPasAOuvrirUneCapa() throws Exception {
        mockMvc.perform(post(URL, PLAN, FINDING).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isForbidden());
        verify(service, never()).open(any(), any(), any());
    }
}
