package com.openlab.qualitos.quality.dashboards.executive;

import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.kpi.KpiDefinition;
import com.openlab.qualitos.quality.kpi.KpiDefinitionRepository;
import com.openlab.qualitos.quality.kpi.KpiEvaluator;
import com.openlab.qualitos.quality.kpi.KpiHealth;
import com.openlab.qualitos.quality.kpi.KpiMeasurement;
import com.openlab.qualitos.quality.kpi.KpiMeasurementRepository;
import com.openlab.qualitos.quality.kpi.KpiStatus;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import com.openlab.qualitos.quality.nonconformity.NcStatus;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import com.openlab.qualitos.quality.risk.FmeaItem;
import com.openlab.qualitos.quality.risk.FmeaItemRepository;
import com.openlab.qualitos.quality.standards.StandardsDto;
import com.openlab.qualitos.quality.standards.StandardsService;
import com.openlab.qualitos.quality.standards.TenantStandard;
import com.openlab.qualitos.quality.standards.TenantStandardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Agrégation du Dashboard exécutif (CLAUDE.md §7.1).
 *
 * <p>Ce service remplace des données de démonstration codées en dur côté Angular : les
 * KPIs, la tendance, la répartition des défauts, les risques majeurs et l'alignement
 * normatif sont désormais calculés à partir des données réelles du tenant.
 *
 * <p>Conception :
 * <ul>
 *   <li><b>Le catalogue KPI est la source des cartes.</b> On n'invente pas une liste
 *       d'indicateurs « stratégiques » côté serveur : on remonte les KPI ACTIFS du
 *       catalogue du tenant, chacun portant déjà sa définition (cible, seuils, unité,
 *       propriétaire). C'est ce qui rend l'invariant §18.2 #8 structurel plutôt que
 *       déclaratif. Bornage à 12 cartes, conformément au §6.1 (« pas plus de 8-12 KPIs
 *       sur le dashboard exécutif », anti-analysis-paralysis).</li>
 *   <li><b>Lecture seule et tenant-scopée.</b> Le tenant vient du JWT via
 *       {@link TenantContext} (règle §18.2 #2) ; chaque requête filtre dessus.</li>
 *   <li><b>Dégradation propre.</b> Un tenant neuf n'a ni KPI, ni NC, ni FMEA : la page
 *       doit afficher des sections vides, pas une erreur. Chaque bloc est donc calculé
 *       indépendamment et une section en échec ne fait pas tomber le dashboard.</li>
 * </ul>
 */
@Service
public class ExecutiveDashboardService {

    private static final Logger log = LoggerFactory.getLogger(ExecutiveDashboardService.class);

    /** §6.1 : 8-12 KPIs maximum sur la vue exécutive. */
    static final int MAX_KPI_CARDS = 12;

    /** Nombre de points de tendance remontés (12 mois de recul, §7.1). */
    static final int TREND_POINTS = 12;

    /** Nombre de risques remontés au comité. */
    static final int MAX_TOP_RISKS = 5;

    /** Nombre de normes affichées dans la barre d'alignement. */
    static final int MAX_ALIGNMENT_BARS = 8;

    /** Au-delà de ce RPN, un item FMEA est considéré comme un risque de direction. */
    static final int RPN_ALERT_THRESHOLD = 100;

    private final KpiDefinitionRepository kpiRepo;
    private final KpiMeasurementRepository measurementRepo;
    private final NonConformityRepository ncRepo;
    private final FmeaItemRepository fmeaItemRepo;
    private final CapaCaseRepository capaRepo;
    private final TenantStandardRepository adoptionRepo;
    private final StandardsService standardsService;
    private final Clock clock;

    // @Autowired est INDISPENSABLE : la classe déclare deux constructeurs (le second
    // pour injecter une horloge en test). Sans annotation, Spring ne sait pas lequel
    // choisir, se rabat sur le constructeur par défaut — inexistant — et le contexte ne
    // démarre plus. C'est précisément ce que QualityEngineContextLoadsTest verrouille.
    @org.springframework.beans.factory.annotation.Autowired
    public ExecutiveDashboardService(KpiDefinitionRepository kpiRepo,
                                     KpiMeasurementRepository measurementRepo,
                                     NonConformityRepository ncRepo,
                                     FmeaItemRepository fmeaItemRepo,
                                     CapaCaseRepository capaRepo,
                                     TenantStandardRepository adoptionRepo,
                                     StandardsService standardsService) {
        this(kpiRepo, measurementRepo, ncRepo, fmeaItemRepo, capaRepo, adoptionRepo,
                standardsService, Clock.systemUTC());
    }

    /** Variante testable : horloge injectée (échéances CAPA en retard). */
    ExecutiveDashboardService(KpiDefinitionRepository kpiRepo,
                              KpiMeasurementRepository measurementRepo,
                              NonConformityRepository ncRepo,
                              FmeaItemRepository fmeaItemRepo,
                              CapaCaseRepository capaRepo,
                              TenantStandardRepository adoptionRepo,
                              StandardsService standardsService,
                              Clock clock) {
        this.kpiRepo = kpiRepo;
        this.measurementRepo = measurementRepo;
        this.ncRepo = ncRepo;
        this.fmeaItemRepo = fmeaItemRepo;
        this.capaRepo = capaRepo;
        this.adoptionRepo = adoptionRepo;
        this.standardsService = standardsService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ExecutiveDashboardDto.ExecutiveDashboard overview() {
        UUID tenantId = requireTenantId();

        List<KpiDefinition> activeKpis = kpiRepo
                .findByTenantIdAndStatus(tenantId, KpiStatus.ACTIVE,
                        PageRequest.of(0, MAX_KPI_CARDS, Sort.by(Sort.Direction.ASC, "code")))
                .getContent();

        List<ExecutiveDashboardDto.ExecutiveKpiCard> cards = new ArrayList<>(activeKpis.size());
        for (KpiDefinition kpi : activeKpis) {
            cards.add(toCard(kpi));
        }

        return new ExecutiveDashboardDto.ExecutiveDashboard(
                cards,
                qualityTrend(activeKpis),
                defectsByCategory(tenantId),
                topRisks(tenantId),
                alignment(tenantId),
                clock.instant());
    }

    // ---- Cartes KPI -----------------------------------------------------------------

    private ExecutiveDashboardDto.ExecutiveKpiCard toCard(KpiDefinition kpi) {
        List<KpiMeasurement> recent = measurementRepo.findTop24ByKpiIdOrderByPeriodStartDesc(kpi.getId());

        KpiMeasurement latest = recent.isEmpty() ? null : recent.get(0);
        KpiMeasurement previous = recent.size() > 1 ? recent.get(1) : null;

        BigDecimal value = latest == null ? null : latest.getValue();
        BigDecimal delta = (latest == null || previous == null)
                ? null
                : latest.getValue().subtract(previous.getValue());
        KpiHealth health = latest == null
                ? KpiHealth.UNKNOWN
                : KpiEvaluator.evaluate(kpi, latest.getValue());

        return new ExecutiveDashboardDto.ExecutiveKpiCard(
                kpi.getId(), kpi.getCode(), kpi.getName(), kpi.getDescription(),
                kpi.getCategory(), kpi.getUnit(), kpi.getDirection(),
                value, kpi.getTargetValue(), delta, health,
                latest == null ? null : latest.getPeriodStart(),
                latest == null ? null : latest.getPeriodEnd());
    }

    // ---- Tendance -------------------------------------------------------------------

    /**
     * Tendance du KPI de tête (premier KPI actif par code). Le dashboard affiche une seule
     * courbe de synthèse ; le détail par KPI reste accessible via {@code /api/v1/kpis/{id}/trend}.
     */
    private List<ExecutiveDashboardDto.TrendPoint> qualityTrend(List<KpiDefinition> activeKpis) {
        if (activeKpis.isEmpty()) {
            return List.of();
        }
        KpiDefinition primary = activeKpis.get(0);
        List<KpiMeasurement> recent = measurementRepo.findTop24ByKpiIdOrderByPeriodStartDesc(primary.getId());

        // Le dépôt renvoie du plus récent au plus ancien : on borne puis on inverse pour
        // obtenir un ordre chronologique directement consommable par un graphique.
        int size = Math.min(recent.size(), TREND_POINTS);
        List<ExecutiveDashboardDto.TrendPoint> points = new ArrayList<>(size);
        for (int i = size - 1; i >= 0; i--) {
            KpiMeasurement m = recent.get(i);
            points.add(new ExecutiveDashboardDto.TrendPoint(
                    m.getPeriodStart(), m.getValue(), primary.getTargetValue(),
                    KpiEvaluator.evaluate(primary, m.getValue())));
        }
        return points;
    }

    // ---- Défauts par catégorie ------------------------------------------------------

    /**
     * Non-conformités NON clôturées par catégorie. Les catégories sans occurrence sont
     * omises : une barre à zéro sur un dashboard de direction est du bruit.
     */
    private List<ExecutiveDashboardDto.DefectByCategory> defectsByCategory(UUID tenantId) {
        List<ExecutiveDashboardDto.DefectByCategory> result = new ArrayList<>();
        for (NcCategory category : NcCategory.values()) {
            long count = ncRepo.countByTenantIdAndCategoryAndStatusNotIn(
                    tenantId, category, CLOSED_NC_STATUSES);
            if (count > 0) {
                result.add(new ExecutiveDashboardDto.DefectByCategory(category.name(), count));
            }
        }
        result.sort(Comparator.comparingLong(
                (ExecutiveDashboardDto.DefectByCategory d) -> d.count()).reversed());
        return result;
    }

    /** Statuts considérés comme « sortis du flux » : ils ne comptent plus comme défaut ouvert. */
    private static final List<NcStatus> CLOSED_NC_STATUSES = List.of(NcStatus.CLOSED, NcStatus.CANCELLED);

    // ---- Risques majeurs ------------------------------------------------------------

    /**
     * Deux sources réelles fusionnées puis triées par gravité :
     * items FMEA au-dessus du seuil de RPN, et CAPA critiques en retard.
     */
    private List<ExecutiveDashboardDto.TopRisk> topRisks(UUID tenantId) {
        List<ExecutiveDashboardDto.TopRisk> risks = new ArrayList<>();

        for (FmeaItem item : fmeaItemRepo.findTop10ByTenantIdAndRpnGreaterThanEqualOrderByRpnDesc(
                tenantId, RPN_ALERT_THRESHOLD)) {
            risks.add(new ExecutiveDashboardDto.TopRisk(
                    item.getId(),
                    item.getFailureMode(),
                    "FMEA",
                    severityOf(item.getRpn()),
                    item.getRpn(),
                    item.getActionDueDate() == null ? null : item.getActionDueDate()
                            .atStartOfDay(clock.getZone()).toInstant()));
        }

        LocalDate today = LocalDate.now(clock);
        for (CapaCase capa : capaRepo.findTop10ByTenantIdAndStatusNotInAndDueDateBeforeOrderByDueDateAsc(
                tenantId, CLOSED_CAPA_STATUSES, today)) {
            if (capa.getCriticity() == CapaCriticity.HIGH || capa.getCriticity() == CapaCriticity.CRITICAL) {
                risks.add(new ExecutiveDashboardDto.TopRisk(
                        capa.getId(),
                        capa.getTitle(),
                        "CAPA",
                        capa.getCriticity().name(),
                        null,
                        capa.getDueDate().atStartOfDay(clock.getZone()).toInstant()));
            }
        }

        risks.sort(Comparator.comparingInt((ExecutiveDashboardDto.TopRisk r) ->
                severityRank(r.severity())).reversed());
        return risks.size() > MAX_TOP_RISKS ? List.copyOf(risks.subList(0, MAX_TOP_RISKS)) : List.copyOf(risks);
    }

    /** Statuts CAPA terminaux : plus rien n'est « en retard » une fois là. */
    private static final List<CapaStatus> CLOSED_CAPA_STATUSES =
            List.of(CapaStatus.CLOSED, CapaStatus.REJECTED);

    /** Traduction RPN → sévérité lisible (bornes usuelles de la pratique FMEA). */
    private static String severityOf(int rpn) {
        if (rpn >= 200) return "CRITICAL";
        if (rpn >= 100) return "HIGH";
        return "MEDIUM";
    }

    private static int severityRank(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 3;
            case "HIGH" -> 2;
            case "MEDIUM" -> 1;
            default -> 0;
        };
    }

    // ---- Alignement normatif --------------------------------------------------------

    /**
     * Score d'alignement par norme adoptée (§8.7). Le calcul détaillé appartient au
     * Standards Hub : on le réutilise plutôt que de le réimplémenter. Une norme dont le
     * calcul échoue est simplement omise — le dashboard ne doit pas tomber pour autant.
     */
    private List<ExecutiveDashboardDto.AlignmentBar> alignment(UUID tenantId) {
        Page<TenantStandard> adoptions = adoptionRepo.findByTenantId(
                tenantId, PageRequest.of(0, MAX_ALIGNMENT_BARS, Sort.by(Sort.Direction.ASC, "createdAt")));

        List<ExecutiveDashboardDto.AlignmentBar> bars = new ArrayList<>(adoptions.getNumberOfElements());
        for (TenantStandard adoption : adoptions) {
            try {
                StandardsDto.AlignmentReport report = standardsService.computeAlignment(adoption.getId());
                List<ExecutiveDashboardDto.SectionScore> sections =
                        new ArrayList<>(report.sections().size());
                for (StandardsDto.SectionAlignment section : report.sections()) {
                    sections.add(new ExecutiveDashboardDto.SectionScore(
                            section.sectionCode(), section.score()));
                }
                bars.add(new ExecutiveDashboardDto.AlignmentBar(
                        adoption.getId(),
                        adoption.getStandard().getCode(),
                        adoption.getStandard().getFullName(),
                        report.overallScore(),
                        adoption.getStatus().name(),
                        sections));
            } catch (RuntimeException e) {
                // Jamais de PII ni de détail exploitable dans les logs (§22 #9).
                log.warn("Alignment computation skipped for adoption {}: {}",
                        adoption.getId(), e.getClass().getSimpleName());
            }
        }
        return bars;
    }

    // ---- Tenant ---------------------------------------------------------------------

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }
}
