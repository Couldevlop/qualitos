package com.openlab.qualitos.quality.anomaly;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de détection d'anomalies non-supervisée multivariée (§3.4, §12.1). POST car
 * l'appel a un effet de bord (relais vers l'inférence d'{@code ai-service} : Isolation
 * Forest ou reconstruction par ACP). Authentifié (SecurityConfig
 * {@code anyRequest().authenticated()}, comme {@code SpcController}) ; le tenant est dérivé
 * du JWT côté passerelle, jamais du corps de requête (règle 18.2 #2).
 */
@RestController
@RequestMapping("/api/v1/ai/anomaly")
public class AnomalyController {

    private final AnomalyService anomalyService;

    public AnomalyController(AnomalyService anomalyService) {
        this.anomalyService = anomalyService;
    }

    @PostMapping("/detect")
    public AnomalyDto.DetectResponse detect(@Valid @RequestBody AnomalyDto.DetectRequest request) {
        return anomalyService.detect(request);
    }

    @PostMapping("/explain")
    public AnomalyDto.ExplainResponse explain(@Valid @RequestBody AnomalyDto.ExplainRequest request) {
        return anomalyService.explain(request);
    }
}
