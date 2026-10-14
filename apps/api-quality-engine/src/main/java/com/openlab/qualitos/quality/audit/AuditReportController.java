package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.common.StepUpGuard;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Rapport d'un audit (§4.4). Le tenant et l'émetteur viennent du jeton, via le
 * service (§18.2 #2).
 *
 * <p>La génération de la synthèse par IA garde sa route historique
 * ({@code POST /plans/{id}/report/generate}, sur {@link AuditController}) : elle
 * écrit dans le même champ que le brouillon, et la déplacer casserait ses appelants.
 */
@RestController
@RequestMapping("/api/v1/audits/plans/{id}/report")
public class AuditReportController {

    private final AuditReportService service;
    private final StepUpGuard stepUp;

    public AuditReportController(AuditReportService service, StepUpGuard stepUp) {
        this.service = service;
        this.stepUp = stepUp;
    }

    @GetMapping
    public AuditReportDto.View get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping
    public AuditReportDto.View saveDraft(@PathVariable UUID id,
                                         @Valid @RequestBody AuditReportDto.DraftRequest req) {
        return service.saveDraft(id, req);
    }

    /**
     * Émet le rapport. Second facteur exigé, comme pour l'émission d'un rapport
     * 8D : le document devient opposable, et §18.2 #5 ne laisse pas le choix. Le
     * garde est appelé dans le corps de la méthode : on lit ici que l'action
     * l'exige, et un intercepteur mal configuré ne peut pas l'éteindre en silence.
     */
    @PostMapping("/issue")
    public AuditReportDto.View issue(@PathVariable UUID id) {
        stepUp.require("émettre un rapport d'audit");
        return service.issue(id);
    }
}
