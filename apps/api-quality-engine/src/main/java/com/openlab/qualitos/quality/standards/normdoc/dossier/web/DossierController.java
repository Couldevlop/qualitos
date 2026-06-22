package com.openlab.qualitos.quality.standards.normdoc.dossier.web;

import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierDto;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints — génération documentaire IA AVANCÉE multi-documents (Standards Hub
 * §8.8) : démarrage d'un dossier complet (Manuel + Politique + procédures),
 * suivi de progression, relance, finalisation (signature + ancrage).
 *
 * <p>Sécurité (§18.2 #2/#5) : tenant + acteur issus du JWT (jamais du body). La
 * génération en lot et la finalisation sont réservées au pilotage qualité
 * (Manager Qualité+) ; la finalisation pose une signature humaine globale.
 * Chaque pièce conserve son propre cycle d'approbation (NormDocController) : la
 * finalisation exige que toutes les pièces soient APPROUVÉES.
 */
@RestController
@RequestMapping("/api/v1/standards/doc-dossiers")
@Validated
public class DossierController {

    private final DossierService service;

    public DossierController(DossierService service) {
        this.service = service;
    }

    /** Catalogue des pièces générables (pour l'UI de sélection). */
    @GetMapping("/catalog")
    @PreAuthorize("isAuthenticated()")
    public List<DossierDto.DocumentView> catalog() {
        return service.catalog();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<DossierDto.View> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public DossierDto.View get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','ADMIN','ADMIN_TENANT','SUPER_ADMIN')")
    public DossierDto.View start(@Valid @RequestBody DossierWebDto.StartBody body) {
        DossierWebDto.TenantProfileBody p = body.tenantProfile();
        DossierDto.TenantProfile profile = new DossierDto.TenantProfile(
                p.organizationName(), p.industry(), p.size(), p.language(),
                p.knownProcesses() == null ? List.of() : p.knownProcesses());
        return service.start(new DossierDto.StartRequest(
                body.standardId(), profile,
                body.documentKeys() == null ? List.of() : body.documentKeys()));
    }

    /** Relance la génération des pièces en attente / en échec (résilience IA). */
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','ADMIN','ADMIN_TENANT','SUPER_ADMIN')")
    public DossierDto.View retry(@PathVariable UUID id) {
        return service.retryFailed(id);
    }

    /** Finalise : exige toutes les pièces approuvées, scelle et ancre. */
    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasAnyRole('DIRECTOR_QUALITY','ADMIN','ADMIN_TENANT','SUPER_ADMIN')")
    public DossierDto.View finalizeDossier(@PathVariable UUID id,
                                          @Valid @RequestBody DossierWebDto.FinalizeBody body) {
        return service.finalizeDossier(id,
                new DossierDto.FinalizeRequest(body.signature(), body.notes()));
    }
}
