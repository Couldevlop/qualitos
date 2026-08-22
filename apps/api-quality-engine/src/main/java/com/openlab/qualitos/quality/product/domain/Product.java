package com.openlab.qualitos.quality.product.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat — un produit fabriqué ou servi par le tenant.
 *
 * <p>Objet Java nu : ni Spring, ni JPA, ni HTTP. Ses invariants tiennent dans ses
 * propres méthodes, donc un appel interne qui contournerait la validation du DTO
 * d'entrée se heurte quand même à eux.
 *
 * <p>Le code est normalisé (majuscules, espaces retirés) parce qu'il sert de clé
 * humaine : « ref-4471 » et « REF-4471 » désignent la même pièce, et deux fiches
 * pour une même pièce rendraient le PFMEA ininterprétable.
 */
public final class Product {

    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9._-]{0,63}$");

    private UUID id;
    private final UUID tenantId;
    private String code;
    private String designation;
    private String family;
    private String revisionIndex;
    private ProductStatus status;
    private String customerLabel;
    private String siteLabel;
    private UUID ownerUserId;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    private Product(UUID tenantId, String code, String designation, UUID createdBy, Instant now) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.code = requireCode(code);
        this.designation = requireDesignation(designation);
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        this.createdAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
        this.status = ProductStatus.DRAFT;
    }

    public static Product create(UUID tenantId, String code, String designation,
                                 UUID createdBy, Instant now) {
        return new Product(tenantId, code, designation, createdBy, now);
    }

    /** Reconstruction depuis la persistance : aucun invariant de transition rejoué. */
    public static Product rehydrate(UUID id, UUID tenantId, String code, String designation,
                                    String family, String revisionIndex, ProductStatus status,
                                    String customerLabel, String siteLabel, UUID ownerUserId,
                                    UUID createdBy, Instant createdAt, Instant updatedAt) {
        Product p = new Product(tenantId, code, designation, createdBy, createdAt);
        p.id = id;
        p.family = family;
        p.revisionIndex = revisionIndex;
        p.status = status == null ? ProductStatus.DRAFT : status;
        p.customerLabel = customerLabel;
        p.siteLabel = siteLabel;
        p.ownerUserId = ownerUserId;
        p.updatedAt = updatedAt == null ? createdAt : updatedAt;
        return p;
    }

    private static String requireCode(String value) {
        String normalised = value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
        if (!CODE.matcher(normalised).matches()) {
            throw new IllegalArgumentException("Invalid product code: " + value);
        }
        return normalised;
    }

    private static String requireDesignation(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || trimmed.length() > 250) {
            throw new IllegalArgumentException("Invalid product designation");
        }
        return trimmed;
    }

    private void requireMutable() {
        if (status == ProductStatus.OBSOLETE) {
            throw new ProductStateException("An obsolete product cannot be modified: " + code);
        }
    }

    public void rename(String newDesignation) {
        requireMutable();
        this.designation = requireDesignation(newDesignation);
        touch();
    }

    public void describe(String family, String revisionIndex, String customerLabel,
                         String siteLabel, UUID ownerUserId) {
        requireMutable();
        this.family = family;
        this.revisionIndex = revisionIndex;
        this.customerLabel = customerLabel;
        this.siteLabel = siteLabel;
        this.ownerUserId = ownerUserId;
        touch();
    }

    public void activate() {
        if (status == ProductStatus.OBSOLETE) {
            throw new ProductStateException("An obsolete product cannot be reactivated: " + code);
        }
        this.status = ProductStatus.ACTIVE;
        touch();
    }

    public void markObsolete() {
        this.status = ProductStatus.OBSOLETE;
        touch();
    }

    private void touch() { this.updatedAt = Instant.now(); }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getCode() { return code; }
    public String getDesignation() { return designation; }
    public String getFamily() { return family; }
    public String getRevisionIndex() { return revisionIndex; }
    public ProductStatus getStatus() { return status; }
    public String getCustomerLabel() { return customerLabel; }
    public String getSiteLabel() { return siteLabel; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
