package com.openlab.qualitos.quality.spc;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.config.ModuleNotEnabledException;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
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
 * contrôle n'est le secret de personne. En revanche {@code openCapa=true}
 * <b>ouvre un dossier CAPA</b> — et cela demande {@code capa.create} partout
 * ailleurs ({@code CapaController}). Sans ce contrôle, n'importe quel
 * authentifié ouvrait par cette porte les dossiers que la porte d'à côté lui
 * refuse.
 *
 * <h2>Pourquoi aucun {@code @RequiresModule} sur la classe</h2>
 *
 * <p>Il fermerait la LECTURE, ce que la frontière commerciale interdit (voir
 * {@link com.openlab.qualitos.quality.config.RequiresModule} : « on ferme ce qui
 * fait grossir le dossier, jamais ce qui permet de le relire »). L'intercepteur
 * n'exempte que GET / HEAD / OPTIONS, or <b>les quatre routes sont des POST</b>,
 * et à dessein : l'appel relaie vers l'inférence statistique. Deux d'entre elles
 * — {@code /analyze} et {@code /forecast} sur une série collée — ne lisent ni
 * n'écrivent aucune donnée du client : il n'y a pas de dossier à faire grossir.
 *
 * <p>Le module est donc vérifié là où il y a quelque chose à vendre : sur la
 * branche qui ouvre un dossier, et sur elle seule.
 */
@RestController
@RequestMapping("/api/v1/ai/spc")
public class SpcController {

    /** Le module qui vend l'ouverture automatique d'un dossier sur signal SPC. */
    private static final String MODULE_SPC = "dmaic";

    private final SpcService spcService;
    private final AuthorizationService droits;
    private final ObjectProvider<ModuleActivationService> modules;

    public SpcController(SpcService spcService, AuthorizationService droits,
                         ObjectProvider<ModuleActivationService> modules) {
        this.spcService = spcService;
        this.droits = droits;
        this.modules = modules;
    }

    /**
     * Ouvrir un dossier CAPA par ricochet exige le droit de l'ouvrir en face, et
     * que l'organisation ait souscrit le module qui le propose.
     *
     * <p>Les deux questions sont différentes et se cumulent : le module dit ce que
     * l'ORGANISATION a souscrit, le droit ce que la PERSONNE peut y faire.
     *
     * <p>Vérifié ici et non par {@code @RequiresPermission} / {@code @RequiresModule} :
     * ni l'un ni l'autre n'exprime « seulement si ce drapeau est levé ». Annoter la
     * méthode entière fermerait la carte de contrôle à qui vient seulement la lire.
     *
     * <p>Module absent du contexte — les tranches {@code @WebMvcTest} n'instancient
     * aucun bean de service : on ne refuse pas, exactement comme
     * {@code ModuleEnabledInterceptor}. Une frontière commerciale n'est pas une
     * frontière de sécurité, et celle-là, le droit {@code capa.create} la tient
     * déjà.
     */
    private void exigerLeDroitDOuvrirUneCapa(boolean openCapa) {
        if (!openCapa) {
            return;
        }
        if (!droits.has(Permission.CAPA_CREATE)) {
            throw new AccessDeniedException("Access denied");
        }
        ModuleActivationService service = modules.getIfAvailable();
        if (service != null && !service.enabledModuleCodes().contains(MODULE_SPC)) {
            throw new ModuleNotEnabledException(MODULE_SPC);
        }
    }

    @PostMapping("/analyze")
    public SpcDto.AnalyzeResponse analyze(@Valid @RequestBody SpcDto.AnalyzeRequest request) {
        return spcService.analyze(request);
    }

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
        exigerLeDroitDOuvrirUneCapa(openCapa);
        return spcService.analyzeKpi(kpiId, limit, openCapa);
    }
}
