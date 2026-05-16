package com.openlab.qualitos.quality.dpia.domain;

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
 * Agrégat — Analyse d'Impact relative à la Protection des Données (AIPD/DPIA,
 * RGPD Art. 35).
 *
 * Couvre les points obligatoires de l'Art. 35§7 :
 *  (a) description systématique des opérations (linkedProcessingActivityIds
 *      vers RoPA, plus title / description structurée),
 *  (b) évaluation de la nécessité et de la proportionnalité
 *      ({@code necessityAndProportionalityNotes}),
 *  (c) évaluation des risques pour les droits et libertés
 *      ({@code risksToRightsAndFreedoms}),
 *  (d) mesures envisagées pour faire face aux risques
 *      ({@code mitigationMeasures} + {@code overallRiskLevel} post-mitigation).
 *
 * Cycle de vie (workflow DPO) :
 *   DRAFT ⇄ IN_PROGRESS → DPO_REVIEW → APPROVED|REJECTED → ARCHIVED
 *   DRAFT → (deletable)
 *
 * Garde-fous :
 *  - Soumission au DPO : nécessité + risques + mitigations renseignés.
 *  - Approbation/rejet : opinion DPO + dpoUserId obligatoires.
 *  - Niveau résiduel HIGH/SEVERE ⇒ consultationRequired (Art. 36) et
 *    consultationNotes documentés à l'approbation.
 *
 * Privacy by design (OWASP A02/A04) : aucun PII ne doit transiter par cet
 * agrégat — les références aux activités de traitement passent par UUID, et
 * les champs libres documentent des analyses, pas des identifiants individuels.
 */
public final class Dpia {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");

    private static final Map<DpiaStatus, Set<DpiaStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(DpiaStatus.class);
        ALLOWED.put(DpiaStatus.DRAFT,       EnumSet.of(DpiaStatus.IN_PROGRESS));
        ALLOWED.put(DpiaStatus.IN_PROGRESS, EnumSet.of(DpiaStatus.DRAFT, DpiaStatus.DPO_REVIEW));
        ALLOWED.put(DpiaStatus.DPO_REVIEW,  EnumSet.of(DpiaStatus.APPROVED, DpiaStatus.REJECTED));
        ALLOWED.put(DpiaStatus.APPROVED,    EnumSet.of(DpiaStatus.ARCHIVED));
        ALLOWED.put(DpiaStatus.REJECTED,    EnumSet.of(DpiaStatus.ARCHIVED));
        ALLOWED.put(DpiaStatus.ARCHIVED,    EnumSet.noneOf(DpiaStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private String title;
    private String description;
    private Set<UUID> linkedProcessingActivityIds;
    private String necessityAndProportionalityNotes;
    private String risksToRightsAndFreedoms;
    private String mitigationMeasures;
    private RiskLevel overallRiskLevel;
    private boolean consultationRequired;
    private String consultationNotes;
    private DpiaStatus status;
    private UUID dpoUserId;
    private String dpoOpinion;
    private Instant dpoOpinionAt;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private final UUID createdByUserId;
    private UUID handledByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public Dpia(UUID id, UUID tenantId, String reference, String title, String description,
                Set<UUID> linkedProcessingActivityIds,
                String necessityAndProportionalityNotes,
                String risksToRightsAndFreedoms, String mitigationMeasures,
                RiskLevel overallRiskLevel,
                boolean consultationRequired, String consultationNotes,
                DpiaStatus status,
                UUID dpoUserId, String dpoOpinion, Instant dpoOpinionAt,
                Instant effectiveFrom, Instant effectiveTo,
                UUID createdByUserId, UUID handledByUserId,
                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.title = requireText(title, "title", 250);
        this.description = description;
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.necessityAndProportionalityNotes = necessityAndProportionalityNotes;
        this.risksToRightsAndFreedoms = risksToRightsAndFreedoms;
        this.mitigationMeasures = mitigationMeasures;
        this.overallRiskLevel = Objects.requireNonNull(overallRiskLevel, "overallRiskLevel");
        this.consultationRequired = consultationRequired;
        this.consultationNotes = consultationNotes;
        this.status = Objects.requireNonNull(status, "status");
        this.dpoUserId = dpoUserId;
        this.dpoOpinion = dpoOpinion;
        this.dpoOpinionAt = dpoOpinionAt;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.handledByUserId = handledByUserId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    /** Factory — création d'une DPIA DRAFT. */
    public static Dpia draft(UUID tenantId, String reference, String title, String description,
                             Set<UUID> linkedProcessingActivityIds,
                             RiskLevel initialRiskLevel, UUID createdByUserId, Instant now) {
        return new Dpia(null, tenantId, reference, title, description,
                linkedProcessingActivityIds,
                null, null, null,
                initialRiskLevel, false, null,
                DpiaStatus.DRAFT,
                null, null, null, null, null,
                createdByUserId, null, now, now);
    }

    /** Édition — seulement DRAFT. Met à jour tous les champs analytiques. */
    public void editDraft(String title, String description,
                          Set<UUID> linkedProcessingActivityIds,
                          String necessityAndProportionalityNotes,
                          String risksToRightsAndFreedoms,
                          String mitigationMeasures,
                          RiskLevel overallRiskLevel,
                          boolean consultationRequired, String consultationNotes,
                          Instant now) {
        if (status != DpiaStatus.DRAFT) {
            throw new DpiaStateException("Only DRAFT DPIAs can be edited");
        }
        this.title = requireText(title, "title", 250);
        this.description = description;
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.necessityAndProportionalityNotes = necessityAndProportionalityNotes;
        this.risksToRightsAndFreedoms = risksToRightsAndFreedoms;
        this.mitigationMeasures = mitigationMeasures;
        this.overallRiskLevel = Objects.requireNonNull(overallRiskLevel, "overallRiskLevel");
        this.consultationRequired = consultationRequired;
        this.consultationNotes = consultationNotes;
        this.updatedAt = now;
    }

    public void start(UUID handledByUserId, Instant now) {
        ensureTransition(DpiaStatus.IN_PROGRESS);
        this.handledByUserId = handledByUserId;
        this.status = DpiaStatus.IN_PROGRESS;
        this.updatedAt = now;
    }

    public void returnToDraft(Instant now) {
        if (status != DpiaStatus.IN_PROGRESS) {
            throw new DpiaStateException("Only IN_PROGRESS DPIAs can return to DRAFT");
        }
        this.status = DpiaStatus.DRAFT;
        this.updatedAt = now;
    }

    public void submitToDpo(Instant now) {
        ensureTransition(DpiaStatus.DPO_REVIEW);
        requireText(necessityAndProportionalityNotes,
                "necessityAndProportionalityNotes", 8000);
        requireText(risksToRightsAndFreedoms, "risksToRightsAndFreedoms", 8000);
        requireText(mitigationMeasures, "mitigationMeasures", 8000);
        this.status = DpiaStatus.DPO_REVIEW;
        this.updatedAt = now;
    }

    public void approve(UUID dpoUserId, String dpoOpinion, Instant now) {
        ensureTransition(DpiaStatus.APPROVED);
        if (dpoUserId == null) throw new DpiaStateException("dpoUserId required");
        requireText(dpoOpinion, "dpoOpinion", 8000);
        if (overallRiskLevel.requiresPriorConsultation()) {
            if (!consultationRequired) {
                throw new DpiaStateException(
                        "Risk level " + overallRiskLevel + " requires Art. 36 prior consultation flag");
            }
            requireText(consultationNotes, "consultationNotes", 8000);
        }
        this.dpoUserId = dpoUserId;
        this.dpoOpinion = dpoOpinion;
        this.dpoOpinionAt = now;
        this.effectiveFrom = now;
        this.status = DpiaStatus.APPROVED;
        this.updatedAt = now;
    }

    public void reject(UUID dpoUserId, String dpoOpinion, Instant now) {
        ensureTransition(DpiaStatus.REJECTED);
        if (dpoUserId == null) throw new DpiaStateException("dpoUserId required");
        requireText(dpoOpinion, "dpoOpinion", 8000);
        this.dpoUserId = dpoUserId;
        this.dpoOpinion = dpoOpinion;
        this.dpoOpinionAt = now;
        this.status = DpiaStatus.REJECTED;
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        ensureTransition(DpiaStatus.ARCHIVED);
        this.effectiveTo = now;
        this.status = DpiaStatus.ARCHIVED;
        this.updatedAt = now;
    }

    public boolean isDraft()      { return status == DpiaStatus.DRAFT; }
    public boolean isTerminal()   {
        return status == DpiaStatus.APPROVED || status == DpiaStatus.REJECTED
                || status == DpiaStatus.ARCHIVED;
    }

    private void ensureTransition(DpiaStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new DpiaStateException(
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
        if (v == null || v.isBlank()) throw new DpiaStateException(f + " required");
        if (v.length() > maxLen) throw new DpiaStateException(
                f + " too long (max " + maxLen + ")");
        return v;
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
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Set<UUID> getLinkedProcessingActivityIds() { return linkedProcessingActivityIds; }
    public String getNecessityAndProportionalityNotes() { return necessityAndProportionalityNotes; }
    public String getRisksToRightsAndFreedoms() { return risksToRightsAndFreedoms; }
    public String getMitigationMeasures() { return mitigationMeasures; }
    public RiskLevel getOverallRiskLevel() { return overallRiskLevel; }
    public boolean isConsultationRequired() { return consultationRequired; }
    public String getConsultationNotes() { return consultationNotes; }
    public DpiaStatus getStatus() { return status; }
    public UUID getDpoUserId() { return dpoUserId; }
    public String getDpoOpinion() { return dpoOpinion; }
    public Instant getDpoOpinionAt() { return dpoOpinionAt; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public UUID getHandledByUserId() { return handledByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
