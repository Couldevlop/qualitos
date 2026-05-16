package com.openlab.qualitos.quality.nis2measures.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat — mesure de gestion des risques cyber NIS2 (Art. 21.2.a-j).
 *
 * Cycle de vie :
 *   PLANNED → IN_PROGRESS → IMPLEMENTED → VERIFIED → DEPRECATED
 *   PLANNED → (deletable)
 *
 * Garde-fous métier :
 *  - VERIFIED exige {@code lastReviewedAt} et {@code reviewedByUserId}.
 *  - {@code maturityLevel} borné [1, 5] (modèle CMMI-like).
 *  - {@code reviewIntervalDays} borné [30, 1095] (1 mois à 3 ans).
 *  - {@code residualRiskRating} CRITICAL ⇒ {@code criticalRiskJustification}
 *    obligatoire (escalade direction).
 *  - À chaque review, {@code nextReviewDueAt = lastReviewedAt + reviewIntervalDays}.
 *
 * Privacy by design (OWASP A02) : aucun PII — l'agrégat décrit l'organisation
 * de la sécurité, pas des individus (le propriétaire est référencé par UUID).
 */
public final class Nis2RiskMeasure {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[^\\s]{1,1022}$");
    public static final int MIN_MATURITY = 1;
    public static final int MAX_MATURITY = 5;
    public static final int MIN_REVIEW_INTERVAL_DAYS = 30;
    public static final int MAX_REVIEW_INTERVAL_DAYS = 1095;

    private static final Map<Nis2MeasureStatus, Set<Nis2MeasureStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(Nis2MeasureStatus.class);
        ALLOWED.put(Nis2MeasureStatus.PLANNED,
                EnumSet.of(Nis2MeasureStatus.IN_PROGRESS, Nis2MeasureStatus.DEPRECATED));
        ALLOWED.put(Nis2MeasureStatus.IN_PROGRESS,
                EnumSet.of(Nis2MeasureStatus.IMPLEMENTED, Nis2MeasureStatus.DEPRECATED));
        ALLOWED.put(Nis2MeasureStatus.IMPLEMENTED,
                EnumSet.of(Nis2MeasureStatus.VERIFIED, Nis2MeasureStatus.DEPRECATED));
        ALLOWED.put(Nis2MeasureStatus.VERIFIED,
                EnumSet.of(Nis2MeasureStatus.DEPRECATED));
        ALLOWED.put(Nis2MeasureStatus.DEPRECATED,
                EnumSet.noneOf(Nis2MeasureStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private final Nis2MeasureCategory category;
    private String title;
    private String description;
    private Nis2MeasureStatus status;
    private UUID ownerUserId;
    private int maturityLevel;
    private ResidualRiskRating residualRiskRating;
    private String criticalRiskJustification;
    private int reviewIntervalDays;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private Instant lastReviewedAt;
    private UUID reviewedByUserId;
    private Instant nextReviewDueAt;
    private Set<String> evidenceUrls;
    private Set<UUID> linkedProcessingActivityIds;
    private Set<UUID> linkedProcessorAgreementIds;
    private String notes;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public Nis2RiskMeasure(UUID id, UUID tenantId, String reference,
                           Nis2MeasureCategory category, String title, String description,
                           Nis2MeasureStatus status, UUID ownerUserId,
                           int maturityLevel,
                           ResidualRiskRating residualRiskRating, String criticalRiskJustification,
                           int reviewIntervalDays,
                           Instant effectiveFrom, Instant effectiveTo,
                           Instant lastReviewedAt, UUID reviewedByUserId, Instant nextReviewDueAt,
                           Set<String> evidenceUrls,
                           Set<UUID> linkedProcessingActivityIds,
                           Set<UUID> linkedProcessorAgreementIds,
                           String notes,
                           UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.category = Objects.requireNonNull(category, "category");
        this.title = requireText(title, "title", 250);
        this.description = description;
        this.status = Objects.requireNonNull(status, "status");
        this.ownerUserId = ownerUserId;
        this.maturityLevel = requireMaturity(maturityLevel);
        this.residualRiskRating = Objects.requireNonNull(residualRiskRating, "residualRiskRating");
        this.criticalRiskJustification = criticalRiskJustification;
        this.reviewIntervalDays = requireReviewInterval(reviewIntervalDays);
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.lastReviewedAt = lastReviewedAt;
        this.reviewedByUserId = reviewedByUserId;
        this.nextReviewDueAt = nextReviewDueAt;
        this.evidenceUrls = sanitizeUrls(evidenceUrls);
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.linkedProcessorAgreementIds = sanitizeIds(linkedProcessorAgreementIds);
        this.notes = notes;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
        validateCriticalRiskInvariant();
    }

    public static Nis2RiskMeasure plan(UUID tenantId, String reference,
                                       Nis2MeasureCategory category,
                                       String title, String description,
                                       UUID ownerUserId, int maturityLevel,
                                       ResidualRiskRating residualRiskRating,
                                       String criticalRiskJustification,
                                       int reviewIntervalDays,
                                       Set<String> evidenceUrls,
                                       Set<UUID> linkedProcessingActivityIds,
                                       Set<UUID> linkedProcessorAgreementIds,
                                       String notes,
                                       UUID createdByUserId, Instant now) {
        return new Nis2RiskMeasure(null, tenantId, reference, category, title, description,
                Nis2MeasureStatus.PLANNED, ownerUserId,
                maturityLevel, residualRiskRating, criticalRiskJustification,
                reviewIntervalDays, null, null,
                null, null, null,
                evidenceUrls, linkedProcessingActivityIds, linkedProcessorAgreementIds,
                notes, createdByUserId, now, now);
    }

    public void edit(String title, String description, UUID ownerUserId,
                     int maturityLevel,
                     ResidualRiskRating residualRiskRating, String criticalRiskJustification,
                     int reviewIntervalDays,
                     Set<String> evidenceUrls,
                     Set<UUID> linkedProcessingActivityIds,
                     Set<UUID> linkedProcessorAgreementIds,
                     String notes, Instant now) {
        if (status == Nis2MeasureStatus.DEPRECATED) {
            throw new Nis2MeasureStateException("Cannot edit DEPRECATED measure");
        }
        this.title = requireText(title, "title", 250);
        this.description = description;
        this.ownerUserId = ownerUserId;
        this.maturityLevel = requireMaturity(maturityLevel);
        this.residualRiskRating = Objects.requireNonNull(residualRiskRating, "residualRiskRating");
        this.criticalRiskJustification = criticalRiskJustification;
        this.reviewIntervalDays = requireReviewInterval(reviewIntervalDays);
        this.evidenceUrls = sanitizeUrls(evidenceUrls);
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.linkedProcessorAgreementIds = sanitizeIds(linkedProcessorAgreementIds);
        this.notes = notes;
        this.updatedAt = now;
        validateCriticalRiskInvariant();
    }

    public void startImplementation(Instant now) {
        ensureTransition(Nis2MeasureStatus.IN_PROGRESS);
        this.status = Nis2MeasureStatus.IN_PROGRESS;
        if (this.effectiveFrom == null) this.effectiveFrom = now;
        this.updatedAt = now;
    }

    public void markImplemented(Instant now) {
        ensureTransition(Nis2MeasureStatus.IMPLEMENTED);
        this.status = Nis2MeasureStatus.IMPLEMENTED;
        this.updatedAt = now;
    }

    /** Vérification : enregistre la revue, calcule la prochaine échéance. */
    public void verify(UUID reviewerUserId, Instant reviewedAt, Instant now) {
        ensureTransition(Nis2MeasureStatus.VERIFIED);
        if (reviewerUserId == null) {
            throw new Nis2MeasureStateException("reviewedByUserId required to verify");
        }
        if (reviewedAt == null) {
            throw new Nis2MeasureStateException("reviewedAt required to verify");
        }
        this.lastReviewedAt = reviewedAt;
        this.reviewedByUserId = reviewerUserId;
        this.nextReviewDueAt = reviewedAt.plus(Duration.ofDays(reviewIntervalDays));
        this.status = Nis2MeasureStatus.VERIFIED;
        this.updatedAt = now;
    }

    /** Re-revue (sur mesure déjà VERIFIED) — repousse la prochaine échéance. */
    public void review(UUID reviewerUserId, Instant reviewedAt, Instant now) {
        if (status != Nis2MeasureStatus.VERIFIED) {
            throw new Nis2MeasureStateException("Only VERIFIED measures can be re-reviewed");
        }
        if (reviewerUserId == null) {
            throw new Nis2MeasureStateException("reviewedByUserId required");
        }
        if (reviewedAt == null) {
            throw new Nis2MeasureStateException("reviewedAt required");
        }
        this.lastReviewedAt = reviewedAt;
        this.reviewedByUserId = reviewerUserId;
        this.nextReviewDueAt = reviewedAt.plus(Duration.ofDays(reviewIntervalDays));
        this.updatedAt = now;
    }

    public void deprecate(Instant now) {
        ensureTransition(Nis2MeasureStatus.DEPRECATED);
        this.status = Nis2MeasureStatus.DEPRECATED;
        this.effectiveTo = now;
        this.updatedAt = now;
    }

    public boolean isPlanned()    { return status == Nis2MeasureStatus.PLANNED; }
    public boolean isVerified()   { return status == Nis2MeasureStatus.VERIFIED; }
    public boolean isTerminal()   { return status == Nis2MeasureStatus.DEPRECATED; }

    public boolean isReviewOverdue(Instant ref) {
        return nextReviewDueAt != null
                && status != Nis2MeasureStatus.DEPRECATED
                && ref.isAfter(nextReviewDueAt);
    }

    private void validateCriticalRiskInvariant() {
        if (residualRiskRating == ResidualRiskRating.CRITICAL
                && (criticalRiskJustification == null || criticalRiskJustification.isBlank())) {
            throw new Nis2MeasureStateException(
                    "CRITICAL residual risk requires criticalRiskJustification "
                    + "(executive attention)");
        }
    }

    private void ensureTransition(Nis2MeasureStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new Nis2MeasureStateException(
                    "Transition " + status + " → " + target + " is not allowed");
        }
    }

    private static String requireReference(String v) {
        if (v == null || !REF_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException(
                    "reference must match [A-Z][A-Z0-9_-]{1,63}");
        }
        return v;
    }
    private static String requireText(String v, String f, int maxLen) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " required");
        if (v.length() > maxLen) throw new IllegalArgumentException(
                f + " too long (max " + maxLen + ")");
        return v;
    }
    private static int requireMaturity(int v) {
        if (v < MIN_MATURITY || v > MAX_MATURITY) {
            throw new IllegalArgumentException(
                    "maturityLevel must be in [" + MIN_MATURITY + ", " + MAX_MATURITY + "]");
        }
        return v;
    }
    private static int requireReviewInterval(int v) {
        if (v < MIN_REVIEW_INTERVAL_DAYS || v > MAX_REVIEW_INTERVAL_DAYS) {
            throw new IllegalArgumentException(
                    "reviewIntervalDays must be in ["
                    + MIN_REVIEW_INTERVAL_DAYS + ", " + MAX_REVIEW_INTERVAL_DAYS + "]");
        }
        return v;
    }
    private static Set<String> sanitizeUrls(Set<String> input) {
        if (input == null) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        for (String u : input) {
            if (u == null) continue;
            String t = u.trim();
            if (t.isEmpty()) continue;
            if (!URL_PATTERN.matcher(t).matches()) {
                throw new IllegalArgumentException(
                        "evidenceUrls: '" + t + "' must be a valid http(s) URL");
            }
            out.add(t);
        }
        return Collections.unmodifiableSet(out);
    }
    private static Set<UUID> sanitizeIds(Set<UUID> input) {
        if (input == null) return Collections.emptySet();
        Set<UUID> out = new LinkedHashSet<>();
        for (UUID u : input) if (u != null) out.add(u);
        return Collections.unmodifiableSet(out);
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getReference() { return reference; }
    public Nis2MeasureCategory getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Nis2MeasureStatus getStatus() { return status; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public int getMaturityLevel() { return maturityLevel; }
    public ResidualRiskRating getResidualRiskRating() { return residualRiskRating; }
    public String getCriticalRiskJustification() { return criticalRiskJustification; }
    public int getReviewIntervalDays() { return reviewIntervalDays; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public UUID getReviewedByUserId() { return reviewedByUserId; }
    public Instant getNextReviewDueAt() { return nextReviewDueAt; }
    public Set<String> getEvidenceUrls() { return evidenceUrls; }
    public Set<UUID> getLinkedProcessingActivityIds() { return linkedProcessingActivityIds; }
    public Set<UUID> getLinkedProcessorAgreementIds() { return linkedProcessorAgreementIds; }
    public String getNotes() { return notes; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
