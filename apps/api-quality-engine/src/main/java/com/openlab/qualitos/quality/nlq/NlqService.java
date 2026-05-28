package com.openlab.qualitos.quality.nlq;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service applicatif NLQ (§7.3) : relaie la question vers la passerelle IA
 * ({@link AiGatewayClient}) et mappe la réponse JSON brute de {@code ai-service}
 * vers un DTO typé. Aucune logique SQL ici : la génération + validation + exécution
 * read-only (allow-list tables, filtre tenant) sont assurées par {@code ai-service}
 * (clean architecture : l'engine ne fait que relayer et présenter).
 */
@Service
public class NlqService {

    /** Plafond par défaut de lignes si le client n'en précise pas. */
    private static final int DEFAULT_MAX_ROWS = 100;

    private final AiGatewayClient ai;

    public NlqService(AiGatewayClient ai) {
        this.ai = ai;
    }

    public NlqDto.AskResponse ask(NlqDto.AskRequest request) {
        int maxRows = request.maxRows() != null ? request.maxRows() : DEFAULT_MAX_ROWS;
        Map<String, Object> resp = ai.askNlq(request.question(), maxRows);
        Map<String, Object> sqlNode = asMap(resp.get("sql"));
        return new NlqDto.AskResponse(
                request.question(),
                str(sqlNode.get("sql")),
                bool(sqlNode.get("tenant_filter_applied")),
                strList(sqlNode.get("tables_used")),
                strList(sqlNode.get("functions_used")),
                rows(resp.get("rows")),
                intVal(resp.get("row_count")),
                dbl(resp.get("confidence")),
                asMap(resp.get("chart")),
                str(resp.get("narrative")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private List<Map<String, Object>> rows(Object o) {
        return o instanceof List<?> l ? l.stream().map(this::asMap).toList() : List.of();
    }

    private List<String> strList(Object o) {
        return o instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of();
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
