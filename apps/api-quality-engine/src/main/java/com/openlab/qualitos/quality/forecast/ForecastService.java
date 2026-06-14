package com.openlab.qualitos.quality.forecast;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service applicatif de prévision KPI (§6.5, §12.1) : relaie une série vers la passerelle IA
 * ({@link AiGatewayClient}) et mappe la réponse JSON brute de {@code ai-service} vers un DTO
 * typé. Aucun calcul ici : la prévision Holt-Winters est faite par {@code ai-service} (NumPy).
 * L'engine relaie et présente (clean architecture), comme pour SPC/anomaly/NLQ.
 *
 * <p>Mapping tolérant (mêmes conventions que {@code SpcService}) : les clés ai-service sont en
 * snake_case ({@code residual_sigma}, {@code seasonal_period}).
 */
@Service
public class ForecastService {

    private final AiGatewayClient ai;

    public ForecastService(AiGatewayClient ai) {
        this.ai = ai;
    }

    public ForecastDto.ForecastResponse forecast(ForecastDto.ForecastRequest request) {
        Map<String, Object> resp = ai.forecastKpi(
                request.values(), request.target(), request.horizon(),
                request.direction(), request.seasonalPeriod());
        return toResponse(resp);
    }

    // ---- mapping réponse ai-service ----

    private ForecastDto.ForecastResponse toResponse(Map<String, Object> resp) {
        return new ForecastDto.ForecastResponse(
                intVal(resp.get("n")),
                dbl(resp.get("slope")),
                dbl(resp.get("intercept")),
                dbl(resp.get("residual_sigma")),
                dbl(resp.get("r2")),
                intVal(resp.get("horizon")),
                dbl(resp.get("target")),
                str(resp.get("direction")),
                dbl(resp.get("probability")),
                str(resp.get("confidence")),
                str(resp.get("model")),
                intVal(resp.get("seasonal_period")),
                points(resp.get("points")));
    }

    private List<ForecastDto.Point> points(Object o) {
        if (!(o instanceof List<?> l)) {
            return List.of();
        }
        return l.stream().map(this::asMap).map(p -> new ForecastDto.Point(
                intVal(p.get("step")),
                dbl(p.get("value")),
                dbl(p.get("low")),
                dbl(p.get("high")))).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private int intVal(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }

    private double dbl(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0.0;
    }
}
