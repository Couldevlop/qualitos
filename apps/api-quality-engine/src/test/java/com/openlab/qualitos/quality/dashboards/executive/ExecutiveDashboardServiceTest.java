package com.openlab.qualitos.quality.dashboards.executive;

import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.kpi.KpiDefinition;
import com.openlab.qualitos.quality.kpi.KpiDefinitionRepository;
import com.openlab.qualitos.quality.kpi.KpiDirection;
import com.openlab.qualitos.quality.kpi.KpiHealth;
import com.openlab.qualitos.quality.kpi.KpiMeasurement;
import com.openlab.qualitos.quality.kpi.KpiMeasurementRepository;
import com.openlab.qualitos.quality.kpi.KpiStatus;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import com.openlab.qualitos.quality.risk.FmeaItem;
import com.openlab.qualitos.quality.risk.FmeaItemRepository;
import com.openlab.qualitos.quality.standards.AdoptionStatus;
import com.openlab.qualitos.quality.standards.Standard;
import com.openlab.qualitos.quality.standards.StandardsDto;
import com.openlab.qualitos.quality.standards.StandardsService;
import com.openlab.qualitos.quality.standards.TenantStandard;
import com.openlab.qualitos.quality.standards.TenantStandardRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests du Dashboard exécutif (§7.1).
 *
 * <p>Le point dur couvert ici est que la page ne fabrique plus aucun chiffre : chaque
 * bloc doit refléter les données du tenant, et un tenant vide doit produire des sections
 * vides plutôt qu'une erreur.
 */
@ExtendWith(MockitoExtension.class)
// Chaque test ne renseigne que les dépôts qui l'intéressent ; les autres renvoient
// leur valeur par défaut (liste vide / 0). Le strict stubbing jugerait ça suspect.
@MockitoSettings(strictness = Strictness.LENIENT)
class ExecutiveDashboardServiceTest {

    @Mock KpiDefinitionRepository kpiRepo;
    @Mock KpiMeasurementRepository measurementRepo;
    @Mock NonConformityRepository ncRepo;
    @Mock FmeaItemRepository fmeaItemRepo;
    @Mock CapaCaseRepository capaRepo;
    @Mock TenantStandardRepository adoptionRepo;
    @Mock StandardsService standardsService;

    ExecutiveDashboardService service;

    static final UUID TENANT = UUID.randomUUID();
    static final LocalDate TODAY = LocalDate.parse("2026-05-15");
    static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT.toString());
        when(kpiRepo.findByTenantIdAndStatus(any(), any(), any())).thenReturn(Page.empty());
        when(adoptionRepo.findByTenantId(any(), any())).thenReturn(Page.empty());
        service = new ExecutiveDashboardService(kpiRepo, measurementRepo, ncRepo, fmeaItemRepo,
                capaRepo, adoptionRepo, standardsService, CLOCK);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---- Tenant ---------------------------------------------------------------------

    @Test
    void rejette_l_appel_sans_tenant_dans_le_contexte() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.overview())
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void un_tenant_vide_rend_un_dashboard_vide_et_non_une_erreur() {
        var dashboard = service.overview();

        assertThat(dashboard.kpis()).isEmpty();
        assertThat(dashboard.qualityTrend()).isEmpty();
        assertThat(dashboard.defectsByCategory()).isEmpty();
        assertThat(dashboard.topRisks()).isEmpty();
        assertThat(dashboard.alignment()).isEmpty();
        assertThat(dashboard.generatedAt()).isEqualTo(CLOCK.instant());
    }

    // ---- Cartes KPI -----------------------------------------------------------------

    @Test
    void une_carte_kpi_porte_la_derniere_mesure_sa_cible_et_l_ecart_a_la_precedente() {
        KpiDefinition kpi = kpi("FPY", "First Pass Yield", new BigDecimal("95"));
        givenActiveKpis(kpi);
        // Le dépôt renvoie du plus récent au plus ancien.
        givenMeasurements(kpi, measurement(kpi, "94.2", "2026-05-01T00:00:00Z"),
                               measurement(kpi, "92.1", "2026-04-01T00:00:00Z"));

        var card = service.overview().kpis().get(0);

        assertThat(card.code()).isEqualTo("FPY");
        assertThat(card.value()).isEqualByComparingTo("94.2");
        assertThat(card.targetValue()).isEqualByComparingTo("95");
        assertThat(card.trendDelta()).isEqualByComparingTo("2.1");
        assertThat(card.health()).isNotNull();
    }

    @Test
    void une_carte_sans_mesure_reste_affichable_avec_une_sante_inconnue() {
        KpiDefinition kpi = kpi("COQ", "Coût d'obtention de la qualité", new BigDecimal("3.2"));
        givenActiveKpis(kpi);
        givenMeasurements(kpi);

        var card = service.overview().kpis().get(0);

        assertThat(card.value()).isNull();
        assertThat(card.trendDelta()).isNull();
        assertThat(card.latestPeriodStart()).isNull();
        assertThat(card.health()).isEqualTo(KpiHealth.UNKNOWN);
    }

    @Test
    void une_seule_mesure_ne_produit_pas_d_ecart_de_tendance() {
        KpiDefinition kpi = kpi("NC", "Non-conformités", new BigDecimal("100"));
        givenActiveKpis(kpi);
        givenMeasurements(kpi, measurement(kpi, "127", "2026-05-01T00:00:00Z"));

        assertThat(service.overview().kpis().get(0).trendDelta()).isNull();
    }

    @Test
    void le_nombre_de_cartes_est_borne_a_douze_conformement_au_paragraphe_6_1() {
        service.overview();
        // La borne est portée par la requête paginée : on vérifie la taille demandée.
        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(kpiRepo)
                .findByTenantIdAndStatus(eq(TENANT), eq(KpiStatus.ACTIVE), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(ExecutiveDashboardService.MAX_KPI_CARDS);
    }

    // ---- Tendance -------------------------------------------------------------------

    @Test
    void la_tendance_du_kpi_de_tete_est_rendue_en_ordre_chronologique() {
        KpiDefinition kpi = kpi("FPY", "First Pass Yield", new BigDecimal("95"));
        givenActiveKpis(kpi);
        givenMeasurements(kpi, measurement(kpi, "94.2", "2026-05-01T00:00:00Z"),
                               measurement(kpi, "92.1", "2026-04-01T00:00:00Z"),
                               measurement(kpi, "90.0", "2026-03-01T00:00:00Z"));

        var trend = service.overview().qualityTrend();

        assertThat(trend).hasSize(3);
        assertThat(trend.get(0).value()).isEqualByComparingTo("90.0");
        assertThat(trend.get(2).value()).isEqualByComparingTo("94.2");
        assertThat(trend.get(0).targetValue()).isEqualByComparingTo("95");
    }

    @Test
    void la_tendance_est_bornee_a_douze_points() {
        KpiDefinition kpi = kpi("FPY", "First Pass Yield", new BigDecimal("95"));
        givenActiveKpis(kpi);
        List<KpiMeasurement> many = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            many.add(measurement(kpi, String.valueOf(80 + i), "2026-05-01T00:00:00Z"));
        }
        when(measurementRepo.findTop24ByKpiIdOrderByPeriodStartDesc(kpi.getId())).thenReturn(many);

        assertThat(service.overview().qualityTrend())
                .hasSize(ExecutiveDashboardService.TREND_POINTS);
    }

    // ---- Défauts --------------------------------------------------------------------

    @Test
    void les_defauts_sont_tries_par_volume_et_les_categories_vides_sont_omises() {
        when(ncRepo.countByTenantIdAndCategoryAndStatusNotIn(eq(TENANT), eq(NcCategory.PROCESS), any()))
                .thenReturn(31L);
        when(ncRepo.countByTenantIdAndCategoryAndStatusNotIn(eq(TENANT), eq(NcCategory.PRODUCT), any()))
                .thenReturn(42L);

        var defects = service.overview().defectsByCategory();

        assertThat(defects).hasSize(2);
        assertThat(defects.get(0).category()).isEqualTo("PRODUCT");
        assertThat(defects.get(0).count()).isEqualTo(42L);
        assertThat(defects.get(1).category()).isEqualTo("PROCESS");
    }

    // ---- Risques --------------------------------------------------------------------

    @Test
    void les_risques_fusionnent_fmea_et_capa_en_retard_et_sont_tries_par_gravite() {
        when(fmeaItemRepo.findTop10ByTenantIdAndRpnGreaterThanEqualOrderByRpnDesc(eq(TENANT), anyInt()))
                .thenReturn(List.of(fmeaItem("Rupture de joint", 240), fmeaItem("Usure outil", 120)));
        when(capaRepo.findTop10ByTenantIdAndStatusNotInAndDueDateBeforeOrderByDueDateAsc(
                eq(TENANT), any(), eq(TODAY)))
                .thenReturn(List.of(capa("Audit ISO en retard", CapaCriticity.HIGH)));

        var risks = service.overview().topRisks();

        assertThat(risks).hasSize(3);
        assertThat(risks.get(0).severity()).isEqualTo("CRITICAL");
        assertThat(risks.get(0).source()).isEqualTo("FMEA");
        assertThat(risks.get(0).rpn()).isEqualTo(240);
        assertThat(risks).extracting(ExecutiveDashboardDto.TopRisk::source).contains("CAPA");
    }

    @Test
    void une_capa_en_retard_mais_peu_critique_n_est_pas_remontee_a_la_direction() {
        when(capaRepo.findTop10ByTenantIdAndStatusNotInAndDueDateBeforeOrderByDueDateAsc(
                eq(TENANT), any(), eq(TODAY)))
                .thenReturn(List.of(capa("Étiquetage à revoir", CapaCriticity.LOW),
                                    capa("Formation à replanifier", CapaCriticity.MEDIUM)));

        assertThat(service.overview().topRisks()).isEmpty();
    }

    @Test
    void le_nombre_de_risques_remontes_est_borne() {
        List<FmeaItem> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            items.add(fmeaItem("Mode " + i, 300));
        }
        when(fmeaItemRepo.findTop10ByTenantIdAndRpnGreaterThanEqualOrderByRpnDesc(eq(TENANT), anyInt()))
                .thenReturn(items);

        assertThat(service.overview().topRisks())
                .hasSize(ExecutiveDashboardService.MAX_TOP_RISKS);
    }

    // ---- Alignement -----------------------------------------------------------------

    @Test
    void l_alignement_reprend_le_score_calcule_par_le_standards_hub() {
        TenantStandard adoption = adoption("iso-9001", "ISO 9001:2015");
        when(adoptionRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(adoption)));
        when(standardsService.computeAlignment(adoption.getId()))
                .thenReturn(new StandardsDto.AlignmentReport(adoption.getId(), UUID.randomUUID(),
                        "iso-9001", 76.0, 100, 76, 50, 40,
                        List.of(new StandardsDto.SectionAlignment(UUID.randomUUID(), "4",
                                "Contexte de l'organisme", 88.0, 10, 9, List.of()))));

        var bars = service.overview().alignment();

        assertThat(bars).hasSize(1);
        assertThat(bars.get(0).standardCode()).isEqualTo("iso-9001");
        assertThat(bars.get(0).standardName()).isEqualTo("ISO 9001:2015");
        assertThat(bars.get(0).score()).isEqualTo(76.0);
        assertThat(bars.get(0).status()).isEqualTo(AdoptionStatus.IN_PROGRESS.name());
        // Le détail par section alimente la heatmap de conformité (§7.1).
        assertThat(bars.get(0).sections()).singleElement()
                .satisfies(s -> {
                    assertThat(s.sectionCode()).isEqualTo("4");
                    assertThat(s.score()).isEqualTo(88.0);
                });
    }

    @Test
    void une_norme_dont_le_calcul_echoue_est_omise_sans_faire_tomber_le_dashboard() {
        TenantStandard ok = adoption("iso-9001", "ISO 9001:2015");
        TenantStandard broken = adoption("iso-27001", "ISO/IEC 27001:2022");
        when(adoptionRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(broken, ok)));
        when(standardsService.computeAlignment(broken.getId()))
                .thenThrow(new IllegalStateException("référentiel incomplet"));
        when(standardsService.computeAlignment(ok.getId()))
                .thenReturn(new StandardsDto.AlignmentReport(ok.getId(), UUID.randomUUID(),
                        "iso-9001", 76.0, 100, 76, 50, 40, List.of()));

        var bars = service.overview().alignment();

        assertThat(bars).hasSize(1);
        assertThat(bars.get(0).standardCode()).isEqualTo("iso-9001");
    }

    // ---- Fixtures -------------------------------------------------------------------

    private void givenActiveKpis(KpiDefinition... kpis) {
        when(kpiRepo.findByTenantIdAndStatus(eq(TENANT), eq(KpiStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of(kpis)));
    }

    private void givenMeasurements(KpiDefinition kpi, KpiMeasurement... measurements) {
        when(measurementRepo.findTop24ByKpiIdOrderByPeriodStartDesc(kpi.getId()))
                .thenReturn(List.of(measurements));
    }

    private static KpiDefinition kpi(String code, String name, BigDecimal target) {
        KpiDefinition d = new KpiDefinition();
        d.setId(UUID.randomUUID());
        d.setTenantId(TENANT);
        d.setCode(code);
        d.setName(name);
        d.setDescription("Défini au catalogue");
        d.setCategory("quality");
        d.setUnit("%");
        d.setDirection(KpiDirection.HIGHER_IS_BETTER);
        d.setStatus(KpiStatus.ACTIVE);
        d.setTargetValue(target);
        return d;
    }

    private static KpiMeasurement measurement(KpiDefinition kpi, String value, String periodStart) {
        KpiMeasurement m = new KpiMeasurement();
        m.setId(UUID.randomUUID());
        m.setKpiId(kpi.getId());
        m.setTenantId(TENANT);
        m.setValue(new BigDecimal(value));
        m.setPeriodStart(Instant.parse(periodStart));
        m.setPeriodEnd(Instant.parse(periodStart).plusSeconds(86_400));
        return m;
    }

    private static FmeaItem fmeaItem(String failureMode, int rpn) {
        FmeaItem item = new FmeaItem();
        item.setId(UUID.randomUUID());
        item.setTenantId(TENANT);
        item.setFailureMode(failureMode);
        item.setRpn(rpn);
        item.setActionDueDate(TODAY.plusDays(30));
        return item;
    }

    private static CapaCase capa(String title, CapaCriticity criticity) {
        CapaCase c = new CapaCase();
        c.setId(UUID.randomUUID());
        c.setTenantId(TENANT);
        c.setTitle(title);
        c.setCriticity(criticity);
        c.setDueDate(TODAY.minusDays(5));
        return c;
    }

    private static TenantStandard adoption(String code, String fullName) {
        Standard standard = new Standard();
        standard.setId(UUID.randomUUID());
        standard.setCode(code);
        standard.setFullName(fullName);

        TenantStandard ts = new TenantStandard();
        ts.setId(UUID.randomUUID());
        ts.setTenantId(TENANT);
        ts.setStandard(standard);
        ts.setStatus(AdoptionStatus.IN_PROGRESS);
        return ts;
    }

    /** Rappel de typage : le dépôt NC prend une collection de statuts exclus. */
    @SuppressWarnings("unused")
    private static Collection<?> unusedTypeHint() {
        return List.of();
    }
}
