package com.openlab.qualitos.quality.forecast;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de prévision KPI (§6.5, §12.1). POST car l'appel a un effet de bord (relais vers
 * l'inférence d'{@code ai-service} : lissage exponentiel Holt-Winters). Authentifié
 * (SecurityConfig {@code anyRequest().authenticated()}, comme {@code SpcController}) ; le
 * tenant est dérivé du JWT côté passerelle, jamais du corps de requête (règle 18.2 #2).
 */
@RestController
@RequestMapping("/api/v1/ai/forecast")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @PostMapping("/kpi")
    public ForecastDto.ForecastResponse forecastKpi(@Valid @RequestBody ForecastDto.ForecastRequest request) {
        return forecastService.forecast(request);
    }
}
