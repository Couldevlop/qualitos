package com.openlab.qualitos.quality.spc;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service applicatif SPC (§3.4, §12.1) : relaie la série vers la passerelle IA
 * ({@link AiGatewayClient}) et mappe la réponse JSON brute de {@code ai-service} vers
 * un DTO typé. Aucune statistique ici : limites de contrôle + 8 règles de Nelson sont
 * calculées par {@code ai-service} (NumPy). L'engine ne fait que relayer et présenter
 * (clean architecture), comme pour NLQ.
 */
@Service
public class SpcService {

    private final AiGatewayClient ai;

    public SpcService(AiGatewayClient ai) {
        this.ai = ai;
    }

    public SpcDto.AnalyzeResponse analyze(SpcDto.AnalyzeRequest request) {
        Map<String, Object> resp = ai.detectSpc(request.values(), request.center(), request.sigma());
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
