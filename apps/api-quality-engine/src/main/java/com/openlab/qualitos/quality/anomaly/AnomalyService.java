package com.openlab.qualitos.quality.anomaly;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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

    private final AiGatewayClient ai;

    public AnomalyService(AiGatewayClient ai) {
        this.ai = ai;
    }

    public AnomalyDto.DetectResponse detect(AnomalyDto.DetectRequest request) {
        Map<String, Object> resp = ai.detectAnomaly(
                request.samples(), request.method(), request.contamination(), request.threshold());
        return toResponse(resp);
    }

    // ---- mapping réponse ai-service ----

    private AnomalyDto.DetectResponse toResponse(Map<String, Object> resp) {
        return new AnomalyDto.DetectResponse(
                intVal(resp.get("n")),
                intVal(resp.get("n_features")),
                str(resp.get("method")),
                dbl(resp.get("contamination")),
                dbl(resp.get("threshold")),
                intVal(resp.get("anomaly_count")),
                bool(resp.get("has_anomalies")),
                points(resp.get("points")));
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
