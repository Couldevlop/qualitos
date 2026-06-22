package com.openlab.qualitos.quality.marketplace.application;

import com.openlab.qualitos.quality.marketplace.domain.InstallationStatus;
import com.openlab.qualitos.quality.marketplace.domain.MarketplaceInstallation;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePack;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackStatus;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * DTOs de la couche application — commandes (entrée) et vues (sortie) du marketplace.
 */
public final class MarketplacePackDto {
    private MarketplacePackDto() {}

    // ---------------- Commandes (entrée) ----------------

    /** Soumission par un partenaire. L'acteur (submittedBy) est dérivé du JWT, pas du body. */
    public record SubmitCommand(
            String packId,
            String version,
            String publisher,
            String title,
            String description,
            String sector,
            List<String> norms,
            int priceCents,
            String currency,
            String manifestUrl,
            String manifestJson,
            String signatureHash
    ) {}

    /** Rejet motivé par l'éditeur. */
    public record RejectCommand(String reason) {}

    /** Notation d'un pack publié (1..5) par un tenant ayant installé. */
    public record RateCommand(int stars) {}

    // ---------------- Vues (sortie) ----------------

    /**
     * Vue d'un pack. Pour le catalogue public, les champs de modération
     * (signatureHash, reviewNotes, submittedBy) NE sont PAS exposés — voir
     * {@link #publicView(MarketplacePack)}. La vue éditeur les inclut.
     */
    public record View(
            UUID id, String packId, String version,
            String publisher, String title, String description,
            String sector, List<String> norms, int priceCents, String currency,
            MarketplacePackStatus status,
            UUID submittedBy, Instant submittedAt,
            UUID reviewedBy, Instant reviewedAt, String reviewNotes,
            String signatureHash, String manifestUrl,
            double ratingAvg, int ratingCount,
            Instant createdAt, Instant updatedAt) {

        /** Vue ÉDITEUR (SUPER_ADMIN) : tous les champs de modération. */
        public static View editorView(MarketplacePack p) {
            return new View(p.getId(), p.getPackId(), p.getVersion(),
                    p.getPublisher(), p.getTitle(), p.getDescription(),
                    p.getSector(), norms(p.getNormsCsv()), p.getPriceCents(), p.getCurrency(),
                    p.getStatus(), p.getSubmittedBy(), p.getSubmittedAt(),
                    p.getReviewedBy(), p.getReviewedAt(), p.getReviewNotes(),
                    p.getSignatureHash(), p.getManifestUrl(),
                    p.getRatingAvg(), p.getRatingCount(),
                    p.getCreatedAt(), p.getUpdatedAt());
        }

        /** Vue PUBLIQUE (catalogue) : sans secrets de modération. */
        public static View publicView(MarketplacePack p) {
            return new View(p.getId(), p.getPackId(), p.getVersion(),
                    p.getPublisher(), p.getTitle(), p.getDescription(),
                    p.getSector(), norms(p.getNormsCsv()), p.getPriceCents(), p.getCurrency(),
                    p.getStatus(), null, null,
                    null, null, null,
                    null, p.getManifestUrl(),
                    p.getRatingAvg(), p.getRatingCount(),
                    p.getCreatedAt(), p.getUpdatedAt());
        }

        private static List<String> norms(String csv) {
            if (csv == null || csv.isBlank()) {
                return List.of();
            }
            return Arrays.stream(csv.split(",")).map(String::trim)
                    .filter(s -> !s.isEmpty()).toList();
        }
    }

    /** Vue d'une installation par tenant. */
    public record InstallationView(
            UUID id, UUID tenantId, UUID marketplacePackId,
            String packId, String packVersion,
            InstallationStatus status,
            UUID installedBy, Instant installedAt,
            UUID uninstalledBy, Instant uninstalledAt) {

        public static InstallationView of(MarketplaceInstallation i) {
            return new InstallationView(i.getId(), i.getTenantId(), i.getMarketplacePackId(),
                    i.getPackId(), i.getPackVersion(), i.getStatus(),
                    i.getInstalledBy(), i.getInstalledAt(),
                    i.getUninstalledBy(), i.getUninstalledAt());
        }
    }
}
