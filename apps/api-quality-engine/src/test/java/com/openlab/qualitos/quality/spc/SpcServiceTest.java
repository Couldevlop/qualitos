package com.openlab.qualitos.quality.spc;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.kpi.KpiDto;
import com.openlab.qualitos.quality.kpi.KpiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpcServiceTest {

    @Mock AiGatewayClient ai;
    @Mock KpiService kpiService;
    @Mock CapaService capaService;
    @Mock CapaCaseRepository capaCaseRepo;
    @InjectMocks SpcService service;

    static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static final UUID KPI = UUID.randomUUID();

    @AfterEach
    void clr() { TenantContext.clear(); }

    // ===== analyze (saisie directe) =====

    @Test
    void analyze_mapsFullGatewayResponse() {
        when(ai.detectSpc(eq(List.of(1.0, 2.0, 3.0)), isNull(), isNull())).thenReturn(Map.of(
                "n", 3,
                "out_of_control", true,
                "limits", Map.of(
                        "center_line", 2.0, "sigma", 0.5,
                        "ucl", 3.5, "lcl", 0.5, "estimated", true),
                "violations", List.of(Map.of(
                        "rule", "RULE_1",
                        "title", "Point hors limites 3σ",
                        "description", "1 point au-delà de l'UCL",
                        "point_indices", List.of(2),
                        "severity", "CRITICAL"))));

        SpcDto.AnalyzeResponse r = service.analyze(new SpcDto.AnalyzeRequest(List.of(1.0, 2.0, 3.0), null, null));

        assertThat(r.n()).isEqualTo(3);
        assertThat(r.outOfControl()).isTrue();
        assertThat(r.limits().centerLine()).isEqualTo(2.0);
        assertThat(r.limits().ucl()).isEqualTo(3.5);
        assertThat(r.limits().estimated()).isTrue();
        assertThat(r.violations()).singleElement().satisfies(v -> {
            assertThat(v.rule()).isEqualTo("RULE_1");
            assertThat(v.pointIndices()).containsExactly(2);
            assertThat(v.severity()).isEqualTo("CRITICAL");
        });
    }

    @Test
    void analyze_toleratesMissingOrMistypedFields() {
        when(ai.detectSpc(eq(List.of(5.0)), isNull(), isNull())).thenReturn(Map.of(
                "limits", "not-a-map",
                "violations", "not-a-list"));

        SpcDto.AnalyzeResponse r = service.analyze(new SpcDto.AnalyzeRequest(List.of(5.0), null, null));

        assertThat(r.n()).isZero();
        assertThat(r.outOfControl()).isFalse();
        assertThat(r.limits().centerLine()).isZero();
        assertThat(r.limits().estimated()).isFalse();
        assertThat(r.violations()).isEmpty();
    }

    @Test
    void analyze_passesBaselineThrough() {
        when(ai.detectSpc(eq(List.of(1.0, 2.0)), eq(10.0), eq(2.0))).thenReturn(Map.of());

        service.analyze(new SpcDto.AnalyzeRequest(List.of(1.0, 2.0), 10.0, 2.0));

        ArgumentCaptor<Double> center = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> sigma = ArgumentCaptor.forClass(Double.class);
        verify(ai).detectSpc(eq(List.of(1.0, 2.0)), center.capture(), sigma.capture());
        assertThat(center.getValue()).isEqualTo(10.0);
        assertThat(sigma.getValue()).isEqualTo(2.0);
    }

    // ===== analyzeKpi (série depuis kpi_measurements + CAPA) =====

    @Test
    void analyzeKpi_inControl_mapsSeries_noCapa() {
        TenantContext.setTenantId(TENANT.toString());
        stubSeries(List.of(10.0, 10.1, 9.9, 10.0));
        when(ai.detectSpc(any(), isNull(), isNull())).thenReturn(okMap(false, List.of()));

        SpcDto.KpiSpcResponse r = service.analyzeKpi(KPI, 30, true);

        assertThat(r.kpiCode()).isEqualTo("OEE");
        assertThat(r.values()).containsExactly(10.0, 10.1, 9.9, 10.0);
        assertThat(r.periods()).hasSize(4);
        assertThat(r.analysis().outOfControl()).isFalse();
        assertThat(r.capaId()).isNull();
        verify(capaService, never()).createCase(any());
    }

    @Test
    void analyzeKpi_outOfControl_openCapa_createsCapa() {
        TenantContext.setTenantId(TENANT.toString());
        stubSeries(List.of(10.0, 10.1, 9.9, 25.0));
        when(ai.detectSpc(any(), isNull(), isNull())).thenReturn(okMap(true,
                List.of(Map.of("rule", "NELSON_1", "title", "Hors 3σ", "description", "…",
                        "point_indices", List.of(3), "severity", "high"))));
        when(capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(
                eq(TENANT), eq(CapaSourceType.SPC_ALERT), anyString(), any())).thenReturn(false);
        UUID capaId = UUID.randomUUID();
        when(capaService.createCase(any())).thenReturn(caseResponse(capaId));

        SpcDto.KpiSpcResponse r = service.analyzeKpi(KPI, 30, true);

        assertThat(r.capaId()).isEqualTo(capaId);
        ArgumentCaptor<CapaDto.CreateCaseRequest> req = ArgumentCaptor.forClass(CapaDto.CreateCaseRequest.class);
        verify(capaService).createCase(req.capture());
        assertThat(req.getValue().sourceType()).isEqualTo(CapaSourceType.SPC_ALERT);
        assertThat(req.getValue().type()).isEqualTo(CapaType.CORRECTIVE);
        assertThat(req.getValue().criticity()).isEqualTo(CapaCriticity.HIGH); // severity "high"
        assertThat(req.getValue().sourceRef()).isEqualTo("kpi:" + KPI);
    }

    @Test
    void analyzeKpi_outOfControl_activeCapaExists_antiSpam_noNewCapa() {
        TenantContext.setTenantId(TENANT.toString());
        stubSeries(List.of(10.0, 10.1, 9.9, 25.0));
        when(ai.detectSpc(any(), isNull(), isNull())).thenReturn(okMap(true,
                List.of(Map.of("rule", "NELSON_1", "title", "x", "description", "y",
                        "point_indices", List.of(3), "severity", "high"))));
        when(capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(
                eq(TENANT), eq(CapaSourceType.SPC_ALERT), anyString(), any())).thenReturn(true);

        SpcDto.KpiSpcResponse r = service.analyzeKpi(KPI, 30, true);

        assertThat(r.capaId()).isNull();
        verify(capaService, never()).createCase(any());
    }

    @Test
    void analyzeKpi_tooFewMeasurements_throws422() {
        TenantContext.setTenantId(TENANT.toString());
        stubSeries(List.of(10.0)); // 1 < MIN_POINTS

        assertThatThrownBy(() -> service.analyzeKpi(KPI, 30, false))
                .isInstanceOf(ResponseStatusException.class);
        verify(ai, never()).detectSpc(any(), any(), any());
    }

    // ===== forecast (§6.5) : quand le procédé VA sortir de ses limites =====

    @Test
    void forecast_mapsFullGatewayResponse() {
        when(ai.forecastSpc(eq(SERIE), eq(12), isNull(), isNull(), isNull()))
                .thenReturn(previsionMap(true, 4, "upper", "high"));

        SpcDto.ForecastResponse r = service.forecast(
                new SpcDto.ForecastRequest(SERIE, 12, null, null, null));

        assertThat(r.willBreach()).isTrue();
        assertThat(r.firstBreachStep()).isEqualTo(4);
        assertThat(r.side()).isEqualTo("upper");
        assertThat(r.horizon()).isEqualTo(12);
        assertThat(r.threshold()).isEqualTo(0.5);
        assertThat(r.trend()).isEqualTo(0.4);
        assertThat(r.confidence()).isEqualTo("high");
        assertThat(r.model()).isEqualTo("holt_linear");
        assertThat(r.limits().ucl()).isEqualTo(13.0);
        // Chaque période porte son intervalle et ses deux côtés : sans eux,
        // l'écran ne pourrait pas dessiner la bande qui justifie l'annonce.
        assertThat(r.points()).hasSize(2);
        assertThat(r.points().get(1).pBreach()).isEqualTo(0.8);
        assertThat(r.points().get(1).pAboveUcl()).isEqualTo(0.8);
        assertThat(r.points().get(0).low()).isEqualTo(10.0);
    }

    /** Une réponse sans annonce ne doit pas se traduire par une étape inventée. */
    @Test
    void forecast_noBreach_leavesStepAndSideNull() {
        when(ai.forecastSpc(any(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(previsionMap(false, null, null, "medium"));

        SpcDto.ForecastResponse r = service.forecast(
                new SpcDto.ForecastRequest(SERIE, null, null, null, null));

        assertThat(r.willBreach()).isFalse();
        assertThat(r.firstBreachStep()).isNull();
        assertThat(r.side()).isNull();
    }

    @Test
    void forecast_passesBaselineAndThresholdThrough() {
        when(ai.forecastSpc(eq(SERIE), eq(6), eq(10.0), eq(1.0), eq(0.9)))
                .thenReturn(previsionMap(false, null, null, "high"));

        service.forecast(new SpcDto.ForecastRequest(SERIE, 6, 10.0, 1.0, 0.9));

        verify(ai).forecastSpc(eq(SERIE), eq(6), eq(10.0), eq(1.0), eq(0.9));
    }

    @Test
    void forecastKpi_returnsSeriesAndForecast() {
        TenantContext.setTenantId(TENANT.toString());
        stubSeries(SERIE);
        when(ai.forecastSpc(eq(SERIE), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(previsionMap(true, 3, "upper", "high"));

        SpcDto.KpiForecastResponse r = service.forecastKpi(KPI, 30, null, false);

        assertThat(r.kpiCode()).isEqualTo("OEE");
        assertThat(r.values()).isEqualTo(SERIE);
        assertThat(r.periods()).hasSize(SERIE.size());
        assertThat(r.forecast().firstBreachStep()).isEqualTo(3);
        // Sans openCapa, on annonce et on n'ouvre rien : l'IA suggère (§12.3).
        assertThat(r.capaId()).isNull();
        verify(capaService, never()).createCase(any());
    }

    /**
     * L'écart n'est pas survenu : le dossier doit être PRÉVENTIF. L'appeler
     * correctif laisserait croire qu'il l'est, et le ferait traiter comme tel.
     */
    @Test
    void forecastKpi_openCapa_opensAPreventiveCase() {
        TenantContext.setTenantId(TENANT.toString());
        stubSeries(SERIE);
        when(ai.forecastSpc(any(), any(), isNull(), isNull(), isNull()))
                .thenReturn(previsionMap(true, 2, "lower", "high"));
        when(capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(
                eq(TENANT), eq(CapaSourceType.SPC_ALERT), anyString(), any())).thenReturn(false);
        UUID capaId = UUID.randomUUID();
        when(capaService.createCase(any())).thenReturn(caseResponse(capaId));

        SpcDto.KpiForecastResponse r = service.forecastKpi(KPI, 30, 6, true);

        assertThat(r.capaId()).isEqualTo(capaId);
        ArgumentCaptor<CapaDto.CreateCaseRequest> captor =
                ArgumentCaptor.forClass(CapaDto.CreateCaseRequest.class);
        verify(capaService).createCase(captor.capture());
        CapaDto.CreateCaseRequest demande = captor.getValue();
        assertThat(demande.type()).isEqualTo(CapaType.PREVENTIVE);
        assertThat(demande.sourceType()).isEqualTo(CapaSourceType.SPC_ALERT);
        // Référence distincte du correctif : une annonce ne doit pas empêcher le
        // dossier correctif quand la dérive, elle, se produit vraiment.
        assertThat(demande.sourceRef()).isEqualTo("kpi:" + KPI + ":forecast");
        assertThat(demande.description())
                .contains("2 période(s)")
                .contains("inférieure")
                .contains("confiance de la prévision : high");
    }

    /**
     * La criticité suit la CONFIANCE, pas le risque : mobiliser en « haute » sur
     * une série de cinq points reviendrait à prêter au chiffre une précision que
     * le modèle lui-même ne tient pas.
     */
    @Test
    void forecastKpi_lowConfidence_opensALowCriticityCase() {
        TenantContext.setTenantId(TENANT.toString());
        stubSeries(SERIE);
        when(ai.forecastSpc(any(), any(), isNull(), isNull(), isNull()))
                .thenReturn(previsionMap(true, 1, "upper", "low"));
        when(capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(
                eq(TENANT), eq(CapaSourceType.SPC_ALERT), anyString(), any())).thenReturn(false);
        when(capaService.createCase(any())).thenReturn(caseResponse(UUID.randomUUID()));

        service.forecastKpi(KPI, 30, null, true);

        ArgumentCaptor<CapaDto.CreateCaseRequest> captor =
                ArgumentCaptor.forClass(CapaDto.CreateCaseRequest.class);
        verify(capaService).createCase(captor.capture());
        assertThat(captor.getValue().criticity()).isEqualTo(CapaCriticity.LOW);
    }

    @Test
    void forecastKpi_existingActiveCase_doesNotOpenAnother() {
        TenantContext.setTenantId(TENANT.toString());
        stubSeries(SERIE);
        when(ai.forecastSpc(any(), any(), isNull(), isNull(), isNull()))
                .thenReturn(previsionMap(true, 2, "upper", "high"));
        when(capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(
                eq(TENANT), eq(CapaSourceType.SPC_ALERT), anyString(), any())).thenReturn(true);

        SpcDto.KpiForecastResponse r = service.forecastKpi(KPI, 30, null, true);

        assertThat(r.capaId()).isNull();
        verify(capaService, never()).createCase(any());
    }

    @Test
    void forecastKpi_noBreach_opensNothingEvenWhenAsked() {
        TenantContext.setTenantId(TENANT.toString());
        stubSeries(SERIE);
        when(ai.forecastSpc(any(), any(), isNull(), isNull(), isNull()))
                .thenReturn(previsionMap(false, null, null, "high"));

        SpcDto.KpiForecastResponse r = service.forecastKpi(KPI, 30, null, true);

        assertThat(r.capaId()).isNull();
        verify(capaService, never()).createCase(any());
    }

    /**
     * Deux mesures suffisent à dessiner une carte, pas à dater une dérive : mieux
     * vaut refuser que rendre une date inventée.
     */
    @Test
    void forecastKpi_tooFewMeasurements_throws422() {
        TenantContext.setTenantId(TENANT.toString());
        stubSeries(List.of(10.0, 10.5, 11.0)); // 3 < MIN_FORECAST_POINTS

        assertThatThrownBy(() -> service.forecastKpi(KPI, 30, null, false))
                .isInstanceOf(ResponseStatusException.class);
        verify(ai, never()).forecastSpc(any(), any(), any(), any(), any());
    }

    // ===== stubs =====

    private void stubSeries(List<Double> values) {
        List<Instant> periods = values.stream().map(v -> Instant.now()).toList();
        when(kpiService.spcSeries(eq(KPI), eq(30))).thenReturn(new KpiDto.SpcSeries(
                KPI, "OEE", "Taux de rendement synthétique", "%", UUID.randomUUID(), periods, values));
    }

    private Map<String, Object> okMap(boolean ooc, List<Map<String, Object>> violations) {
        return Map.of(
                "n", 4, "out_of_control", ooc,
                "limits", Map.of("center_line", 10.0, "sigma", 1.0,
                        "ucl", 13.0, "lcl", 7.0, "estimated", true),
                "violations", violations);
    }

    private CapaDto.CaseResponse caseResponse(UUID id) {
        Instant now = Instant.now();
        return new CapaDto.CaseResponse(
                id, TENANT, "SPC hors contrôle — KPI OEE", "desc",
                CapaType.CORRECTIVE, CapaCriticity.HIGH, CapaStatus.OPEN,
                CapaSourceType.SPC_ALERT, "kpi:" + KPI, UUID.randomUUID(), null,
                null, null, null, null, null, null,
                null, null,
                null, null, null, null,
                now, now, List.of(), null, List.of());
    }

    /** Serie de quatre mesures : le minimum pour que prevoir ait un sens. */
    static final List<Double> SERIE = List.of(10.0, 10.6, 11.2, 11.8);

    private Map<String, Object> previsionMap(boolean sortie, Integer etape, String cote,
                                             String confiance) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("n", 4);
        m.put("horizon", 12);
        m.put("threshold", 0.5);
        m.put("will_breach", sortie);
        m.put("first_breach_step", etape);   // null quand rien n'est annonce
        m.put("side", cote);
        m.put("trend", 0.4);
        m.put("residual_sigma", 0.2);
        m.put("r2", 0.95);
        m.put("confidence", confiance);
        m.put("model", "holt_linear");
        m.put("limits", Map.of("center_line", 10.0, "sigma", 1.0,
                "ucl", 13.0, "lcl", 7.0, "estimated", true));
        m.put("points", List.of(
                Map.of("step", 1, "value", 12.0, "low", 10.0, "high", 14.0,
                        "p_above_ucl", 0.2, "p_below_lcl", 0.0, "p_breach", 0.2),
                Map.of("step", 2, "value", 13.5, "low", 11.0, "high", 16.0,
                        "p_above_ucl", 0.8, "p_below_lcl", 0.0, "p_breach", 0.8)));
        return m;
    }
}
