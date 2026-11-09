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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service applicatif SPC (§3.4, §12.1) : relaie une série vers la passerelle IA
 * ({@link AiGatewayClient}) et mappe la réponse JSON brute de {@code ai-service} vers un
 * DTO typé. Aucune statistique ici : limites de contrôle + 8 règles de Nelson sont
 * calculées par {@code ai-service} (NumPy). L'engine relaie et présente (clean
 * architecture), comme pour NLQ.
 *
 * <p>Mode KPI ({@link #analyzeKpi}) : tire la série des {@code kpi_measurements} via
 * {@link KpiService} et, sur procédé hors-contrôle, peut ouvrir une CAPA (même schéma
 * que la dérive IoT, ADR 0016 : {@code sourceType=SPC_ALERT}, anti-spam par KPI).
 */
@Service
public class SpcService {

    /** SPC sur valeurs individuelles : ≥ 2 points requis (étendue mobile MR̄). */
    private static final int MIN_POINTS = 2;

    /**
     * Prévoir demande plus que décrire : ≥ 4 points.
     *
     * <p>Deux mesures suffisent à dessiner une carte ; elles ne suffisent pas à
     * dater une dérive, parce qu'une tendance ajustée sur trois points ne dit
     * rien de l'avenir. Mieux vaut refuser que donner une date inventée.
     */
    private static final int MIN_FORECAST_POINTS = 4;

    /** Statuts d'une CAPA encore active (anti-spam des CAPA auto-générées). */
    private static final List<CapaStatus> ACTIVE_CAPA_STATUSES =
            List.of(CapaStatus.OPEN, CapaStatus.IN_PROGRESS);

    private final AiGatewayClient ai;
    private final KpiService kpiService;
    private final CapaService capaService;
    private final CapaCaseRepository capaCaseRepo;

    public SpcService(AiGatewayClient ai, KpiService kpiService,
                      CapaService capaService, CapaCaseRepository capaCaseRepo) {
        this.ai = ai;
        this.kpiService = kpiService;
        this.capaService = capaService;
        this.capaCaseRepo = capaCaseRepo;
    }

    public SpcDto.AnalyzeResponse analyze(SpcDto.AnalyzeRequest request) {
        return toResponse(ai.detectSpc(request.values(), request.center(), request.sigma()));
    }

    /**
     * Analyse SPC d'un KPI : série tirée de {@code kpi_measurements} (les {@code limit}
     * dernières, chronologiques). Si {@code openCapa} et procédé hors-contrôle, ouvre une
     * CAPA corrective (anti-spam : une seule active par KPI).
     */
    @Transactional
    public SpcDto.KpiSpcResponse analyzeKpi(UUID kpiId, int limit, boolean openCapa) {
        KpiDto.SpcSeries series = kpiService.spcSeries(kpiId, limit);
        if (series.values().size() < MIN_POINTS) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Au moins " + MIN_POINTS + " mesures sont nécessaires pour une carte de contrôle.");
        }
        SpcDto.AnalyzeResponse analysis = toResponse(ai.detectSpc(series.values(), null, null));

        UUID capaId = (openCapa && analysis.outOfControl())
                ? openCapaForDrift(series, analysis)
                : null;

        List<String> periods = series.periods().stream().map(Instant::toString).toList();
        return new SpcDto.KpiSpcResponse(
                series.kpiId(), series.code(), series.name(), series.unit(),
                periods, series.values(), analysis, capaId);
    }

    // ---- prévision de sortie des limites (§6.5) ----

    public SpcDto.ForecastResponse forecast(SpcDto.ForecastRequest request) {
        return toForecast(ai.forecastSpc(request.values(), request.horizon(),
                request.center(), request.sigma(), request.threshold()));
    }

    /**
     * Prévision SPC d'un KPI : même série que la carte de contrôle, projetée.
     *
     * <p>La carte dit si le procédé EST hors contrôle — et l'opérateur l'apprend
     * quand les pièces sont faites. Ici on dit quand il VA en sortir, ce qui laisse
     * encore le temps d'agir. Si {@code openCapa} et sortie annoncée, un dossier
     * CAPA <b>préventif</b> est ouvert : l'écart n'est pas survenu, et l'appeler
     * correctif laisserait croire qu'il l'est.
     *
     * <p>La référence du dossier est distincte de celle du correctif
     * ({@code :forecast}) : une CAPA préventive ouverte sur une annonce ne doit pas
     * empêcher le correctif quand la dérive, elle, se produit vraiment.
     */
    @Transactional
    public SpcDto.KpiForecastResponse forecastKpi(UUID kpiId, int limit, Integer horizon,
                                                  boolean openCapa) {
        KpiDto.SpcSeries series = kpiService.spcSeries(kpiId, limit);
        if (series.values().size() < MIN_FORECAST_POINTS) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Au moins " + MIN_FORECAST_POINTS
                            + " mesures sont nécessaires pour prévoir une dérive.");
        }
        SpcDto.ForecastResponse forecast =
                toForecast(ai.forecastSpc(series.values(), horizon, null, null, null));

        UUID capaId = (openCapa && forecast.willBreach())
                ? openCapaForPredictedDrift(series, forecast)
                : null;

        List<String> periods = series.periods().stream().map(Instant::toString).toList();
        return new SpcDto.KpiForecastResponse(
                series.kpiId(), series.code(), series.name(), series.unit(),
                periods, series.values(), forecast, capaId);
    }

    /**
     * Ouvre une CAPA PRÉVENTIVE sur dérive annoncée, ou {@code null} si une CAPA
     * active couvre déjà cette annonce.
     *
     * <p>La criticité suit la confiance de la prévision, et non le risque : annoncer
     * « critique » sur une série de cinq points mobiliserait sur un chiffre que le
     * modèle lui-même ne tient pas.
     */
    private UUID openCapaForPredictedDrift(KpiDto.SpcSeries series,
                                           SpcDto.ForecastResponse forecast) {
        UUID tenantId = UUID.fromString(TenantContext.getTenantId());
        String sourceRef = "kpi:" + series.kpiId() + ":forecast";
        if (capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(
                tenantId, CapaSourceType.SPC_ALERT, sourceRef, ACTIVE_CAPA_STATUSES)) {
            return null; // une CAPA active couvre déjà cette annonce
        }
        String cote = "lower".equals(forecast.side()) ? "inférieure" : "supérieure";
        String title = "SPC — dérive annoncée sur le KPI " + series.code();
        String description = "Le procédé suivi par le KPI « " + series.name() + " » ("
                + series.code() + ") devrait franchir sa limite " + cote + " dans "
                + forecast.firstBreachStep() + " période(s), avec un risque de "
                + Math.round(forecast.points().get(forecast.firstBreachStep() - 1).pBreach() * 100)
                + " %. Tendance mesurée : " + arrondi(forecast.trend())
                + " par période ; confiance de la prévision : " + forecast.confidence()
                + " (" + forecast.n() + " mesures, R² " + arrondi(forecast.r2())
                + "). L'écart n'est pas survenu : action préventive à décider, "
                + "cause de la dérive à investiguer.";
        CapaDto.CaseResponse capa = capaService.createCase(new CapaDto.CreateCaseRequest(
                title, description, CapaType.PREVENTIVE, criticityOf(forecast),
                CapaSourceType.SPC_ALERT, sourceRef, series.ownerId(), null, null));
        return capa.id();
    }

    /** Confiance de la prévision → criticité : haute→HIGH, moyenne→MEDIUM, basse→LOW. */
    private CapaCriticity criticityOf(SpcDto.ForecastResponse forecast) {
        return switch (forecast.confidence() == null ? "" : forecast.confidence()) {
            case "high" -> CapaCriticity.HIGH;
            case "medium" -> CapaCriticity.MEDIUM;
            default -> CapaCriticity.LOW;
        };
    }

    /** Deux décimales : un dossier CAPA n'a pas à porter quinze chiffres de flottant. */
    private static String arrondi(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    private SpcDto.ForecastResponse toForecast(Map<String, Object> resp) {
        Map<String, Object> limitsNode = asMap(resp.get("limits"));
        Object first = resp.get("first_breach_step");
        return new SpcDto.ForecastResponse(
                intVal(resp.get("n")),
                intVal(resp.get("horizon")),
                dbl(resp.get("threshold")),
                bool(resp.get("will_breach")),
                first instanceof Number n ? n.intValue() : null,
                resp.get("side") == null ? null : resp.get("side").toString(),
                dbl(resp.get("trend")),
                dbl(resp.get("residual_sigma")),
                dbl(resp.get("r2")),
                str(resp.get("confidence")),
                str(resp.get("model")),
                new SpcDto.Limits(
                        dbl(limitsNode.get("center_line")),
                        dbl(limitsNode.get("sigma")),
                        dbl(limitsNode.get("ucl")),
                        dbl(limitsNode.get("lcl")),
                        bool(limitsNode.get("estimated"))),
                forecastPoints(resp.get("points")));
    }

    private List<SpcDto.ForecastPoint> forecastPoints(Object o) {
        if (!(o instanceof List<?> l)) {
            return List.of();
        }
        return l.stream().map(this::asMap).map(p -> new SpcDto.ForecastPoint(
                intVal(p.get("step")),
                dbl(p.get("value")),
                dbl(p.get("low")),
                dbl(p.get("high")),
                dbl(p.get("p_above_ucl")),
                dbl(p.get("p_below_lcl")),
                dbl(p.get("p_breach")))).toList();
    }

    /** Ouvre une CAPA SPC_ALERT sur dérive, ou {@code null} si une CAPA active existe déjà. */
    private UUID openCapaForDrift(KpiDto.SpcSeries series, SpcDto.AnalyzeResponse analysis) {
        UUID tenantId = UUID.fromString(TenantContext.getTenantId());
        String sourceRef = "kpi:" + series.kpiId();
        if (capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(
                tenantId, CapaSourceType.SPC_ALERT, sourceRef, ACTIVE_CAPA_STATUSES)) {
            return null; // une CAPA active couvre déjà ce KPI
        }
        String rules = analysis.violations().stream().map(SpcDto.Violation::rule).distinct()
                .reduce((a, b) -> a + ", " + b).orElse("—");
        String title = "SPC hors contrôle — KPI " + series.code();
        String description = "Procédé statistiquement hors contrôle détecté sur le KPI « "
                + series.name() + " » (" + series.code() + "). Règles de Nelson déclenchées : "
                + rules + ". " + analysis.violations().size() + " constat(s). Analyse à confirmer, "
                + "cause-racine à investiguer (Ishikawa/5 Pourquoi) puis action corrective.";
        CapaDto.CaseResponse capa = capaService.createCase(new CapaDto.CreateCaseRequest(
                title, description, CapaType.CORRECTIVE, worstCriticity(analysis),
                CapaSourceType.SPC_ALERT, sourceRef, series.ownerId(), null, null));
        return capa.id();
    }

    /** Sévérité Nelson la plus forte → criticité CAPA (high→HIGH, medium→MEDIUM, sinon LOW). */
    private CapaCriticity worstCriticity(SpcDto.AnalyzeResponse analysis) {
        boolean high = false, medium = false;
        for (SpcDto.Violation v : analysis.violations()) {
            String s = v.severity() == null ? "" : v.severity().toLowerCase();
            if (s.equals("high") || s.equals("critical")) high = true;
            else if (s.equals("medium")) medium = true;
        }
        return high ? CapaCriticity.HIGH : medium ? CapaCriticity.MEDIUM : CapaCriticity.LOW;
    }

    // ---- mapping réponse ai-service ----

    private SpcDto.AnalyzeResponse toResponse(Map<String, Object> resp) {
        Map<String, Object> limitsNode = asMap(resp.get("limits"));
        return new SpcDto.AnalyzeResponse(
                intVal(resp.get("n")),
                bool(resp.get("out_of_control")),
                new SpcDto.Limits(
                        dbl(limitsNode.get("center_line")),
                        dbl(limitsNode.get("sigma")),
                        dbl(limitsNode.get("ucl")),
                        dbl(limitsNode.get("lcl")),
                        bool(limitsNode.get("estimated"))),
                violations(resp.get("violations")));
    }

    private List<SpcDto.Violation> violations(Object o) {
        if (!(o instanceof List<?> l)) {
            return List.of();
        }
        return l.stream().map(this::asMap).map(v -> new SpcDto.Violation(
                str(v.get("rule")),
                str(v.get("title")),
                str(v.get("description")),
                intList(v.get("point_indices")),
                str(v.get("severity")))).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private List<Integer> intList(Object o) {
        return o instanceof List<?> l
                ? l.stream().map(x -> x instanceof Number n ? n.intValue() : 0).toList()
                : List.of();
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private boolean bool(Object o) {
        return o instanceof Boolean b && b;
    }

    private int intVal(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }

    private double dbl(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0.0;
    }
}
