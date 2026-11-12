package com.openlab.qualitos.quality.spc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.aigateway.AiGatewayException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("web")
@WebMvcTest(controllers = SpcController.class)
class SpcControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SpcService spcService;
    // Le controleur exige le droit d ouvrir une CAPA quand openCapa=true.
    @MockitoBean AuthorizationService droits;
    private final ObjectMapper om = new ObjectMapper();

    @Test @WithMockUser
    void analyze_returns200_withMappedResult() throws Exception {
        when(spcService.analyze(any())).thenReturn(new SpcDto.AnalyzeResponse(
                3, true,
                new SpcDto.Limits(2.0, 0.5, 3.5, 0.5, true),
                List.of(new SpcDto.Violation("RULE_1", "Hors 3σ", "1 point au-delà",
                        List.of(2), "CRITICAL"))));

        mockMvc.perform(post("/api/v1/ai/spc/analyze").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("values", List.of(1.0, 2.0, 3.0)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outOfControl").value(true))
                .andExpect(jsonPath("$.limits.ucl").value(3.5))
                .andExpect(jsonPath("$.violations[0].rule").value("RULE_1"))
                .andExpect(jsonPath("$.violations[0].pointIndices[0]").value(2));
    }

    @Test @WithMockUser
    void analyze_emptyValues_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/spc/analyze").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("values", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void analyze_gatewayDown_returns502() throws Exception {
        when(spcService.analyze(any())).thenThrow(new AiGatewayException("ai-service down"));
        mockMvc.perform(post("/api/v1/ai/spc/analyze").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("values", List.of(1.0, 2.0)))))
                .andExpect(status().isBadGateway());
    }

    // ---- prévision de sortie des limites (§6.5) ----

    @Test @WithMockUser
    void forecast_returns200_withMappedResult() throws Exception {
        when(spcService.forecast(any())).thenReturn(prevision(true, 4, "upper"));

        mockMvc.perform(post("/api/v1/ai/spc/forecast").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "values", List.of(10.0, 10.6, 11.2, 11.8), "horizon", 12))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.willBreach").value(true))
                .andExpect(jsonPath("$.firstBreachStep").value(4))
                .andExpect(jsonPath("$.side").value("upper"))
                .andExpect(jsonPath("$.confidence").value("high"))
                .andExpect(jsonPath("$.points[0].pBreach").value(0.2));
    }

    /** Trois points ne datent pas une dérive : la requête est refusée à l'entrée. */
    @Test @WithMockUser
    void forecast_seriesTooShort_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/spc/forecast").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("values", List.of(1.0, 2.0, 3.0)))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void forecast_horizonOutOfBounds_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/spc/forecast").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "values", List.of(10.0, 10.6, 11.2, 11.8), "horizon", 61))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void forecast_thresholdOutOfBounds_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/spc/forecast").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "values", List.of(10.0, 10.6, 11.2, 11.8), "threshold", 1.4))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void forecast_gatewayDown_returns502() throws Exception {
        when(spcService.forecast(any())).thenThrow(new AiGatewayException("ai-service down"));

        mockMvc.perform(post("/api/v1/ai/spc/forecast").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "values", List.of(10.0, 10.6, 11.2, 11.8)))))
                .andExpect(status().isBadGateway());
    }

    @Test @WithMockUser
    void forecastKpi_returns200_withSeriesAndForecast() throws Exception {
        UUID kpi = UUID.randomUUID();
        when(spcService.forecastKpi(eq(kpi), eq(30), isNull(), eq(false)))
                .thenReturn(new SpcDto.KpiForecastResponse(
                        kpi, "OEE", "Rendement", "%",
                        List.of("2026-11-01T00:00:00Z"), List.of(10.0),
                        prevision(true, 2, "lower"), null));

        mockMvc.perform(post("/api/v1/ai/spc/kpi/" + kpi + "/forecast").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpiCode").value("OEE"))
                .andExpect(jsonPath("$.forecast.side").value("lower"))
                .andExpect(jsonPath("$.capaId").doesNotExist());
    }

    @Test @WithMockUser
    void forecastKpi_openCapa_passesTheFlagThrough() throws Exception {
        UUID kpi = UUID.randomUUID();
        UUID capa = UUID.randomUUID();
        // Ouvrir un dossier par ricochet exige le droit de l'ouvrir en face.
        when(droits.has(Permission.CAPA_CREATE)).thenReturn(true);
        when(spcService.forecastKpi(eq(kpi), eq(10), eq(8), eq(true)))
                .thenReturn(new SpcDto.KpiForecastResponse(
                        kpi, "OEE", "Rendement", "%", List.of(), List.of(),
                        prevision(true, 2, "upper"), capa));

        mockMvc.perform(post("/api/v1/ai/spc/kpi/" + kpi + "/forecast").with(csrf())
                        .param("limit", "10").param("horizon", "8").param("openCapa", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capaId").value(capa.toString()));
    }

    // ---- la porte de service par laquelle on ouvrait une CAPA sans y avoir droit ----

    /*
     * Deux routes acceptent openCapa=true et, si le procede est hors controle ou
     * annonce sortant, OUVRENT un dossier CAPA (ADR 0016, ADR 0110). Or ouvrir un
     * dossier demande capa.create partout ailleurs, et ces routes ne demandaient
     * rien : n'importe quel authentifie ouvrait par ici les dossiers que
     * CapaController lui refuse.
     *
     * Le droit n'est exige que SI l'appelant demande l'ouverture. Fermer les
     * routes entieres serait plus simple et faux : LIRE la carte n'est le secret
     * de personne, et c'est l'ecran des indicateurs qui les appelle pour
     * l'afficher.
     */

    @Test @WithMockUser(roles = "USER")
    void sansLeDroitDOuvrirUneCapa_laCarteRefuseDEnOuvrirUne() throws Exception {
        UUID kpi = UUID.randomUUID();
        when(droits.has(Permission.CAPA_CREATE)).thenReturn(false);

        mockMvc.perform(post("/api/v1/ai/spc/kpi/" + kpi + "/analyze").with(csrf())
                        .param("openCapa", "true"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/ai/spc/kpi/" + kpi + "/forecast").with(csrf())
                        .param("openCapa", "true"))
                .andExpect(status().isForbidden());

        // Et le refus arrive AVANT l'analyse : on ne paie pas un appel d'inference
        // pour un geste qu'on va refuser.
        verify(spcService, never()).analyzeKpi(any(), anyInt(), anyBoolean());
        verify(spcService, never()).forecastKpi(any(), anyInt(), any(), anyBoolean());
    }

    @Test @WithMockUser(roles = "USER")
    void sansCeDroit_laCarteSeLitQuandMeme() throws Exception {
        UUID kpi = UUID.randomUUID();
        when(droits.has(Permission.CAPA_CREATE)).thenReturn(false);

        mockMvc.perform(post("/api/v1/ai/spc/kpi/" + kpi + "/analyze").with(csrf()))
                .andExpect(status().isOk());

        verify(spcService).analyzeKpi(kpi, 30, false);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void avecLeDroit_lOuvertureEstTransmise() throws Exception {
        UUID kpi = UUID.randomUUID();
        when(droits.has(Permission.CAPA_CREATE)).thenReturn(true);

        mockMvc.perform(post("/api/v1/ai/spc/kpi/" + kpi + "/analyze").with(csrf())
                        .param("openCapa", "true"))
                .andExpect(status().isOk());

        verify(spcService).analyzeKpi(kpi, 30, true);
    }

    private static SpcDto.ForecastResponse prevision(boolean sortie, Integer etape, String cote) {
        return new SpcDto.ForecastResponse(
                4, 12, 0.5, sortie, etape, cote, 0.4, 0.2, 0.95, "high", "holt_linear",
                new SpcDto.Limits(10.0, 1.0, 13.0, 7.0, true),
                List.of(new SpcDto.ForecastPoint(1, 12.0, 10.0, 14.0, 0.2, 0.0, 0.2),
                        new SpcDto.ForecastPoint(2, 13.5, 11.0, 16.0, 0.8, 0.0, 0.8)));
    }
}
