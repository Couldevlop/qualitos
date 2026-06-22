package com.openlab.qualitos.quality.marketplace.application;

import com.openlab.qualitos.quality.marketplace.domain.ManifestScanResult;
import com.openlab.qualitos.quality.marketplace.domain.MarketplaceInstallation;
import com.openlab.qualitos.quality.marketplace.domain.MarketplaceInstallationNotFoundException;
import com.openlab.qualitos.quality.marketplace.domain.MarketplaceInstallationRepository;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePack;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackNotFoundException;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackRepository;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackStateException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use cases — marketplace de packs normatifs (CLAUDE.md §8.11).
 *
 * <p>Habilitations (appliquées via les ports d'acteur + RBAC d'URL côté SecurityConfig) :</p>
 * <ul>
 *   <li>SOUMISSION : partenaire authentifié ({@link CurrentActorProvider}) ;</li>
 *   <li>PRISE EN REVUE / PUBLICATION / REJET / DÉPRÉCIATION : éditeur SUPER_ADMIN
 *       ({@link SuperAdminProvider}) — aucune publication sans validation humaine ;</li>
 *   <li>INSTALLATION / DÉSINSTALLATION / NOTATION : tenant ({@link TenantProvider} +
 *       {@link CurrentActorProvider}) ;</li>
 *   <li>LECTURE catalogue public : tout authentifié (uniquement les packs PUBLISHED).</li>
 * </ul>
 */
public class MarketplacePackService {

    private final MarketplacePackRepository packRepo;
    private final MarketplaceInstallationRepository installRepo;
    private final SuperAdminProvider superAdmin;
    private final CurrentActorProvider actor;
    private final TenantProvider tenant;
    private final ManifestScanner manifestScanner;
    private final Clock clock;

    public MarketplacePackService(MarketplacePackRepository packRepo,
                                  MarketplaceInstallationRepository installRepo,
                                  SuperAdminProvider superAdmin,
                                  CurrentActorProvider actor,
                                  TenantProvider tenant,
                                  ManifestScanner manifestScanner,
                                  Clock clock) {
        this.packRepo = packRepo;
        this.installRepo = installRepo;
        this.superAdmin = superAdmin;
        this.actor = actor;
        this.tenant = tenant;
        this.manifestScanner = manifestScanner;
        this.clock = clock;
    }

    // ---------------- Catalogue public ----------------

    /** Catalogue public : packs PUBLISHED uniquement, vue sans secrets de modération. */
    public List<MarketplacePackDto.View> listPublished(String sectorFilter) {
        return packRepo.findPublished(blankToNull(sectorFilter)).stream()
                .map(MarketplacePackDto.View::publicView)
                .toList();
    }

    /** Détail d'un pack PUBLISHED (catalogue public). 404 sinon (pas de fuite des brouillons). */
    public MarketplacePackDto.View getPublished(UUID id) {
        MarketplacePack p = packRepo.findById(id)
                .orElseThrow(() -> new MarketplacePackNotFoundException(id));
        if (!p.getStatus().isPubliclyVisible()) {
            throw new MarketplacePackNotFoundException(id);
        }
        return MarketplacePackDto.View.publicView(p);
    }

    // ---------------- Soumission (partenaire) ----------------

    public MarketplacePackDto.View submit(MarketplacePackDto.SubmitCommand cmd) {
        UUID submitter = actor.requireActorId();
        if (packRepo.existsByPackIdAndVersion(cmd.packId(), cmd.version())) {
            throw new MarketplacePackStateException(
                    "pack already submitted: " + cmd.packId() + " v" + cmd.version());
        }
        // Scan basique du manifeste AVANT toute persistance (refus si anomalie bloquante).
        ManifestScanResult scan = manifestScanner.scan(cmd.manifestJson());
        if (!scan.ok()) {
            throw new MarketplacePackStateException(
                    "manifest scan failed: " + String.join("; ", scan.errors()));
        }
        Instant now = Instant.now(clock);
        MarketplacePack p = MarketplacePack.submit(
                cmd.packId(), cmd.version(), cmd.publisher(), cmd.title(),
                cmd.description(), cmd.sector(), normsCsv(cmd.norms()),
                cmd.priceCents(), cmd.currency(),
                cmd.manifestUrl(), cmd.manifestJson(), cmd.signatureHash(),
                submitter, now);
        return MarketplacePackDto.View.editorView(packRepo.save(p));
    }

    // ---------------- Modération (éditeur SUPER_ADMIN) ----------------

    public List<MarketplacePackDto.View> moderationQueue() {
        superAdmin.requireSuperAdminId();
        return packRepo.findModerationQueue().stream()
                .map(MarketplacePackDto.View::editorView)
                .toList();
    }

    /** Détail éditeur (tous champs) — réservé SUPER_ADMIN. */
    public MarketplacePackDto.View getForEditor(UUID id) {
        superAdmin.requireSuperAdminId();
        MarketplacePack p = packRepo.findById(id)
                .orElseThrow(() -> new MarketplacePackNotFoundException(id));
        return MarketplacePackDto.View.editorView(p);
    }

    public MarketplacePackDto.View takeForReview(UUID id) {
        UUID reviewer = superAdmin.requireSuperAdminId();
        MarketplacePack p = packRepo.findById(id)
                .orElseThrow(() -> new MarketplacePackNotFoundException(id));
        p.takeForReview(reviewer, Instant.now(clock));
        return MarketplacePackDto.View.editorView(packRepo.save(p));
    }

    public MarketplacePackDto.View publish(UUID id) {
        UUID reviewer = superAdmin.requireSuperAdminId();
        MarketplacePack p = packRepo.findById(id)
                .orElseThrow(() -> new MarketplacePackNotFoundException(id));
        p.publish(reviewer, Instant.now(clock));
        return MarketplacePackDto.View.editorView(packRepo.save(p));
    }

    public MarketplacePackDto.View reject(UUID id, MarketplacePackDto.RejectCommand cmd) {
        UUID reviewer = superAdmin.requireSuperAdminId();
        MarketplacePack p = packRepo.findById(id)
                .orElseThrow(() -> new MarketplacePackNotFoundException(id));
        p.reject(reviewer, cmd == null ? null : cmd.reason(), Instant.now(clock));
        return MarketplacePackDto.View.editorView(packRepo.save(p));
    }

    public MarketplacePackDto.View deprecate(UUID id) {
        UUID reviewer = superAdmin.requireSuperAdminId();
        MarketplacePack p = packRepo.findById(id)
                .orElseThrow(() -> new MarketplacePackNotFoundException(id));
        p.deprecate(reviewer, Instant.now(clock));
        return MarketplacePackDto.View.editorView(packRepo.save(p));
    }

    // ---------------- Installation (tenant) ----------------

    public MarketplacePackDto.InstallationView install(UUID packId) {
        UUID tenantId = tenant.requireTenantId();
        UUID installer = actor.requireActorId();
        MarketplacePack pack = packRepo.findById(packId)
                .orElseThrow(() -> new MarketplacePackNotFoundException(packId));
        // Un pack non publié n'est pas installable (et reste invisible du tenant).
        if (!pack.getStatus().isPubliclyVisible()) {
            throw new MarketplacePackNotFoundException(packId);
        }
        Optional<MarketplaceInstallation> existing = installRepo.findActive(tenantId, packId);
        if (existing.isPresent()) {
            // Idempotent : déjà installé pour ce tenant.
            return MarketplacePackDto.InstallationView.of(existing.get());
        }
        MarketplaceInstallation inst =
                MarketplaceInstallation.install(tenantId, pack, installer, Instant.now(clock));
        return MarketplacePackDto.InstallationView.of(installRepo.save(inst));
    }

    public MarketplacePackDto.InstallationView uninstall(UUID installationId) {
        UUID tenantId = tenant.requireTenantId();
        UUID actorId = actor.requireActorId();
        MarketplaceInstallation inst = installRepo.findByIdForTenant(tenantId, installationId)
                .orElseThrow(() -> new MarketplaceInstallationNotFoundException(installationId));
        inst.uninstall(actorId, Instant.now(clock));
        return MarketplacePackDto.InstallationView.of(installRepo.save(inst));
    }

    public List<MarketplacePackDto.InstallationView> myInstallations() {
        UUID tenantId = tenant.requireTenantId();
        return installRepo.findActiveByTenant(tenantId).stream()
                .map(MarketplacePackDto.InstallationView::of)
                .toList();
    }

    public List<MarketplacePackDto.InstallationView> myInstallationHistory() {
        UUID tenantId = tenant.requireTenantId();
        return installRepo.findAllByTenant(tenantId).stream()
                .map(MarketplacePackDto.InstallationView::of)
                .toList();
    }

    // ---------------- Notation (tenant ayant installé) ----------------

    public MarketplacePackDto.View rate(UUID packId, MarketplacePackDto.RateCommand cmd) {
        UUID tenantId = tenant.requireTenantId();
        actor.requireActorId();
        MarketplacePack pack = packRepo.findById(packId)
                .orElseThrow(() -> new MarketplacePackNotFoundException(packId));
        if (!pack.getStatus().isPubliclyVisible()) {
            throw new MarketplacePackNotFoundException(packId);
        }
        // Seul un tenant ayant une installation ACTIVE peut noter (anti-vote fantôme).
        if (installRepo.findActive(tenantId, packId).isEmpty()) {
            throw new MarketplacePackStateException(
                    "only a tenant that installed the pack can rate it");
        }
        pack.addRating(cmd == null ? 0 : cmd.stars(), Instant.now(clock));
        return MarketplacePackDto.View.publicView(packRepo.save(pack));
    }

    // ---------------- helpers ----------------

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String normsCsv(List<String> norms) {
        if (norms == null || norms.isEmpty()) {
            return null;
        }
        String csv = norms.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
        return (csv == null || csv.isBlank()) ? null : csv;
    }
}
