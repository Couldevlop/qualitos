package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.common.StepUpGuard;
import com.openlab.qualitos.quality.common.StepUpRequiredException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Façade HTTP du rapport d'audit : chaque refus a son code, parce que chacun
 * appelle une conduite différente à l'écran (compléter, clôturer, se
 * réauthentifier).
 */
@Tag("web")
@WebMvcTest(controllers = AuditReportController.class)
class AuditReportControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuditReportService service;
    @MockitoBean StepUpGuard stepUp;

    static final UUID PLAN = UUID.randomUUID();

    private static AuditReportDto.View view(AuditReportStatus status) {
        return new AuditReportDto.View(UUID.randomUUID(), PLAN, status, "S", null, null,
                AuditConclusion.FAVORABLE, new AuditReportDto.KeyFigures(1, 0, 0, 0, 0, 1, 90.0),
                null, null, null, null, Instant.parse("2026-10-14T09:00:00Z"));
    }

    @Test @WithMockUser
    void lecture_200() throws Exception {
        when(service.get(PLAN)).thenReturn(view(AuditReportStatus.DRAFT));
        mockMvc.perform(get("/api/v1/audits/plans/{id}/report", PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.keyFigures.majorNc").value(1));
    }

    @Test @WithMockUser
    void brouillon_200() throws Exception {
        when(service.saveDraft(eq(PLAN), any())).thenReturn(view(AuditReportStatus.DRAFT));
        mockMvc.perform(put("/api/v1/audits/plans/{id}/report", PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\"S\",\"conclusion\":\"FAVORABLE\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void brouillon_conclusionInconnue_400() throws Exception {
        mockMvc.perform(put("/api/v1/audits/plans/{id}/report", PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conclusion\":\"PEUT_ETRE\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser
    void brouillon_rapportEmis_409() throws Exception {
        when(service.saveDraft(eq(PLAN), any())).thenThrow(new AuditStateException("issued"));
        mockMvc.perform(put("/api/v1/audits/plans/{id}/report", PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void emission_200_apresSecondFacteur() throws Exception {
        when(service.issue(PLAN)).thenReturn(view(AuditReportStatus.ISSUED));
        mockMvc.perform(post("/api/v1/audits/plans/{id}/report/issue", PLAN).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"));
        verify(stepUp).require(any());
    }

    @Test @WithMockUser
    void emission_sansSecondFacteur_403_etRienNEstEmis() throws Exception {
        doThrow(new StepUpRequiredException("émettre un rapport d'audit")).when(stepUp).require(any());
        mockMvc.perform(post("/api/v1/audits/plans/{id}/report/issue", PLAN).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser
    void emission_incomplete_422() throws Exception {
        when(service.issue(PLAN)).thenThrow(new AuditReportIncompleteException("no summary"));
        mockMvc.perform(post("/api/v1/audits/plans/{id}/report/issue", PLAN).with(csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/audit-report-incomplete"));
    }

    @Test
    void sansAuthentification_401() throws Exception {
        mockMvc.perform(get("/api/v1/audits/plans/{id}/report", PLAN))
                .andExpect(status().isUnauthorized());
    }
}
