package com.openlab.qualitos.quality.objective;

import com.openlab.qualitos.quality.capa.ObjectiveRecoveryPort;
import com.openlab.qualitos.quality.kpi.KpiDefinition;
import com.openlab.qualitos.quality.kpi.KpiDefinitionRepository;
import com.openlab.qualitos.quality.kpi.KpiDirection;
import com.openlab.qualitos.quality.kpi.KpiMeasurement;
import com.openlab.qualitos.quality.kpi.KpiMeasurementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Ce que le module CAPA demande à un objectif qualité (ADR 0133)")
class ObjectiveRecoveryAdapterTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID KPI = UUID.randomUUID();
    static final UUID OBJ = UUID.randomUUID();
    static final UUID CASE = UUID.randomUUID();

    @Mock QualityObjectiveProposalRepository proposals;
    @Mock QualityObjectiveRepository objectives;
    @Mock KpiDefinitionRepository kpis;
    @Mock KpiMeasurementRepository measurements;

    ObjectiveRecoveryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ObjectiveRecoveryAdapter(proposals, objectives, kpis, measurements);
        when(kpis.findById(KPI)).thenReturn(Optional.of(kpi()));
        when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(objectif()));
        when(proposals.findByTenantIdAndCapaCaseId(TENANT, CASE))
                .thenReturn(Optional.of(proposition()));
    }

    static KpiDefinition kpi() {
        KpiDefinition d = new KpiDefinition();
        d.setId(KPI);
        d.setTenantId(TENANT);
        d.setCode("nc-client-ppm");
        d.setName("Taux de NC client");
        d.setUnit("ppm");
        d.setDirection(KpiDirection.LOWER_IS_BETTER);
        return d;
    }

    /** Taux de NC client : cible 500 ppm, retour exige sur 2 periodes. */
    static QualityObjective objectif() {
        QualityObjective o = new QualityObjective();
        o.setId(OBJ);
        o.setTenantId(TENANT);
        o.setTitle("Taux de NC client");
        o.setKpiId(KPI);
        o.setPeriodMonths(1);
        o.setTargetValue(new BigDecimal("500"));
        o.setThresholdWarning(new BigDecimal("600"));
        o.setThresholdCritical(new BigDecimal("800"));
        o.setWarningPeriods(2);
        o.setCriticalPeriods(1);
        o.setRecoveryPeriods(2);
        o.setStatus(ObjectiveLifecycle.ACTIVE);
        return o;
    }

    static QualityObjectiveProposal proposition() {
        QualityObjectiveProposal p = new QualityObjectiveProposal();
        p.setTenantId(TENANT);
        p.setObjectiveId(OBJ);
        p.setCapaCaseId(CASE);
        p.setEffectivenessCriterion("Le KPI revient à ou sous 500 ppm pendant 2 période(s).");
        return p;
    }

    private void releves(String... moisEtValeurs) {
        List<KpiMeasurement> liste = new ArrayList<>();
        for (int i = 0; i < moisEtValeurs.length; i += 2) {
            KpiMeasurement m = new KpiMeasurement();
            m.setPeriodStart(LocalDate.parse(moisEtValeurs[i] + "-01")
                    .atStartOfDay(ZoneOffset.UTC).toInstant());
            m.setValue(new BigDecimal(moisEtValeurs[i + 1]));
            liste.add(m);
        }
        when(measurements.findTop24ByKpiIdOrderByPeriodStartDesc(KPI)).thenReturn(liste);
    }

    @Test
    void unDossierQuiNeVientPasDUnObjectif_laQuestionNeSePosePas() {
        // Vide ne veut PAS dire « pas encore revenu » : sans quoi tout dossier
        // ordinaire deviendrait impossible à clore.
        when(proposals.findByTenantIdAndCapaCaseId(TENANT, CASE)).thenReturn(Optional.empty());
        assertThat(adapter.forCase(TENANT, CASE)).isEmpty();
    }

    @Test
    void sansIdentifiant_laQuestionNeSePosePas() {
        assertThat(adapter.forCase(null, CASE)).isEmpty();
        assertThat(adapter.forCase(TENANT, null)).isEmpty();
    }

    @Test
    void unObjectifDisparu_laQuestionNeSePosePlus() {
        when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.empty());
        assertThat(adapter.forCase(TENANT, CASE)).isEmpty();
    }

    @Test
    void leKpiPasEncoreRevenu_leRefusEstChiffre() {
        releves("2026-09", "900", "2026-10", "480");
        ObjectiveRecoveryPort.Recovery r = adapter.forCase(TENANT, CASE).orElseThrow();
        assertThat(r.achieved()).isFalse();
        assertThat(r.onTargetPeriods()).isEqualTo(1);
        assertThat(r.requiredPeriods()).isEqualTo(2);
        assertThat(r.criterion()).contains("500 ppm");
    }

    @Test
    void leKpiRevenuPourLaDureeExigee_leRetourEstConstate() {
        releves("2026-08", "900", "2026-09", "480", "2026-10", "470");
        ObjectiveRecoveryPort.Recovery r = adapter.forCase(TENANT, CASE).orElseThrow();
        assertThat(r.achieved()).isTrue();
        assertThat(r.onTargetPeriods()).isEqualTo(2);
    }

    @Test
    void sansAucunReleve_rienNEstConstate() {
        releves();
        ObjectiveRecoveryPort.Recovery r = adapter.forCase(TENANT, CASE).orElseThrow();
        assertThat(r.achieved()).isFalse();
        assertThat(r.onTargetPeriods()).isZero();
    }

    @Test
    void unKpiDisparu_repondPasRevenu_etNonPasDeQuestion() {
        // Laisser clore reviendrait à déclarer l'efficacité acquise faute de
        // pouvoir la mesurer.
        when(kpis.findById(KPI)).thenReturn(Optional.empty());
        ObjectiveRecoveryPort.Recovery r = adapter.forCase(TENANT, CASE).orElseThrow();
        assertThat(r.achieved()).isFalse();
        assertThat(r.criterion()).isNotBlank();
    }

    @Test
    void unKpiSansSensDAmelioration_repondPasRevenu() {
        KpiDefinition sansSens = kpi();
        sansSens.setDirection(null);
        when(kpis.findById(KPI)).thenReturn(Optional.of(sansSens));
        assertThat(adapter.forCase(TENANT, CASE).orElseThrow().achieved()).isFalse();
    }

    @Test
    void unKpiDUnAutreClient_nEstPasLu() {
        KpiDefinition ailleurs = kpi();
        ailleurs.setTenantId(UUID.randomUUID());
        when(kpis.findById(KPI)).thenReturn(Optional.of(ailleurs));
        assertThat(adapter.forCase(TENANT, CASE).orElseThrow().achieved()).isFalse();
    }
}
