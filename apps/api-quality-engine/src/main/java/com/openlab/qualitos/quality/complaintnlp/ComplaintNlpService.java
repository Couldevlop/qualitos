package com.openlab.qualitos.quality.complaintnlp;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service applicatif de l'analyse NLP des réclamations (§4.9, §12.1) : relaie le lot vers la
 * passerelle IA ({@link AiGatewayClient}) et mappe la réponse JSON brute de {@code ai-service}
 * vers un DTO typé. Aucun calcul ici : sentiment + classification sont faits par
 * {@code ai-service}. L'engine relaie et présente (clean architecture), comme pour SPC/NLQ.
 *
 * <p>Mapping tolérant (mêmes conventions que {@code SpcService}) : clés ai-service en snake_case
 * ({@code sentiment_label}, {@code critical_count}).
 */
@Service
public class ComplaintNlpService {

    private final AiGatewayClient ai;

    public ComplaintNlpService(AiGatewayClient ai) {
        this.ai = ai;
    }

    public ComplaintNlpDto.AnalyzeResponse analyze(ComplaintNlpDto.AnalyzeRequest request) {
        Map<String, Object> resp = ai.analyzeComplaints(request.texts(), request.categories());
        return toResponse(resp);
    }

    // ---- mapping réponse ai-service ----

    private ComplaintNlpDto.AnalyzeResponse toResponse(Map<String, Object> resp) {
        return new ComplaintNlpDto.AnalyzeResponse(
                intVal(resp.get("n")),
                intVal(resp.get("critical_count")),
                insights(resp.get("insights")));
    }

    private List<ComplaintNlpDto.Insight> insights(Object o) {
        if (!(o instanceof List<?> l)) {
            return List.of();
        }
        return l.stream().map(this::asMap).map(i -> new ComplaintNlpDto.Insight(
                intVal(i.get("index")),
                dbl(i.get("sentiment")),
                str(i.get("sentiment_label")),
                str(i.get("category")),
                bool(i.get("critical")))).toList();
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
