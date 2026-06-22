package com.openlab.qualitos.quality.marketplace.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Aggregate — un pack normatif / sectoriel proposé par un partenaire éditeur
 * (CLAUDE.md §8.11). CROSS-tenant : un pack est soumis une fois, publié après
 * validation de l'éditeur, puis consommé (installé) par de nombreux tenants.
 *
 * <p>Cycle de vie porté par {@link MarketplacePackStatus} :
 * SUBMITTED → IN_REVIEW → PUBLISHED / REJECTED → DEPRECATED. Toutes les
 * transitions invalides lèvent {@link MarketplacePackStateException} (→ 409).</p>
 *
 * <p>Invariant de validation humaine : un pack ne peut atteindre PUBLISHED que
 * via {@link #publish(UUID, Instant)} appelé après {@link #takeForReview(UUID, Instant)} ;
 * l'éditeur (SUPER_ADMIN) est l'acteur, jamais le partenaire soumissionnaire.</p>
 */
public final class MarketplacePack {

    private static final Pattern PACK_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]{1,63}$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+(\\.\\d+)?$");
    private static final Set<String> CURRENCIES = Set.of("EUR", "USD", "GBP", "CHF", "JPY");

    private UUID id;
    private final String packId;
    private final String version;
    private final String publisher;
    private final String title;
    private String description;
    private final String sector;
    /** Normes couvertes, CSV de slugs (ex : "iso-9001,iso-13485"). */
    private final String normsCsv;
    private final int priceCents;
    private final String currency;

    private MarketplacePackStatus status;
    private final UUID submittedBy;
    private final Instant submittedAt;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private String reviewNotes;

    private final String signatureHash;
    private final String manifestUrl;
    /** Manifeste inline (JSON) scanné à la soumission. Opaque côté DB (TEXT). */
    private final String manifestJson;

    /** Note moyenne (0..5, deux décimales) et nombre de votes — réputation catalogue. */
    private double ratingAvg;
    private int ratingCount;

    private final Instant createdAt;
    private Instant updatedAt;

    public MarketplacePack(UUID id, String packId, String version,
                           String publisher, String title, String description,
                           String sector, String normsCsv, int priceCents, String currency,
                           MarketplacePackStatus status, UUID submittedBy, Instant submittedAt,
                           UUID reviewedBy, Instant reviewedAt, String reviewNotes,
                           String signatureHash, String manifestUrl, String manifestJson,
                           double ratingAvg, int ratingCount,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.packId = requirePackId(packId);
        this.version = requireVersion(version);
        this.publisher = requireText(publisher, "publisher", 120);
        this.title = requireText(title, "title", 250);
        this.description = description;
        this.sector = requireText(sector, "sector", 80);
        this.normsCsv = normsCsv;
        if (priceCents < 0) {
            throw new IllegalArgumentException("priceCents must be >= 0");
        }
        this.priceCents = priceCents;
        this.currency = requireCurrency(currency);
        this.status = Objects.requireNonNull(status, "status");
        this.submittedBy = Objects.requireNonNull(submittedBy, "submittedBy");
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt");
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewNotes = reviewNotes;
        this.signatureHash = signatureHash;
        this.manifestUrl = requireManifestUrl(manifestUrl);
        this.manifestJson = manifestJson;
        this.ratingAvg = clampRating(ratingAvg);
        this.ratingCount = Math.max(0, ratingCount);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    /**
     * Soumission par un partenaire. Le pack démarre en {@link MarketplacePackStatus#SUBMITTED}.
     * La signature publisher est exigée dès la soumission (preuve d'intégrité du contenu).
     */
    public static MarketplacePack submit(String packId, String version,
                                         String publisher, String title,
                                         String description, String sector, String normsCsv,
                                         int priceCents, String currency,
                                         String manifestUrl, String manifestJson,
                                         String signatureHash, UUID submittedBy, Instant now) {
        if (signatureHash == null || signatureHash.length() < 16) {
            throw new MarketplacePackStateException(
                    "signatureHash (>= 16 chars) required at submission");
        }
        return new MarketplacePack(null, packId, version, publisher, title,
                description, sector, normsCsv, priceCents, currency,
                MarketplacePackStatus.SUBMITTED, submittedBy, now,
                null, null, null,
                signatureHash, manifestUrl, manifestJson,
                0.0, 0, now, now);
    }

    /** L'éditeur prend le pack en revue. SUBMITTED → IN_REVIEW uniquement. */
    public void takeForReview(UUID reviewerId, Instant now) {
        Objects.requireNonNull(reviewerId, "reviewerId");
        requireStatus(MarketplacePackStatus.SUBMITTED, "take for review");
        this.status = MarketplacePackStatus.IN_REVIEW;
        this.reviewedBy = reviewerId;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    /** Validation humaine + publication. IN_REVIEW → PUBLISHED uniquement. */
    public void publish(UUID reviewerId, Instant now) {
        Objects.requireNonNull(reviewerId, "reviewerId");
        requireStatus(MarketplacePackStatus.IN_REVIEW, "publish");
        if (signatureHash == null || signatureHash.length() < 16) {
            throw new MarketplacePackStateException("signatureHash required before publication");
        }
        this.status = MarketplacePackStatus.PUBLISHED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    /** Rejet motivé. Autorisé depuis SUBMITTED ou IN_REVIEW. État terminal. */
    public void reject(UUID reviewerId, String reason, Instant now) {
        Objects.requireNonNull(reviewerId, "reviewerId");
        if (status != MarketplacePackStatus.SUBMITTED && status != MarketplacePackStatus.IN_REVIEW) {
            throw new MarketplacePackStateException(
                    "cannot reject a pack in status " + status);
        }
        if (reason == null || reason.isBlank()) {
            throw new MarketplacePackStateException("a rejection reason is required");
        }
        this.status = MarketplacePackStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = now;
        this.reviewNotes = reason.length() > 2000 ? reason.substring(0, 2000) : reason;
        this.updatedAt = now;
    }

    /** Retrait du catalogue après publication. PUBLISHED → DEPRECATED uniquement. */
    public void deprecate(UUID reviewerId, Instant now) {
        Objects.requireNonNull(reviewerId, "reviewerId");
        requireStatus(MarketplacePackStatus.PUBLISHED, "deprecate");
        this.status = MarketplacePackStatus.DEPRECATED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    /**
     * Ajoute une note (1..5) et recalcule la moyenne incrémentale. Autorisé
     * uniquement pour un pack publié (réputation du catalogue public).
     * L'anti-double-vote est géré au niveau use-case.
     */
    public void addRating(int stars, Instant now) {
        if (status != MarketplacePackStatus.PUBLISHED) {
            throw new MarketplacePackStateException("only a PUBLISHED pack can be rated");
        }
        if (stars < 1 || stars > 5) {
            throw new MarketplacePackStateException("rating must be between 1 and 5");
        }
        double total = this.ratingAvg * this.ratingCount + stars;
        this.ratingCount += 1;
        this.ratingAvg = clampRating(Math.round((total / this.ratingCount) * 100.0) / 100.0);
        this.updatedAt = now;
    }

    public void assignId(UUID id) { this.id = id; }

    private void requireStatus(MarketplacePackStatus expected, String op) {
        if (this.status != expected) {
            throw new MarketplacePackStateException(
                    "cannot " + op + " a pack in status " + status + " (expected " + expected + ")");
        }
    }

    private static double clampRating(double r) {
        if (r < 0) return 0.0;
        return Math.min(r, 5.0);
    }

    private static String requirePackId(String v) {
        if (v == null || !PACK_ID_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException("packId must match [a-z][a-z0-9_-]{1,63}");
        }
        return v;
    }

    private static String requireVersion(String v) {
        if (v == null || !VERSION_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException("version must match X.Y or X.Y.Z");
        }
        return v;
    }

    private static String requireText(String v, String field, int maxLen) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(field + " required");
        }
        if (v.length() > maxLen) {
            throw new IllegalArgumentException(field + " too long (max " + maxLen + ")");
        }
        return v;
    }

    private static String requireCurrency(String currency) {
        if (currency == null || !CURRENCIES.contains(currency)) {
            throw new IllegalArgumentException("currency must be one of " + CURRENCIES);
        }
        return currency;
    }

    private static String requireManifestUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("manifestUrl required");
        }
        if (!url.startsWith("https://") && !url.startsWith("oci://")) {
            throw new IllegalArgumentException("manifestUrl must use https:// or oci://");
        }
        return url;
    }

    public UUID getId() { return id; }
    public String getPackId() { return packId; }
    public String getVersion() { return version; }
    public String getPublisher() { return publisher; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSector() { return sector; }
    public String getNormsCsv() { return normsCsv; }
    public int getPriceCents() { return priceCents; }
    public String getCurrency() { return currency; }
    public MarketplacePackStatus getStatus() { return status; }
    public UUID getSubmittedBy() { return submittedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewNotes() { return reviewNotes; }
    public String getSignatureHash() { return signatureHash; }
    public String getManifestUrl() { return manifestUrl; }
    public String getManifestJson() { return manifestJson; }
    public double getRatingAvg() { return ratingAvg; }
    public int getRatingCount() { return ratingCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
