package com.openlab.qualitos.quality.spc;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.domain.Permission;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoint de détection d'anomalies SPC (§3.4, §12.1). POST car l'appel a un effet
 * de bord (relais vers l'inférence statistique d'{@code ai-service}) ; le tenant est
 * dérivé du JWT, jamais du corps de requête (règle 18.2 #2).
 *
 * <p>LIRE la carte reste ouvert à tout authentifié : savoir si un procédé est sous
 * contrôle n'est le secret de personne, et c'est l'écran des indicateurs qui
 * appelle ces routes. En revanche {@code openCapa=true} <b>ouvre un dossier
 * CAPA</b> — et ouvrir un dossier demande {@code capa.create} partout ailleurs
 * ({@code CapaController}). Sans ce contrôle, n'importe quel authentifié ouvrait
 * par cette porte les dossiers que la porte d'à côté lui refuse.
 */
@RestController
@RequestMapping("/api/v1/ai/spc")
public class SpcController {

    private final SpcService spcService;
    private final AuthorizationService droits;

    public SpcController(SpcService spcService, AuthorizationService droits) {
        this.spcService = spcService;
        this.droits = droits;
    }

    /**
     * Ouvrir un dossier CAPA par ricochet exige le droit de l'ouvrir en face.
     *
     * <p>Vérifié ici et non par {@code @RequiresPermission} : le droit n'est exigé
     * que SI l'appelant demande l'ouverture. Annoter la méthode entière fermerait
     * la carte de contrôle à qui vient seulement la lire.
     */
    private void exigerLeDroitDOuvrirUneCapa(boolean openCapa) {
        if (openCapa && !droits.has(Permission.CAPA_CREATE)) {
            throw new AccessDeniedException("Access denied");
        }
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
    /**
     * Prévision de sortie des limites sur une série fournie (§6.5).
     *
     * <p>La carte de contrôle dit si le procédé EST hors contrôle ; celle-ci dit
     * quand il VA en sortir — la seule des deux réponses sur laquelle on peut
     * encore agir.
     */
    @PostMapping("/forecast")
    public SpcDto.ForecastResponse forecast(@Valid @RequestBody SpcDto.ForecastRequest request) {
        return spcService.forecast(request);
    }

    /**
     * Prévision de dérive d'un KPI : même série que la carte, projetée. Si
     * {@code openCapa=true} et sortie annoncée, ouvre une CAPA <b>préventive</b>
     * (l'écart n'est pas survenu). Le tenant vient du JWT (jamais du chemin/body).
     */
    @PostMapping("/kpi/{kpiId}/forecast")
    public SpcDto.KpiForecastResponse forecastKpi(
            @PathVariable UUID kpiId,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(required = false) Integer horizon,
            @RequestParam(defaultValue = "false") boolean openCapa) {
        exigerLeDroitDOuvrirUneCapa(openCapa);
        return spcService.forecastKpi(kpiId, limit, horizon, openCapa);
    }

    @PostMapping("/kpi/{kpiId}/analyze")
    public SpcDto.KpiSpcResponse analyzeKpi(
            @PathVariable UUID kpiId,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(defaultValue = "false") boolean openCapa) {
        exigerLeDroitDOuvrirUneCapa(openCapa);
        return spcService.analyzeKpi(kpiId, limit, openCapa);
    }
}
