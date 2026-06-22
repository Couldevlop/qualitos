package com.openlab.qualitos.quality.marketplace.web;

import com.openlab.qualitos.quality.marketplace.application.MarketplacePackDto;
import com.openlab.qualitos.quality.marketplace.application.MarketplacePackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Marketplace de packs normatifs — CLAUDE.md §8.11.
 *
 * <p>RBAC (OWASP A01) :</p>
 * <ul>
 *   <li>LECTURE catalogue/détail public : tout authentifié (filtré PUBLISHED côté service) ;</li>
 *   <li>SOUMISSION : partenaire (rôle PARTNER) / Admin Tenant / Super Admin —
 *       {@code @PreAuthorize} + carve-out URL ({@link com.openlab.qualitos.quality.config.SecurityConfig}) ;</li>
 *   <li>MODÉRATION (queue, prise en revue, publication, rejet, dépréciation) : Super Admin ;</li>
 *   <li>INSTALLATION / DÉSINSTALLATION / NOTATION : Admin Tenant (tenant courant du JWT).</li>
 * </ul>
 *
 * <p>Double rempart : règles d'URL dans SecurityConfig + {@code @PreAuthorize}
 * méthode + ports d'acteur dans le service.</p>
 */
@RestController
@RequestMapping("/api/v1/marketplace/packs")
@Validated
public class MarketplacePackController {

    private final MarketplacePackService service;

    public MarketplacePackController(MarketplacePackService service) {
        this.service = service;
    }

    // ---------------- Catalogue public ----------------

    @GetMapping
    public List<MarketplacePackDto.View> list(@RequestParam(required = false) String sector) {
        return service.listPublished(sector);
    }

    @GetMapping("/{id}")
    public MarketplacePackDto.View get(@PathVariable UUID id) {
        return service.getPublished(id);
    }

    // ---------------- Soumission (partenaire) ----------------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN_TENANT','ADMIN','SUPER_ADMIN')")
    public MarketplacePackDto.View submit(@Valid @RequestBody MarketplacePackWebDto.SubmitRequest req) {
        return service.submit(new MarketplacePackDto.SubmitCommand(
                req.packId(), req.version(), req.publisher(), req.title(),
                req.description(), req.sector(), req.norms(), req.priceCents(),
                req.currency(), req.manifestUrl(), req.manifestJson(), req.signatureHash()));
    }

    // ---------------- Modération (éditeur SUPER_ADMIN) ----------------

    @GetMapping("/moderation/queue")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<MarketplacePackDto.View> moderationQueue() {
        return service.moderationQueue();
    }

    @GetMapping("/{id}/editor")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MarketplacePackDto.View editorDetail(@PathVariable UUID id) {
        return service.getForEditor(id);
    }

    @PostMapping("/{id}/take-review")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MarketplacePackDto.View takeForReview(@PathVariable UUID id) {
        return service.takeForReview(id);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MarketplacePackDto.View publish(@PathVariable UUID id) {
        return service.publish(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MarketplacePackDto.View reject(@PathVariable UUID id,
                                          @Valid @RequestBody MarketplacePackWebDto.RejectRequest req) {
        return service.reject(id, new MarketplacePackDto.RejectCommand(req.reason()));
    }

    @PostMapping("/{id}/deprecate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MarketplacePackDto.View deprecate(@PathVariable UUID id) {
        return service.deprecate(id);
    }

    // ---------------- Installation (tenant — Admin Tenant) ----------------

    @PostMapping("/{id}/install")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_TENANT','ADMIN','SUPER_ADMIN')")
    public MarketplacePackDto.InstallationView install(@PathVariable UUID id) {
        return service.install(id);
    }

    @DeleteMapping("/installations/{installationId}")
    @PreAuthorize("hasAnyRole('ADMIN_TENANT','ADMIN','SUPER_ADMIN')")
    public MarketplacePackDto.InstallationView uninstall(@PathVariable UUID installationId) {
        return service.uninstall(installationId);
    }

    @GetMapping("/installations/my")
    public List<MarketplacePackDto.InstallationView> myInstallations() {
        return service.myInstallations();
    }

    @GetMapping("/installations/my/history")
    public List<MarketplacePackDto.InstallationView> myInstallationHistory() {
        return service.myInstallationHistory();
    }

    // ---------------- Notation (tenant ayant installé) ----------------

    @PostMapping("/{id}/rate")
    @PreAuthorize("hasAnyRole('ADMIN_TENANT','ADMIN','SUPER_ADMIN','QUALITY_MANAGER')")
    public MarketplacePackDto.View rate(@PathVariable UUID id,
                                        @Valid @RequestBody MarketplacePackWebDto.RateRequest req) {
        return service.rate(id, new MarketplacePackDto.RateCommand(req.stars()));
    }
}
