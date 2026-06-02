package com.openlab.qualitos.quality.spc;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoint de détection d'anomalies SPC (§3.4, §12.1). POST car l'appel a un effet
 * de bord (relais vers l'inférence statistique d'{@code ai-service}). Authentifié
 * (SecurityConfig {@code anyRequest().authenticated()}) ; le tenant est dérivé du JWT
 * côté passerelle, jamais du corps de requête (règle 18.2 #2).
 */
@RestController
@RequestMapping("/api/v1/ai/spc")
public class SpcController {

    private final SpcService spcService;

    public SpcController(SpcService spcService) {
        this.spcService = spcService;
    }

    @PostMapping("/analyze")
    public SpcDto.AnalyzeResponse analyze(@Valid @RequestBody SpcDto.AnalyzeRequest request) {
        return spcService.analyze(request);
    }

    /**
     * Carte de contrôle SPC d'un KPI : tire les {@code limit} dernières mesures de
     * {@code kpi_measurements} et, si {@code openCapa=true} et procédé hors-contrôle,
     * ouvre une CAPA corrective (ADR 0016, {@code sourceType=SPC_ALERT}). Le tenant
     * vient du JWT (jamais du chemin/body).
     */
    @PostMapping("/kpi/{kpiId}/analyze")
    public SpcDto.KpiSpcResponse analyzeKpi(
            @PathVariable UUID kpiId,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(defaultValue = "false") boolean openCapa) {
        return spcService.analyzeKpi(kpiId, limit, openCapa);
    }
}
