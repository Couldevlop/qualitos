package com.openlab.qualitos.quality.anomaly;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service applicatif de détection d'anomalies multivariées (§3.4, §12.1) : relaie la
 * matrice vers la passerelle IA ({@link AiGatewayClient}) et mappe la réponse JSON brute
 * de {@code ai-service} vers un DTO typé. Aucun calcul ici : Isolation Forest et
 * reconstruction par ACP sont exécutés par {@code ai-service} (NumPy). L'engine relaie et
 * présente (clean architecture), comme pour le SPC et le NLQ.
 *
 * <p>Le mapping est <b>tolérant</b> (champs manquants/mal typés → valeurs neutres) pour ne
 * pas casser sur une évolution mineure du contrat de la passerelle.
 */
@Service
public class AnomalyService {

    /** Statuts qui font qu'une CAPA « couvre déjà » le sujet (anti-doublon). */
    private static final List<CapaStatus> ACTIVE_CAPA_STATUSES =
            List.of(CapaStatus.OPEN, CapaStatus.IN_PROGRESS);

    /** Au-delà, ce n'est plus un point aberrant mais un procédé qui a dérivé. */
    private static final double HIGH_RATIO = 0.10;
    private static final double MEDIUM_RATIO = 0.02;

    private final AiGatewayClient ai;
    private final CapaService capaService;
    private final CapaCaseRepository capaCaseRepo;

    public AnomalyService(AiGatewayClient ai, CapaService capaService, CapaCaseRepository capaCaseRepo) {
        this.ai = ai;
        this.capaService = capaService;
        this.capaCaseRepo = capaCaseRepo;
    }

    public AnomalyDto.DetectResponse detect(AnomalyDto.DetectRequest request) {
        Map<String, Object> resp = ai.detectAnomaly(
                request.samples(), request.method(), request.contamination(), request.threshold());
        AnomalyDto.DetectResponse detection = toResponse(resp);

        UUID capaId = Boolean.TRUE.equals(request.openCapa()) && detection.hasAnomalies()
                ? openCapa(detection, request.subject())
                : null;
        return withCapa(detection, capaId);
    }

    /**
     * Referme la boucle détection → action corrective (ADR 0022). Jusqu'ici elle
     * s'arrêtait à l'écran : on savait qu'un point était anormal, et rien ne
     * s'ensuivait.
     *
     * @return la CAPA ouverte, ou {@code null} si le sujet manque ou si un
     *         dossier actif couvre déjà ce même sujet.
     */
    private UUID openCapa(AnomalyDto.DetectResponse detection, String rawSubject) {
        String subject = sanitizeSubject(rawSubject);
        if (subject == null) {
            // Une action corrective qui ne dit pas sur quoi elle porte n'est pas
            // exploitable. On préfère ne rien ouvrir plutôt qu'ouvrir un dossier
            // que personne ne saura rattacher.
            return null;
        }
        UUID tenantId = UUID.fromString(TenantContext.getTenantId());
        String sourceRef = "anomaly:" + subject;
        if (capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(
                tenantId, CapaSourceType.ANOMALY, sourceRef, ACTIVE_CAPA_STATUSES)) {
            // Un dossier actif couvre déjà ce sujet : en ouvrir un second à
            // chaque analyse noierait le vrai dans une pile de doublons.
            return null;
        }

        String title = "Anomalie détectée — " + subject;
        String description = "Détection non supervisée (" + detection.method() + ") : "
                + detection.anomalyCount() + " point(s) anormal(aux) sur " + detection.n()
                + " observations, seuil " + detection.threshold() + ". Sujet : " + subject
                + ". Le modèle signale un comportement inhabituel, il ne nomme pas de cause : "
                + "confirmer le constat, puis remonter à la cause racine (Ishikawa / 5 Pourquoi) "
                + "avant de décider de l'action.";

        CapaDto.CaseResponse capa = capaService.createCase(new CapaDto.CreateCaseRequest(
                title, description, CapaType.CORRECTIVE, criticity(detection),
                CapaSourceType.ANOMALY, sourceRef, null, null, null));
        return capa.id();
    }

    /**
     * Criticité tirée de la PART d'observations anormales, et non de leur nombre :
     * dix points sur dix mille est un aléa, dix sur cinquante est une dérive. Le
     * score brut ne s'y prêterait pas — il n'a pas la même échelle d'une méthode
     * à l'autre.
     */
    private CapaCriticity criticity(AnomalyDto.DetectResponse d) {
        if (d.n() <= 0) {
            return CapaCriticity.LOW;
        }
        double ratio = (double) d.anomalyCount() / d.n();
        if (ratio >= HIGH_RATIO) {
            return CapaCriticity.HIGH;
        }
        return ratio >= MEDIUM_RATIO ? CapaCriticity.MEDIUM : CapaCriticity.LOW;
    }

    /**
     * Le sujet finit dans le titre d'un dossier et dans sa référence de source.
     * On le réduit à un jeu de caractères lisible et on écarte les caractères de
     * contrôle : un titre n'a pas à porter de saut de ligne, et une référence
     * doit rester comparable à l'identique d'une analyse à l'autre.
     */
    private static String sanitizeSubject(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replaceAll("[\\p{Cntrl}]", " ").trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned;
    }

    private static AnomalyDto.DetectResponse withCapa(AnomalyDto.DetectResponse d, UUID capaId) {
        return new AnomalyDto.DetectResponse(d.n(), d.nFeatures(), d.method(), d.contamination(),
                d.threshold(), d.anomalyCount(), d.hasAnomalies(), d.points(), capaId);
    }

    public AnomalyDto.ExplainResponse explain(AnomalyDto.ExplainRequest request) {
        Map<String, Object> resp = ai.explainAnomaly(request.samples(), request.index());
        return toExplain(resp);
    }

    // ---- mapping réponse ai-service ----

    private AnomalyDto.ExplainResponse toExplain(Map<String, Object> resp) {
        return new AnomalyDto.ExplainResponse(
                intVal(resp.get("index")),
                str(resp.get("method")),
                dbl(resp.get("score")),
                dbl(resp.get("base_value")),
                contributions(resp.get("contributions")));
    }

    private List<AnomalyDto.Contribution> contributions(Object o) {
        if (!(o instanceof List<?> l)) {
            return List.of();
        }
        return l.stream().map(this::asMap).map(c -> new AnomalyDto.Contribution(
                intVal(c.get("feature")),
                dbl(c.get("value")),
                dbl(c.get("contribution")))).toList();
    }

    private AnomalyDto.DetectResponse toResponse(Map<String, Object> resp) {
        return new AnomalyDto.DetectResponse(
                intVal(resp.get("n")),
                intVal(resp.get("n_features")),
                str(resp.get("method")),
                dbl(resp.get("contamination")),
                dbl(resp.get("threshold")),
                intVal(resp.get("anomaly_count")),
                bool(resp.get("has_anomalies")),
                points(resp.get("points")),
                // La CAPA est décidée après le mapping : ici on ne fait que
                // traduire la réponse de la passerelle.
                null);
    }

    private List<AnomalyDto.Point> points(Object o) {
        if (!(o instanceof List<?> l)) {
            return List.of();
        }
        return l.stream().map(this::asMap).map(p -> new AnomalyDto.Point(
                intVal(p.get("index")),
                dbl(p.get("score")),
                bool(p.get("is_anomaly")),
                topFeature(p.get("top_feature")))).toList();
    }

    /** {@code top_feature} : Integer si fourni (non négatif), sinon null. */
    private Integer topFeature(Object o) {
        return o instanceof Number n ? n.intValue() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
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
