package com.openlab.qualitos.quality.marketplace.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Aggregate — a normative / industry pack offered by a third-party publisher.
 * CLAUDE.md §8.11. CROSS-tenant: a pack is published once, consumed by many tenants.
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
    private final int priceCents;
    private final String currency;
    private boolean verified;
    private UUID verifiedBy;
    private Instant verifiedAt;
    private String signatureHash;
    private final String manifestUrl;
    private final Instant createdAt;
    private Instant updatedAt;

    public MarketplacePack(UUID id, String packId, String version,
                           String publisher, String title, String description,
                           String sector, int priceCents, String currency,
                           boolean verified, UUID verifiedBy, Instant verifiedAt,
                           String signatureHash, String manifestUrl,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.packId = requirePackId(packId);
        this.version = requireVersion(version);
        this.publisher = requireText(publisher, "publisher", 120);
        this.title = requireText(title, "title", 250);
        this.description = description;
        this.sector = requireText(sector, "sector", 80);
        if (priceCents < 0) {
            throw new IllegalArgumentException("priceCents must be >= 0");
        }
        this.priceCents = priceCents;
        this.currency = requireCurrency(currency);
        this.verified = verified;
        this.verifiedBy = verifiedBy;
        this.verifiedAt = verifiedAt;
        this.signatureHash = signatureHash;
        this.manifestUrl = requireManifestUrl(manifestUrl);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    public static MarketplacePack register(String packId, String version,
                                           String publisher, String title,
                                           String description, String sector,
                                           int priceCents, String currency,
                                           String manifestUrl,
                                           String signatureHash, Instant now) {
        return new MarketplacePack(null, packId, version, publisher, title,
                description, sector, priceCents, currency, false, null, null,
                signatureHash, manifestUrl, now, now);
    }

    public void verify(UUID superAdminId, Instant now) {
        Objects.requireNonNull(superAdminId, "superAdminId");
        if (this.verified) {
            throw new MarketplacePackStateException("pack already verified");
        }
        if (signatureHash == null || signatureHash.length() < 16) {
            throw new MarketplacePackStateException(
                    "signatureHash required before verification");
        }
        this.verified = true;
        this.verifiedBy = superAdminId;
        this.verifiedAt = now;
        this.updatedAt = now;
    }

    public void assignId(UUID id) { this.id = id; }

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
    public int getPriceCents() { return priceCents; }
    public String getCurrency() { return currency; }
    public boolean isVerified() { return verified; }
    public UUID getVerifiedBy() { return verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public String getSignatureHash() { return signatureHash; }
    public String getManifestUrl() { return manifestUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
