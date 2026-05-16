package com.openlab.qualitos.quality.retention.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat règle de rétention (RGPD Art. 5.1.e — "Limitation de la conservation").
 *
 * Une règle s'applique à une catégorie de données ({@code dataCategoryCode}) pour
 * un tenant, et définit une durée de conservation maximale ({@code retentionPeriod}).
 *
 * Cycle de vie :
 *   DRAFT  → ACTIVE  (activate) → ARCHIVED (archive)
 *   DRAFT  → (supprimable)
 *
 * Invariants :
 *  - DRAFT : éditable.
 *  - ACTIVE : immutable — toute évolution requiert l'archivage de la règle
 *    courante et la création d'une nouvelle DRAFT (traçabilité audit).
 *  - {@code legalBasis} obligatoire (Art. 6 — base légale du traitement).
 *  - {@code retentionPeriod} entre 1 jour et 100 ans (cap raisonnable, refus
 *    des durées indéfinies non motivées).
 *  - Unicité d'une règle ACTIVE par (tenant, dataCategoryCode) à un instant
 *    donné — garantie par le service application + index DB.
 */
public final class RetentionRule {

    private static final Pattern DATA_CATEGORY_CODE =
            Pattern.compile("^[a-z][a-z0-9._-]{1,63}$");
    public static final Duration MIN_PERIOD = Duration.ofDays(1);
    public static final Duration MAX_PERIOD = Duration.ofDays(365L * 100);

    private UUID id;
    private final UUID tenantId;
    private final String dataCategoryCode;
    private String dataCategoryLabel;
    private Duration retentionPeriod;
    private String legalBasis;
    private String lawfulBasisReference;
    private RetentionRuleStatus status;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public RetentionRule(UUID id, UUID tenantId,
                         String dataCategoryCode, String dataCategoryLabel,
                         Duration retentionPeriod, String legalBasis, String lawfulBasisReference,
                         RetentionRuleStatus status, Instant effectiveFrom, Instant effectiveTo,
                         UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.dataCategoryCode = requireCategoryCode(dataCategoryCode);
        this.dataCategoryLabel = dataCategoryLabel;
        this.retentionPeriod = requirePeriod(retentionPeriod);
        this.legalBasis = requireText(legalBasis, "legalBasis");
        this.lawfulBasisReference = lawfulBasisReference;
        this.status = Objects.requireNonNull(status, "status");
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    /** Factory — création d'une règle DRAFT. */
    public static RetentionRule draft(UUID tenantId,
                                      String dataCategoryCode, String dataCategoryLabel,
                                      Duration retentionPeriod,
                                      String legalBasis, String lawfulBasisReference,
                                      UUID createdByUserId, Instant now) {
        return new RetentionRule(null, tenantId, dataCategoryCode, dataCategoryLabel,
                retentionPeriod, legalBasis, lawfulBasisReference,
                RetentionRuleStatus.DRAFT, null, null,
                createdByUserId, now, now);
    }

    /** Édition (seulement DRAFT). */
    public void editDraft(String dataCategoryLabel,
                          Duration retentionPeriod,
                          String legalBasis, String lawfulBasisReference,
                          Instant now) {
        if (status != RetentionRuleStatus.DRAFT) {
            throw new RetentionRuleStateException("Only DRAFT rules can be edited");
        }
        this.dataCategoryLabel = dataCategoryLabel;
        this.retentionPeriod = requirePeriod(retentionPeriod);
        this.legalBasis = requireText(legalBasis, "legalBasis");
        this.lawfulBasisReference = lawfulBasisReference;
        this.updatedAt = now;
    }

    /** Activation : DRAFT → ACTIVE. */
    public void activate(Instant now) {
        if (status != RetentionRuleStatus.DRAFT) {
            throw new RetentionRuleStateException("Only DRAFT rules can be activated");
        }
        this.status = RetentionRuleStatus.ACTIVE;
        this.effectiveFrom = now;
        this.updatedAt = now;
    }

    /** Archivage : ACTIVE → ARCHIVED (terminal). */
    public void archive(Instant now) {
        if (status != RetentionRuleStatus.ACTIVE) {
            throw new RetentionRuleStateException("Only ACTIVE rules can be archived");
        }
        this.status = RetentionRuleStatus.ARCHIVED;
        this.effectiveTo = now;
        this.updatedAt = now;
    }

    public boolean isDraft()    { return status == RetentionRuleStatus.DRAFT; }
    public boolean isActive()   { return status == RetentionRuleStatus.ACTIVE; }
    public boolean isArchived() { return status == RetentionRuleStatus.ARCHIVED; }

    /** Date d'effacement calculée pour un enregistrement créé à {@code recordCreatedAt}. */
    public Instant computeErasureAt(Instant recordCreatedAt) {
        Objects.requireNonNull(recordCreatedAt, "recordCreatedAt");
        return recordCreatedAt.plus(retentionPeriod);
    }

    private static String requireCategoryCode(String v) {
        if (v == null || !DATA_CATEGORY_CODE.matcher(v).matches()) {
            throw new IllegalArgumentException(
                    "dataCategoryCode must match [a-z][a-z0-9._-]{1,63}");
        }
        return v;
    }
    private static Duration requirePeriod(Duration p) {
        if (p == null || p.compareTo(MIN_PERIOD) < 0 || p.compareTo(MAX_PERIOD) > 0) {
            throw new IllegalArgumentException(
                    "retentionPeriod must be between 1 day and 100 years");
        }
        return p;
    }
    private static String requireText(String v, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " required");
        if (v.length() > 2000) throw new IllegalArgumentException(f + " too long (max 2000)");
        return v;
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getDataCategoryCode() { return dataCategoryCode; }
    public String getDataCategoryLabel() { return dataCategoryLabel; }
    public Duration getRetentionPeriod() { return retentionPeriod; }
    public String getLegalBasis() { return legalBasis; }
    public String getLawfulBasisReference() { return lawfulBasisReference; }
    public RetentionRuleStatus getStatus() { return status; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
