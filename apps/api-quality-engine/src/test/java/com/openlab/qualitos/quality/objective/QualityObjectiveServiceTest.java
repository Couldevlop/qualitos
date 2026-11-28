package com.openlab.qualitos.quality.objective;

import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.kpi.KpiDefinition;
import com.openlab.qualitos.quality.kpi.KpiDefinitionRepository;
import com.openlab.qualitos.quality.kpi.KpiDirection;
import com.openlab.qualitos.quality.kpi.KpiMeasurement;
import com.openlab.qualitos.quality.kpi.KpiMeasurementRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Les objectifs qualité : définir, revoir, proposer (ADR 0133)")
class QualityObjectiveServiceTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID ACTOR = UUID.randomUUID();
    static final UUID PILOTE = UUID.randomUUID();
    static final UUID KPI = UUID.randomUUID();
    static final UUID OBJ = UUID.randomUUID();
    static final UUID PROP = UUID.randomUUID();
    static final UUID CASE = UUID.randomUUID();

    @Mock QualityObjectiveRepository objectives;
    @Mock QualityObjectiveProposalRepository proposals;
    @Mock KpiDefinitionRepository kpis;
    @Mock KpiMeasurementRepository measurements;
    @Mock CapaService capaService;
    @Mock CapaCaseRepository capaCases;

    QualityObjectiveService service;

    @BeforeEach
    void setUp() {
        service = new QualityObjectiveService(objectives, proposals, kpis, measurements,
                capaService, capaCases);
        TenantContext.setTenantId(TENANT.toString());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ACTOR.toString(), "n/a", List.of()));
        when(kpis.findById(KPI)).thenReturn(Optional.of(kpi()));
        when(objectives.save(any())).thenAnswer(inv -> {
            QualityObjective o = inv.getArgument(0);
            if (o.getId() == null) o.setId(OBJ);
            if (o.getCreatedAt() == null) o.setCreatedAt(Instant.now());
            o.setUpdatedAt(Instant.now());
            return o;
        });
        when(proposals.save(any())).thenAnswer(inv -> {
            QualityObjectiveProposal p = inv.getArgument(0);
            if (p.getId() == null) p.setId(PROP);
            if (p.getCreatedAt() == null) p.setCreatedAt(Instant.now());
            return p;
        });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ---------- fixtures ----------

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

    static QualityObjective objectif() {
        QualityObjective o = new QualityObjective();
        o.setId(OBJ);
        o.setTenantId(TENANT);
        o.setTitle("Taux de NC client");
        o.setKpiId(KPI);
        o.setProcessName("Production");
        o.setIsoClause("6.2");
        o.setPeriodMonths(1);
        o.setTargetValue(new BigDecimal("500"));
        o.setThresholdWarning(new BigDecimal("600"));
        o.setThresholdCritical(new BigDecimal("800"));
        o.setWarningPeriods(2);
        o.setCriticalPeriods(1);
        o.setRecoveryPeriods(2);
        o.setStatus(ObjectiveLifecycle.ACTIVE);
        o.setCreatedBy(ACTOR);
        o.setCreatedAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        return o;
    }

    /** Le dossier que renvoie {@code CapaService} : seul son identifiant compte ici. */
    static CapaDto.CaseResponse dossierCree() {
        return new CapaDto.CaseResponse(
                CASE, TENANT, "Objectif qualité critique — Taux de NC client", null,
                CapaType.CORRECTIVE, null, CapaStatus.OPEN,
                CapaSourceType.QUALITY_OBJECTIVE, null, PILOTE, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, List.of(), null, List.of());
    }

    static ObjectiveDto.CreateRequest creation(String target, String warning, String critical) {
        return new ObjectiveDto.CreateRequest("Taux de NC client", KPI, null, "Production",
                "6.2", 1, new BigDecimal(target), new BigDecimal(warning),
                new BigDecimal(critical), 2, 1, 2);
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
        liste.sort((a, b) -> b.getPeriodStart().compareTo(a.getPeriodStart()));
        when(measurements.findTop24ByKpiIdOrderByPeriodStartDesc(KPI)).thenReturn(liste);
    }

    // ---------- définition ----------

    @Nested
    @DisplayName("Définir un objectif")
    class Definir {

        @Test
        void lObjectifNaitActif_etPorteLeKpiEtLAuteurDuJeton() {
            releves();
            ObjectiveDto.ObjectiveResponse out = service.create(creation("500", "600", "800"));
            assertThat(out.lifecycle()).isEqualTo(ObjectiveLifecycle.ACTIVE);
            assertThat(out.kpiId()).isEqualTo(KPI);
            ArgumentCaptor<QualityObjective> c = ArgumentCaptor.forClass(QualityObjective.class);
            verify(objectives).save(c.capture());
            assertThat(c.getValue().getTenantId()).isEqualTo(TENANT);
            assertThat(c.getValue().getCreatedBy()).isEqualTo(ACTOR);
        }

        @Test
        void lUniteEtLeSensViennentDuKpi_etNeSontPasRecopiesSurLObjectif() {
            // La maquette ne met pas d'unité sur les champs de cible et de
            // seuils, parce qu'elle change d'un indicateur à l'autre.
            releves();
            ObjectiveDto.ObjectiveResponse out = service.create(creation("500", "600", "800"));
            assertThat(out.kpiUnit()).isEqualTo("ppm");
            assertThat(out.kpiDirection()).isEqualTo(KpiDirection.LOWER_IS_BETTER);
        }

        @Test
        void unKpiInconnu_estRefuse() {
            UUID inconnu = UUID.randomUUID();
            when(kpis.findById(inconnu)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.create(new ObjectiveDto.CreateRequest(
                    "x", inconnu, null, null, null, 1, BigDecimal.ONE, BigDecimal.TWO,
                    new BigDecimal("3"), 2, 1, 1)))
                    .isInstanceOf(ObjectiveValidationException.class)
                    .hasMessageContaining("Unknown KPI");
        }

        @Test
        void unKpiDUnAutreClient_estInconnu() {
            KpiDefinition ailleurs = kpi();
            ailleurs.setTenantId(UUID.randomUUID());
            when(kpis.findById(KPI)).thenReturn(Optional.of(ailleurs));
            assertThatThrownBy(() -> service.create(creation("500", "600", "800")))
                    .isInstanceOf(ObjectiveValidationException.class);
        }

        @Test
        void unKpiSansSensDAmelioration_estRefuse() {
            KpiDefinition sansSens = kpi();
            sansSens.setDirection(null);
            when(kpis.findById(KPI)).thenReturn(Optional.of(sansSens));
            assertThatThrownBy(() -> service.create(creation("500", "600", "800")))
                    .isInstanceOf(ObjectiveValidationException.class)
                    .hasMessageContaining("direction of improvement");
        }

        @Test
        void unePeriodeHorsDe1_3_6_12_estRefusee() {
            assertThatThrownBy(() -> service.create(new ObjectiveDto.CreateRequest(
                    "x", KPI, null, null, null, 7, new BigDecimal("500"),
                    new BigDecimal("600"), new BigDecimal("800"), 2, 1, 1)))
                    .isInstanceOf(ObjectiveValidationException.class)
                    .hasMessageContaining("1, 3, 6 or 12");
        }

        @Test
        void desSeuilsDansLeMauvaisSens_sontRefuses() {
            // On veut descendre : un seuil d'alerte SOUS la cible ne déclencherait
            // jamais, et l'objectif aurait l'air réglé.
            assertThatThrownBy(() -> service.create(creation("600", "500", "800")))
                    .isInstanceOf(ObjectiveValidationException.class)
                    .hasMessageContaining("ordered in the KPI's direction");
        }

        @Test
        void unCritiquePlusClementQueLAlerte_estRefuse() {
            assertThatThrownBy(() -> service.create(creation("500", "800", "600")))
                    .isInstanceOf(ObjectiveValidationException.class);
        }

        @Test
        void surUnKpiQuOnVeutFaireMonter_lOrdreEstInverse() {
            KpiDefinition monte = kpi();
            monte.setDirection(KpiDirection.HIGHER_IS_BETTER);
            when(kpis.findById(KPI)).thenReturn(Optional.of(monte));
            releves();
            // cible 95 ≥ alerte 93 ≥ critique 90
            assertThat(service.create(creation("95", "93", "90"))).isNotNull();
            assertThatThrownBy(() -> service.create(creation("90", "93", "95")))
                    .isInstanceOf(ObjectiveValidationException.class);
        }

        @Test
        void sansCibleNiSeuils_leRefusDitPourquoi() {
            assertThatThrownBy(() -> service.create(new ObjectiveDto.CreateRequest(
                    "x", KPI, null, null, null, 1, null, null, null, 2, 1, 1)))
                    .isInstanceOf(ObjectiveValidationException.class)
                    .hasMessageContaining("cannot say whether it is met");
        }

        @Test
        void unObjectifInconnu_estIntrouvable() {
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.get(OBJ))
                    .isInstanceOf(ObjectiveNotFoundException.class);
        }

        @Test
        void laModificationNeChangeQueCeQuiEstRenseigne() {
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(objectif()));
            releves();
            ObjectiveDto.ObjectiveResponse out = service.update(OBJ,
                    new ObjectiveDto.UpdateRequest("Nouveau titre", null, null, null,
                            null, null, null, null, null, null, 3, null));
            assertThat(out.title()).isEqualTo("Nouveau titre");
            assertThat(out.recoveryPeriods()).isEqualTo(3);
            assertThat(out.targetValue()).isEqualByComparingTo("500");
        }

        @Test
        void archiver_neSupprimePas() {
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(objectif()));
            service.archive(OBJ);
            ArgumentCaptor<QualityObjective> c = ArgumentCaptor.forClass(QualityObjective.class);
            verify(objectives).save(c.capture());
            assertThat(c.getValue().getStatus()).isEqualTo(ObjectiveLifecycle.ARCHIVED);
            verify(objectives, never()).delete(any());
        }
    }

    // ---------- revue ----------

    @Nested
    @DisplayName("La revue")
    class Revue {

        @Test
        void lesQuatreCompteursSuiventLesStatutsDesLignes() {
            QualityObjective atteint = objectif();
            atteint.setId(UUID.randomUUID());
            QualityObjective critique = objectif();
            critique.setId(UUID.randomUUID());
            when(objectives.findByTenantIdAndStatusOrderByCreatedAtDesc(
                    TENANT, ObjectiveLifecycle.ACTIVE)).thenReturn(List.of(atteint, critique));
            when(kpis.findAllById(any())).thenReturn(List.of(kpi()));
            releves("2026-10", "480");

            ObjectiveDto.ReviewResponse r = service.review(null, null);
            assertThat(r.counters().tracked()).isEqualTo(2);
            assertThat(r.counters().onTarget()).isEqualTo(2);
            assertThat(r.counters().critical()).isZero();
        }

        @Test
        void unObjectifDontLeKpiADisparu_neFigurePasDansLaRevue() {
            when(objectives.findByTenantIdAndStatusOrderByCreatedAtDesc(
                    TENANT, ObjectiveLifecycle.ACTIVE)).thenReturn(List.of(objectif()));
            when(kpis.findAllById(any())).thenReturn(List.of());
            assertThat(service.review(null, null).objectives()).isEmpty();
        }

        @Test
        void leFiltreParProcessus_neGardeQueLeSien() {
            UUID processus = UUID.randomUUID();
            QualityObjective dansLeProcessus = objectif();
            dansLeProcessus.setProcessId(processus);
            when(objectives.findByTenantIdAndStatusOrderByCreatedAtDesc(
                    TENANT, ObjectiveLifecycle.ACTIVE))
                    .thenReturn(List.of(dansLeProcessus, objectif()));
            when(kpis.findAllById(any())).thenReturn(List.of(kpi()));
            releves("2026-10", "480");
            assertThat(service.review(processus, null).objectives()).hasSize(1);
        }

        @Test
        void laRevueDOctobreNeVoitPasNovembre() {
            // Une revue rejouée doit dire ce qu'on y avait lu.
            when(objectives.findByTenantIdAndStatusOrderByCreatedAtDesc(
                    TENANT, ObjectiveLifecycle.ACTIVE)).thenReturn(List.of(objectif()));
            when(kpis.findAllById(any())).thenReturn(List.of(kpi()));
            releves("2026-10", "900", "2026-11", "480");

            ObjectiveDto.ReviewResponse r = service.review(null, LocalDate.parse("2026-10-31"));
            assertThat(r.objectives()).hasSize(1);
            assertThat(r.objectives().get(0).assessment().observedValue())
                    .isEqualByComparingTo("900");
            assertThat(r.objectives().get(0).assessment().status())
                    .isEqualTo(ObjectiveStatus.CRITICAL);
        }

        // ---------- la confrontation est automatique (ADR 0146) ----------

        @Test
        void ouvrirLaRevue_confronteLesObjectifsAuxMesures() {
            // Il n'y a plus de bouton : l'écran dit la vérité du jour sans
            // qu'on ait à demander la confrontation.
            when(objectives.findByTenantIdAndStatusOrderByCreatedAtDesc(
                    TENANT, ObjectiveLifecycle.ACTIVE)).thenReturn(List.of(objectif()));
            when(objectives.findByIdAndTenantId(OBJ, TENANT))
                    .thenReturn(Optional.of(objectif()));
            when(kpis.findAllById(any())).thenReturn(List.of(kpi()));
            when(kpis.findById(KPI)).thenReturn(Optional.of(kpi()));
            releves("2026-09", "650", "2026-10", "660");

            service.review(null, null);

            ArgumentCaptor<QualityObjectiveProposal> c =
                    ArgumentCaptor.forClass(QualityObjectiveProposal.class);
            verify(proposals).save(c.capture());
            assertThat(c.getValue().getStatus()).isEqualTo(ProposalStatus.PENDING);
            assertThat(c.getValue().getObjectiveId()).isEqualTo(OBJ);
        }

        @Test
        void rejouerUneRevuePASSEE_nEcritRien() {
            // Confronter pendant une relecture d'archive daterait d'aujourd'hui
            // des propositions nées d'une lecture d'hier.
            // Le MEME jeu que le banc precedent, qui lui ecrit.
            when(objectives.findByTenantIdAndStatusOrderByCreatedAtDesc(
                    TENANT, ObjectiveLifecycle.ACTIVE)).thenReturn(List.of(objectif()));
            lenient().when(objectives.findByIdAndTenantId(OBJ, TENANT))
                    .thenReturn(Optional.of(objectif()));
            when(kpis.findAllById(any())).thenReturn(List.of(kpi()));
            lenient().when(kpis.findById(KPI)).thenReturn(Optional.of(kpi()));
            releves("2026-09", "650", "2026-10", "660");

            service.review(null, LocalDate.parse("2026-10-31"));

            verify(proposals, never()).save(any());
        }

        @Test
        void unObjectifQuOnNePeutPasConfronter_neCassePasLaRevue() {
            // Tant que la confrontation était un geste, son échec se disait
            // dans un bandeau. Devenue automatique, la même erreur emporterait
            // la page entière et masquerait les objectifs qui se lisent bien.
            when(objectives.findByTenantIdAndStatusOrderByCreatedAtDesc(
                    TENANT, ObjectiveLifecycle.ACTIVE)).thenReturn(List.of(objectif()));
            when(objectives.findByIdAndTenantId(OBJ, TENANT))
                    .thenReturn(Optional.of(objectif()));
            // La revue lit le catalogue en bloc ; la confrontation, elle,
            // recharge le KPI par son identifiant. Absent de ce second chemin,
            // elle lève — et la ligne doit paraître quand même.
            when(kpis.findAllById(any())).thenReturn(List.of(kpi()));
            when(kpis.findById(KPI)).thenReturn(Optional.empty());
            releves("2026-09", "650", "2026-10", "660");

            ObjectiveDto.ReviewResponse r = service.review(null, null);

            assertThat(r.objectives()).hasSize(1);
            verify(proposals, never()).save(any());
        }
    }

    // ---------- règle de déclenchement ----------

    @Nested
    @DisplayName("La règle propose, elle ne crée pas")
    class Proposer {

        @BeforeEach
        void objectifTrouvable() {
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(objectif()));
        }

        @Test
        void sousLaCible_rienNEstPropose() {
            releves("2026-09", "480", "2026-10", "470");
            assertThat(service.evaluate(OBJ)).isEmpty();
            verify(proposals, never()).save(any());
        }

        @Test
        void unSeulMoisEnAlerte_rienNEstPropose() {
            releves("2026-09", "480", "2026-10", "650");
            assertThat(service.evaluate(OBJ)).isEmpty();
        }

        @Test
        void deuxMoisConsecutifsEnAlerte_uneCapaEstRedigee_maisAucunDossierNEstCree() {
            releves("2026-09", "650", "2026-10", "660");
            Optional<ObjectiveDto.ProposalResponse> p = service.evaluate(OBJ);
            assertThat(p).isPresent();
            assertThat(p.get().level()).isEqualTo(ObjectiveLevel.WARNING);
            assertThat(p.get().status()).isEqualTo(ProposalStatus.PENDING);
            assertThat(p.get().breachPeriods()).isEqualTo(2);
            assertThat(p.get().capaCaseId()).isNull();
            verify(capaService, never()).openCase(any(), any(), any());
        }

        @Test
        void auDelaDuCritique_unePeriodeSuffit() {
            releves("2026-10", "900");
            Optional<ObjectiveDto.ProposalResponse> p = service.evaluate(OBJ);
            assertThat(p).isPresent();
            assertThat(p.get().level()).isEqualTo(ObjectiveLevel.CRITICAL);
            verify(capaService, never()).openCase(any(), any(), any());
        }

        @Test
        void laPropositionCiteLEcart_laRegle_etLaPeriode() {
            releves("2026-10", "900");
            ObjectiveDto.ProposalResponse p = service.evaluate(OBJ).orElseThrow();
            assertThat(p.observedValue()).isEqualByComparingTo("900");
            assertThat(p.title()).contains("Objectif qualité critique", "Taux de NC client");
            assertThat(p.description())
                    .contains("ISO 9001 §6.2")
                    .contains("cible 500 ppm")
                    .contains("900 ppm")
                    .contains("Production")
                    .contains("du 01/10/2026 au 01/11/2026")
                    // Plus de délai d'analyse : il se saisit à l'ouverture du
                    // dossier, dans la fenêtre CAPA (ADR 0143).
                    .doesNotContain("jours ouvrés");
        }

        @Test
        void leCritereDEfficaciteEstLeRetourSousLaCible_etIlEstRedigeParLaRegle() {
            releves("2026-10", "900");
            ObjectiveDto.ProposalResponse p = service.evaluate(OBJ).orElseThrow();
            assertThat(p.effectivenessCriterion())
                    .contains("revient à ou sous")
                    .contains("500 ppm")
                    .contains("2 période(s)")
                    .contains("ne repasse « atteint » qu'à ce moment");
        }

        @Test
        void surUnKpiQuOnVeutFaireMonter_leCritereDitLInverse() {
            KpiDefinition monte = kpi();
            monte.setDirection(KpiDirection.HIGHER_IS_BETTER);
            when(kpis.findById(KPI)).thenReturn(Optional.of(monte));
            QualityObjective o = objectif();
            o.setTargetValue(new BigDecimal("95"));
            o.setThresholdWarning(new BigDecimal("93"));
            o.setThresholdCritical(new BigDecimal("90"));
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(o));
            releves("2026-10", "88");
            assertThat(service.evaluate(OBJ).orElseThrow().effectivenessCriterion())
                    .contains("revient à ou au-dessus de");
        }

        @Test
        void reevaluerLaMemePeriode_neProposePasDeuxFois() {
            releves("2026-10", "900");
            QualityObjectiveProposal deja = new QualityObjectiveProposal();
            deja.setId(PROP);
            deja.setTenantId(TENANT);
            deja.setObjectiveId(OBJ);
            deja.setLevel(ObjectiveLevel.CRITICAL);
            deja.setStatus(ProposalStatus.PENDING);
            deja.setCreatedAt(Instant.now());
            when(proposals.findByTenantIdAndObjectiveIdAndLevelAndPeriodStart(
                    eq(TENANT), eq(OBJ), eq(ObjectiveLevel.CRITICAL), any()))
                    .thenReturn(Optional.of(deja));

            assertThat(service.evaluate(OBJ)).isPresent();
            verify(proposals, never()).save(any());
        }

        @Test
        void unObjectifArchive_neProposePlus() {
            QualityObjective archive = objectif();
            archive.setStatus(ObjectiveLifecycle.ARCHIVED);
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(archive));
            assertThatThrownBy(() -> service.evaluate(OBJ))
                    .isInstanceOf(ObjectiveStateException.class)
                    .hasMessageContaining("archived");
        }

        @Test
        void sansAucunReleve_rienNEstPropose() {
            releves();
            assertThat(service.evaluate(OBJ)).isEmpty();
        }

        @Test
        void evaluerTout_rendLesPropositionsNeesDeCePassage() {
            when(objectives.findByTenantIdAndStatusOrderByCreatedAtDesc(
                    TENANT, ObjectiveLifecycle.ACTIVE)).thenReturn(List.of(objectif()));
            releves("2026-10", "900");
            assertThat(service.evaluateAll()).hasSize(1);
        }
    }

    // ---------- décision du pilote ----------

    @Nested
    @DisplayName("Le pilote ouvre la CAPA depuis la ligne, ou décline avec un motif")
    class Decider {

        QualityObjectiveProposal enAttente;

        /** L'action telle que le pilote l'écrit dans la fenêtre CAPA. */
        static final ObjectiveDto.OpenCapaRequest ACTION = new ObjectiveDto.OpenCapaRequest(
                "Recaler le réglage de la presse 3",
                "Reprise du plan de contrôle et nouvelle gamme de réglage.",
                PILOTE, "Awa Diallo", LocalDate.now().plusDays(10));

        @BeforeEach
        void proposition() {
            enAttente = new QualityObjectiveProposal();
            enAttente.setId(PROP);
            enAttente.setTenantId(TENANT);
            enAttente.setObjectiveId(OBJ);
            enAttente.setLevel(ObjectiveLevel.CRITICAL);
            enAttente.setPeriodStart(Instant.parse("2026-10-01T00:00:00Z"));
            enAttente.setPeriodEnd(Instant.parse("2026-11-01T00:00:00Z"));
            enAttente.setObservedValue(new BigDecimal("900"));
            enAttente.setBreachPeriods(1);
            enAttente.setTitle("Objectif qualité critique — Taux de NC client");
            enAttente.setDescription("…");
            enAttente.setEffectivenessCriterion("Le KPI revient à ou sous 500 ppm…");
            enAttente.setStatus(ProposalStatus.PENDING);
            enAttente.setCreatedAt(Instant.now());
            when(proposals.findByIdAndTenantId(PROP, TENANT)).thenReturn(Optional.of(enAttente));
            when(proposals.findByTenantIdAndObjectiveIdOrderByCreatedAtDesc(TENANT, OBJ))
                    .thenReturn(List.of(enAttente));
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(objectif()));
            when(capaService.openCase(any(), any(), any())).thenReturn(dossierCree());
            releves("2026-10", "900");
        }

        private CapaDto.OpenCaseRequest ouvrir() {
            service.openCapa(OBJ, ACTION);
            ArgumentCaptor<CapaDto.OpenCaseRequest> c =
                    ArgumentCaptor.forClass(CapaDto.OpenCaseRequest.class);
            verify(capaService).openCase(c.capture(), any(), any());
            return c.getValue();
        }

        @Test
        void leDossierNaitDeLActionSaisie_etRienNEstPreredige() {
            // La recette l'a demandé ainsi : un dossier prérempli par la règle
            // se signait sans être lu, et son action ne disait pas ce qu'on
            // allait vraiment faire.
            CapaDto.OpenCaseRequest req = ouvrir();
            assertThat(req.title()).isEqualTo("Recaler le réglage de la presse 3");
            assertThat(req.description()).isEqualTo(ACTION.description());
            assertThat(req.title()).isNotEqualTo(enAttente.getTitle());
        }

        @Test
        void laPropositionEnAttenteEstSoldeeParLeDossier() {
            ObjectiveDto.CapaOpenedResponse out = service.openCapa(OBJ, ACTION);
            assertThat(out.capaId()).isEqualTo(CASE);
            assertThat(out.proposalId()).isEqualTo(PROP);
            assertThat(enAttente.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
            assertThat(enAttente.getCapaCaseId()).isEqualTo(CASE);
            assertThat(enAttente.getDecidedBy()).isEqualTo(ACTOR);
        }

        @Test
        void sansProposition_onPeutQuandMemeAgir() {
            // La règle ne propose qu'au bout de ses périodes consécutives ; la
            // recette veut le bouton dès la première ligne passée en alerte.
            when(proposals.findByTenantIdAndObjectiveIdOrderByCreatedAtDesc(TENANT, OBJ))
                    .thenReturn(List.of());
            ObjectiveDto.CapaOpenedResponse out = service.openCapa(OBJ, ACTION);
            assertThat(out.capaId()).isEqualTo(CASE);
            assertThat(out.proposalId()).isNull();
        }

        @Test
        void leDossierNaitCorrectif_surLaSourceObjectifQualite() {
            service.openCapa(OBJ, ACTION);
            ArgumentCaptor<CapaDto.OpenCaseRequest> c =
                    ArgumentCaptor.forClass(CapaDto.OpenCaseRequest.class);
            verify(capaService).openCase(c.capture(),
                    eq(CapaSourceType.QUALITY_OBJECTIVE), any());
            // Un écart déjà survenu se traite par un correctif : le nommer
            // préventif laisserait croire qu'il ne s'est pas produit.
            assertThat(c.getValue().type()).isEqualTo(CapaType.CORRECTIVE);
            assertThat(c.getValue().sourceType()).isEqualTo(CapaSourceType.QUALITY_OBJECTIVE);
            assertThat(c.getValue().sourceRef()).startsWith("objective:" + OBJ + ":CRITICAL:");
        }

        @Test
        void laVerificationDEfficaciteEstOBLIGATOIRE_sansQuestionPosee() {
            // Contrairement aux autres dossiers, où elle se décide à l'ouverture :
            // un écart d'objectif ne se referme pas sur une déclaration.
            CapaDto.OpenCaseRequest req = ouvrir();
            assertThat(req.verificationRequired()).isTrue();
            assertThat(req.verificationAssigneeId()).isEqualTo(PILOTE);
        }

        @Test
        void leCritereEstEcritParLaRegle_etNonParLaFenetre() {
            // Verrouillé : le laisser saisir reviendrait à clore sur une cible
            // plus facile que celle qu'on s'était donnée.
            CapaDto.OpenCaseRequest req = ouvrir();
            assertThat(req.effectivenessCriterion())
                    .contains("Taux de NC client")
                    .contains("500")
                    .contains("2 période(s)");
        }

        @Test
        void lInstructionDeVerificationDitQuAucunCalculNEstADemander() {
            // Le vérificateur n'a rien à mesurer : la plateforme compte les
            // périodes. Sans cela, il pourrait conclure de son côté.
            CapaDto.OpenCaseRequest req = ouvrir();
            assertThat(req.verificationInstructions())
                    .contains("Taux de NC client")
                    .contains("2 période(s)")
                    .contains("Aucun calcul");
            // Le critère et l'instruction ne se confondent pas : l'un dit à quoi
            // on reconnaîtra l'effet, l'autre ce qu'il y a à faire.
            assertThat(req.verificationInstructions())
                    .isNotEqualTo(req.effectivenessCriterion());
        }

        @Test
        void leDossierEstConfieAQuiLaFenetreDesigne_nommeEtPasEnUuid() {
            // Le pilote n'est plus porté par l'objectif : il se choisit au
            // moment où l'on décide d'agir (ADR 0143).
            CapaDto.OpenCaseRequest req = ouvrir();
            assertThat(req.ownerId()).isEqualTo(PILOTE);
            assertThat(req.ownerName()).isEqualTo("Awa Diallo");
        }

        @Test
        void lEcheanceEstCelleQuOnASaisie() {
            // Et non un délai réglé des mois plus tôt sur l'objectif.
            assertThat(ouvrir().dueDate()).isEqualTo(ACTION.dueDate());
        }

        @Test
        void laCriticiteSuitLeNiveauAtteint() {
            assertThat(ouvrir().criticity()).isEqualTo(ObjectiveLevel.CRITICAL.criticity());
        }

        @Test
        void unObjectifAtteint_nOuvreRien() {
            // Il n'y a pas d'écart à traiter : le bouton n'est pas proposé, et
            // le serveur refuse si on l'appelle quand même.
            releves("2026-10", "100");
            assertThatThrownBy(() -> service.openCapa(OBJ, ACTION))
                    .isInstanceOf(ObjectiveStateException.class)
                    .hasMessageContaining("not in alert or critical");
            verify(capaService, never()).openCase(any(), any(), any());
        }

        @Test
        void unObjectifArchive_nOuvrePlusRien() {
            QualityObjective archive = objectif();
            archive.setStatus(ObjectiveLifecycle.ARCHIVED);
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(archive));
            assertThatThrownBy(() -> service.openCapa(OBJ, ACTION))
                    .isInstanceOf(ObjectiveStateException.class);
        }

        @Test
        void rejeter_exigeUnMotif() {
            assertThatThrownBy(() -> service.reject(PROP, "   "))
                    .isInstanceOf(ObjectiveValidationException.class)
                    .hasMessageContaining("requires a justification");
            assertThatThrownBy(() -> service.reject(PROP, null))
                    .isInstanceOf(ObjectiveValidationException.class);
            verify(proposals, never()).save(any());
        }

        @Test
        void rejeter_garderLeMotif_etAucunDossierNEstCree() {
            ObjectiveDto.ProposalResponse out =
                    service.reject(PROP, "Cause déjà traitée par la CAPA-039 ");
            assertThat(out.status()).isEqualTo(ProposalStatus.REJECTED);
            assertThat(out.rejectionReason()).isEqualTo("Cause déjà traitée par la CAPA-039");
            assertThat(out.capaCaseId()).isNull();
            verify(capaService, never()).openCase(any(), any(), any());
        }

        @Test
        void rejeterDeuxFois_estRefuse() {
            enAttente.setStatus(ProposalStatus.REJECTED);
            assertThatThrownBy(() -> service.reject(PROP, "motif"))
                    .isInstanceOf(ObjectiveStateException.class);
        }

        @Test
        void unePropositionInconnue_estIntrouvable() {
            when(proposals.findByIdAndTenantId(PROP, TENANT)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.reject(PROP, "motif"))
                    .isInstanceOf(ProposalNotFoundException.class);
        }
    }

    // ---------- la mémoire de l'écart ----------

    @Nested
    @DisplayName("Tant que le dossier est ouvert, l'objectif ne repasse pas atteint")
    class Memoire {

        private void propositionAcceptee(CapaStatus statutDuDossier) {
            QualityObjectiveProposal acceptee = new QualityObjectiveProposal();
            acceptee.setId(PROP);
            acceptee.setTenantId(TENANT);
            acceptee.setObjectiveId(OBJ);
            acceptee.setLevel(ObjectiveLevel.CRITICAL);
            acceptee.setStatus(ProposalStatus.ACCEPTED);
            acceptee.setCapaCaseId(CASE);
            when(proposals.findByTenantIdAndObjectiveIdAndStatus(
                    TENANT, OBJ, ProposalStatus.ACCEPTED)).thenReturn(List.of(acceptee));
            CapaCase dossier = new CapaCase();
            dossier.setId(CASE);
            dossier.setTenantId(TENANT);
            dossier.setStatus(statutDuDossier);
            when(capaCases.findByIdAndTenantId(CASE, TENANT)).thenReturn(Optional.of(dossier));
        }

        @Test
        void dossierOuvert_unSeulMoisAuVertNeSuffitPas() {
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(objectif()));
            propositionAcceptee(CapaStatus.IN_PROGRESS);
            releves("2026-09", "900", "2026-10", "480");

            ObjectiveDto.AssessmentResponse a = service.get(OBJ).assessment();
            assertThat(a.heldByOpenCapa()).isTrue();
            assertThat(a.recoveryAchieved()).isFalse();
            assertThat(a.status()).isEqualTo(ObjectiveStatus.CRITICAL);
        }

        @Test
        void dossierOuvert_laDureeExigeeAtteinte_lObjectifRepasseAtteint() {
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(objectif()));
            propositionAcceptee(CapaStatus.RESOLVED);
            releves("2026-08", "900", "2026-09", "480", "2026-10", "470");

            ObjectiveDto.AssessmentResponse a = service.get(OBJ).assessment();
            assertThat(a.onTargetPeriods()).isEqualTo(2);
            assertThat(a.recoveryAchieved()).isTrue();
            assertThat(a.status()).isEqualTo(ObjectiveStatus.ON_TARGET);
        }

        @Test
        void dossierClos_neMaintientPlusRien() {
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(objectif()));
            propositionAcceptee(CapaStatus.CLOSED);
            releves("2026-09", "900", "2026-10", "480");
            assertThat(service.get(OBJ).assessment().heldByOpenCapa()).isFalse();
        }

        @Test
        void dossierRejete_neMaintientPlusRien() {
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(objectif()));
            propositionAcceptee(CapaStatus.REJECTED);
            releves("2026-09", "900", "2026-10", "480");
            assertThat(service.get(OBJ).assessment().heldByOpenCapa()).isFalse();
        }

        @Test
        void dossierIntrouvable_neMaintientRien() {
            // On ne bloque pas un objectif sur une référence morte.
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(objectif()));
            propositionAcceptee(CapaStatus.OPEN);
            when(capaCases.findByIdAndTenantId(CASE, TENANT)).thenReturn(Optional.empty());
            releves("2026-09", "900", "2026-10", "480");
            assertThat(service.get(OBJ).assessment().heldByOpenCapa()).isFalse();
        }
    }

    @Nested
    @DisplayName("L'historique")
    class Historique {

        @Test
        void lHistoriqueDUnObjectif_porteSonTitre() {
            when(objectives.findByIdAndTenantId(OBJ, TENANT)).thenReturn(Optional.of(objectif()));
            QualityObjectiveProposal p = new QualityObjectiveProposal();
            p.setId(PROP);
            p.setTenantId(TENANT);
            p.setObjectiveId(OBJ);
            p.setLevel(ObjectiveLevel.WARNING);
            p.setStatus(ProposalStatus.REJECTED);
            p.setRejectionReason("motif");
            p.setCreatedAt(Instant.now());
            when(proposals.findByTenantIdAndObjectiveIdOrderByCreatedAtDesc(TENANT, OBJ))
                    .thenReturn(List.of(p));
            List<ObjectiveDto.ProposalResponse> h = service.history(OBJ);
            assertThat(h).hasSize(1);
            assertThat(h.get(0).objectiveTitle()).isEqualTo("Taux de NC client");
            assertThat(h.get(0).rejectionReason()).isEqualTo("motif");
        }
    }
}
