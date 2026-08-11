package com.openlab.qualitos.quality.anomaly;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyServiceTest {

    @Mock AiGatewayClient ai;
    @Mock com.openlab.qualitos.quality.capa.CapaService capaService;
    @Mock com.openlab.qualitos.quality.capa.CapaCaseRepository capaCaseRepo;
    @InjectMocks AnomalyService service;

    @Test
    void detect_mapsFullGatewayResponse() {
        when(ai.detectAnomaly(any(), eq("isolation_forest"), eq(0.1), isNull())).thenReturn(Map.of(
                "n", 3,
                "n_features", 2,
                "method", "isolation_forest",
                "contamination", 0.1,
                "threshold", 0.62,
                "anomaly_count", 1,
                "has_anomalies", true,
                "points", List.of(
                        Map.of("index", 0, "score", 0.40, "is_anomaly", false, "top_feature", 1),
                        Map.of("index", 1, "score", 0.45, "is_anomaly", false),
                        Map.of("index", 2, "score", 0.80, "is_anomaly", true))));

        AnomalyDto.DetectResponse r = service.detect(new AnomalyDto.DetectRequest(
                List.of(List.of(1.0, 2.0), List.of(1.1, 2.1), List.of(50.0, -50.0)),
                "isolation_forest", 0.1, null, null, null));

        assertThat(r.n()).isEqualTo(3);
        assertThat(r.nFeatures()).isEqualTo(2);
        assertThat(r.method()).isEqualTo("isolation_forest");
        assertThat(r.threshold()).isEqualTo(0.62);
        assertThat(r.anomalyCount()).isEqualTo(1);
        assertThat(r.hasAnomalies()).isTrue();
        assertThat(r.points()).hasSize(3);
        assertThat(r.points().get(0).topFeature()).isEqualTo(1);
        assertThat(r.points().get(1).topFeature()).isNull(); // absent → null
        assertThat(r.points().get(2).isAnomaly()).isTrue();
    }

    @Test
    void detect_toleratesMissingOrMistypedFields() {
        when(ai.detectAnomaly(any(), isNull(), isNull(), isNull())).thenReturn(Map.of(
                "points", "not-a-list"));

        AnomalyDto.DetectResponse r = service.detect(new AnomalyDto.DetectRequest(
                List.of(List.of(5.0)), null, null, null, null, null));

        assertThat(r.n()).isZero();
        assertThat(r.method()).isEmpty();
        assertThat(r.hasAnomalies()).isFalse();
        assertThat(r.points()).isEmpty();
    }

    @Test
    void detect_passesOptionalsThrough() {
        when(ai.detectAnomaly(any(), eq("reconstruction"), eq(0.2), eq(0.7))).thenReturn(Map.of());

        service.detect(new AnomalyDto.DetectRequest(
                List.of(List.of(1.0, 2.0), List.of(3.0, 4.0)), "reconstruction", 0.2, 0.7, null, null));

        ArgumentCaptor<String> method = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> cont = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> thr = ArgumentCaptor.forClass(Double.class);
        verify(ai).detectAnomaly(any(), method.capture(), cont.capture(), thr.capture());
        assertThat(method.getValue()).isEqualTo("reconstruction");
        assertThat(cont.getValue()).isEqualTo(0.2);
        assertThat(thr.getValue()).isEqualTo(0.7);
    }

    @Test
    void explain_mapsGatewayResponse() {
        when(ai.explainAnomaly(any(), eq(2))).thenReturn(Map.of(
                "index", 2,
                "method", "isolation_forest",
                "score", 0.82,
                "base_value", 0.50,
                "contributions", List.of(
                        Map.of("feature", 0, "value", 50.0, "contribution", 0.20),
                        Map.of("feature", 1, "value", -50.0, "contribution", 0.12))));

        AnomalyDto.ExplainResponse r = service.explain(new AnomalyDto.ExplainRequest(
                List.of(List.of(1.0, 2.0), List.of(1.1, 2.1), List.of(50.0, -50.0)), 2));

        assertThat(r.index()).isEqualTo(2);
        assertThat(r.method()).isEqualTo("isolation_forest");
        assertThat(r.score()).isEqualTo(0.82);
        assertThat(r.baseValue()).isEqualTo(0.50);
        assertThat(r.contributions()).hasSize(2);
        assertThat(r.contributions().get(0).contribution()).isEqualTo(0.20);
    }

    @Test
    void explain_toleratesMissingFields() {
        when(ai.explainAnomaly(any(), any())).thenReturn(Map.of("contributions", "nope"));
        AnomalyDto.ExplainResponse r = service.explain(new AnomalyDto.ExplainRequest(
                List.of(List.of(1.0), List.of(2.0)), 0));
        assertThat(r.method()).isEmpty();
        assertThat(r.contributions()).isEmpty();
    }

    // --- ouverture d'une CAPA sur anomalie (ADR 0022) ---------------------------
    // La boucle détection → action corrective s'arrêtait à l'écran : on savait
    // qu'un point était anormal, et rien ne s'ensuivait.

    private void gatewayReturns(int n, int anomalies) {
        when(ai.detectAnomaly(any(), any(), any(), any())).thenReturn(Map.of(
                "n", n, "n_features", 2, "method", "isolation_forest",
                "contamination", 0.1, "threshold", 0.62,
                "anomaly_count", anomalies, "has_anomalies", anomalies > 0,
                "points", List.of()));
    }

    private AnomalyDto.DetectRequest detectRequest(String subject, Boolean openCapa) {
        return new AnomalyDto.DetectRequest(
                List.of(List.of(1.0, 2.0)), "isolation_forest", 0.1, null, subject, openCapa);
    }

    private void tenantInContext() {
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private static final UUID TENANT = UUID.randomUUID();

    @Test
    void ouvreUneCapaQuandDesAnomaliesRessortent() {
        tenantInContext();
        gatewayReturns(50, 1);
        UUID capaId = UUID.randomUUID();
        when(capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(any(), any(), any(), any()))
                .thenReturn(false);
        when(capaService.createCase(any())).thenReturn(caseResponse(capaId));

        AnomalyDto.DetectResponse r = service.detect(detectRequest("presse-2 / ligne 3", true));

        assertThat(r.capaId()).isEqualTo(capaId);
        ArgumentCaptor<CapaDto.CreateCaseRequest> req =
                ArgumentCaptor.forClass(CapaDto.CreateCaseRequest.class);
        verify(capaService).createCase(req.capture());
        assertThat(req.getValue().sourceType()).isEqualTo(CapaSourceType.ANOMALY);
        assertThat(req.getValue().sourceRef()).isEqualTo("anomaly:presse-2 / ligne 3");
        assertThat(req.getValue().title()).contains("presse-2 / ligne 3");
        // Le modèle signale, il ne conclut pas : la fiche doit le dire.
        assertThat(req.getValue().description()).contains("il ne nomme pas de cause");
    }

    @Test
    void nOuvreRienSansAnomalie() {
        gatewayReturns(50, 0);

        AnomalyDto.DetectResponse r = service.detect(detectRequest("presse-2", true));

        assertThat(r.capaId()).isNull();
        verifyNoInteractions(capaService);
    }

    @Test
    void nOuvreRienSansDemande() {
        gatewayReturns(50, 5);

        AnomalyDto.DetectResponse r = service.detect(detectRequest("presse-2", null));

        assertThat(r.capaId()).isNull();
        verifyNoInteractions(capaService);
    }

    @Test
    void refuseDOuvrirUnDossierSansSujet() {
        gatewayReturns(50, 5);

        // Une action corrective qui ne dit pas sur quoi elle porte n'est pas
        // exploitable : mieux vaut ne rien ouvrir.
        assertThat(service.detect(detectRequest(null, true)).capaId()).isNull();
        assertThat(service.detect(detectRequest("   ", true)).capaId()).isNull();
        verifyNoInteractions(capaService);
    }

    @Test
    void nOuvrePasUnSecondDossierSurLeMemeSujet() {
        tenantInContext();
        gatewayReturns(50, 5);
        when(capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(
                eq(TENANT), eq(CapaSourceType.ANOMALY), eq("anomaly:presse-2"), any()))
                .thenReturn(true);

        assertThat(service.detect(detectRequest("presse-2", true)).capaId()).isNull();

        // Un dossier par analyse noierait le vrai dans une pile de doublons.
        verifyNoInteractions(capaService);
    }

    @Test
    void graduelLaCriticiteSelonLaPartDObservationsAnormales() {
        tenantInContext();
        when(capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(any(), any(), any(), any()))
                .thenReturn(false);
        when(capaService.createCase(any())).thenReturn(caseResponse(UUID.randomUUID()));
        ArgumentCaptor<CapaDto.CreateCaseRequest> req =
                ArgumentCaptor.forClass(CapaDto.CreateCaseRequest.class);

        gatewayReturns(1000, 1);      // 0,1 % — un aléa
        service.detect(detectRequest("a", true));
        gatewayReturns(100, 5);       // 5 % — à regarder
        service.detect(detectRequest("b", true));
        gatewayReturns(100, 20);      // 20 % — le procédé a dérivé
        service.detect(detectRequest("c", true));

        verify(capaService, times(3)).createCase(req.capture());
        assertThat(req.getAllValues().get(0).criticity()).isEqualTo(CapaCriticity.LOW);
        assertThat(req.getAllValues().get(1).criticity()).isEqualTo(CapaCriticity.MEDIUM);
        assertThat(req.getAllValues().get(2).criticity()).isEqualTo(CapaCriticity.HIGH);
    }

    @Test
    void assainitLeSujetAvantDenFaireUnTitreEtUneReference() {
        tenantInContext();
        gatewayReturns(50, 5);
        when(capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(any(), any(), any(), any()))
                .thenReturn(false);
        when(capaService.createCase(any())).thenReturn(caseResponse(UUID.randomUUID()));

        service.detect(detectRequest("  presse\n2   \t ligne\r3  ", true));

        ArgumentCaptor<CapaDto.CreateCaseRequest> req =
                ArgumentCaptor.forClass(CapaDto.CreateCaseRequest.class);
        verify(capaService).createCase(req.capture());
        // Un titre ne porte pas de saut de ligne, et une référence doit rester
        // comparable à l'identique d'une analyse à l'autre.
        assertThat(req.getValue().sourceRef()).isEqualTo("anomaly:presse 2 ligne 3");
    }

    private CapaDto.CaseResponse caseResponse(UUID id) {
        return new CapaDto.CaseResponse(id, TENANT, "t", "d", CapaType.CORRECTIVE,
                CapaCriticity.LOW, CapaStatus.OPEN, CapaSourceType.ANOMALY, "anomaly:x",
                null, null, null, null, null, null, null, null, null, List.of(), null, List.of());
    }
}
