package com.openlab.qualitos.quality.automateddecisions.domain;

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
 * Agrégat — décision automatisée / profilage (RGPD Art. 22).
 *
 * Décrit un traitement opérant un profilage ou une décision automatisée :
 *  - typologie ({@link AutomatedDecisionType}),
 *  - base juridique Art. 22.2 (uniquement pour AUTOMATED_DECISION_WITH_LEGAL_EFFECT),
 *  - catégories de données en entrée,
 *  - description de l'algorithme (transparence Art. 13.2.f),
 *  - importance des conséquences pour la personne,
 *  - mécanisme de révision humaine (Art. 22.3),
 *  - mécanisme d'opposition (Art. 21),
 *  - liens vers RoPA et DPIA (DPIA fortement recommandée pour Art. 22).
 *
 * Cycle de vie :
 *   DRAFT → ACTIVE → DEPRECATED → ARCHIVED
 *   ACTIVE → ARCHIVED (raccourci si abandon)
 *   DRAFT → (deletable)
 *
 * Garde-fous métier :
 *  - AUTOMATED_DECISION_WITH_LEGAL_EFFECT exige :
 *      • {@code art22LawfulBasis} renseigné (Art. 22.2),
 *      • {@code humanReviewMechanism} non vide (Art. 22.3),
 *      • {@code linkedDpiaId} renseigné (Art. 35.3.a, DPIA obligatoire),
 *      • base AUTHORIZED_BY_LAW ⇒ {@code lawfulBasisDetails} (citation légale).
 *
 * Privacy by design (OWASP A02) : aucune PII concrète — l'agrégat décrit
 * un traitement, pas un individu.
 */
public final class AutomatedDecisionRecord {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9._-]{1,63}$");

    private static final Map<AutomatedDecisionStatus, Set<AutomatedDecisionStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(AutomatedDecisionStatus.class);
        ALLOWED.put(AutomatedDecisionStatus.DRAFT,
                EnumSet.of(AutomatedDecisionStatus.ACTIVE));
        ALLOWED.put(AutomatedDecisionStatus.ACTIVE,
                EnumSet.of(AutomatedDecisionStatus.DEPRECATED,
                        AutomatedDecisionStatus.ARCHIVED));
        ALLOWED.put(AutomatedDecisionStatus.DEPRECATED,
                EnumSet.of(AutomatedDecisionStatus.ARCHIVED));
        ALLOWED.put(AutomatedDecisionStatus.ARCHIVED,
                EnumSet.noneOf(AutomatedDecisionStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private String name;
    private String description;
    private AutomatedDecisionType decisionType;
    private Art22LawfulBasis art22LawfulBasis;
    private String lawfulBasisDetails;
    private Set<String> inputDataCategories;
    private Set<UUID> linkedProcessingActivityIds;
    private UUID linkedDpiaId;
    private String algorithmDescription;
    private String significanceForSubject;
    private String humanReviewMechanism;
    private String objectionMechanism;
    private AutomatedDecisionStatus status;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public AutomatedDecisionRecord(UUID id, UUID tenantId, String reference,
                                   String name, String description,
                                   AutomatedDecisionType decisionType,
                                   Art22LawfulBasis art22LawfulBasis, String lawfulBasisDetails,
                                   Set<String> inputDataCategories,
                                   Set<UUID> linkedProcessingActivityIds, UUID linkedDpiaId,
                                   String algorithmDescription, String significanceForSubject,
                                   String humanReviewMechanism, String objectionMechanism,
                                   AutomatedDecisionStatus status,
                                   Instant effectiveFrom, Instant effectiveTo,
                                   UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.name = requireText(name, "name", 250);
        this.description = description;
        this.decisionType = Objects.requireNonNull(decisionType, "decisionType");
        this.art22LawfulBasis = art22LawfulBasis;
        this.lawfulBasisDetails = lawfulBasisDetails;
        this.inputDataCategories = sanitizeCodes(inputDataCategories);
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.linkedDpiaId = linkedDpiaId;
        this.algorithmDescription = algorithmDescription;
        this.significanceForSubject = significanceForSubject;
        this.humanReviewMechanism = humanReviewMechanism;
        this.objectionMechanism = objectionMechanism;
        this.status = Objects.requireNonNull(status, "status");
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    public static AutomatedDecisionRecord draft(UUID tenantId, String reference,
                                                String name, String description,
                                                AutomatedDecisionType decisionType,
                                                Art22LawfulBasis art22LawfulBasis,
                                                String lawfulBasisDetails,
                                                Set<String> inputDataCategories,
                                                Set<UUID> linkedProcessingActivityIds,
                                                UUID linkedDpiaId,
                                                String algorithmDescription,
                                                String significanceForSubject,
                                                String humanReviewMechanism,
                                                String objectionMechanism,
                                                UUID createdByUserId, Instant now) {
        return new AutomatedDecisionRecord(null, tenantId, reference, name, description,
                decisionType, art22LawfulBasis, lawfulBasisDetails,
                inputDataCategories, linkedProcessingActivityIds, linkedDpiaId,
                algorithmDescription, significanceForSubject,
                humanReviewMechanism, objectionMechanism,
                AutomatedDecisionStatus.DRAFT, null, null,
                createdByUserId, now, now);
    }

    public void editDraft(String name, String description,
                          AutomatedDecisionType decisionType,
                          Art22LawfulBasis art22LawfulBasis, String lawfulBasisDetails,
                          Set<String> inputDataCategories,
                          Set<UUID> linkedProcessingActivityIds, UUID linkedDpiaId,
                          String algorithmDescription, String significanceForSubject,
                          String humanReviewMechanism, String objectionMechanism,
                          Instant now) {
        if (status != AutomatedDecisionStatus.DRAFT) {
            throw new AutomatedDecisionStateException("Only DRAFT records can be edited");
        }
        this.name = requireText(name, "name", 250);
        this.description = description;
        this.decisionType = Objects.requireNonNull(decisionType, "decisionType");
        this.art22LawfulBasis = art22LawfulBasis;
        this.lawfulBasisDetails = lawfulBasisDetails;
        this.inputDataCategories = sanitizeCodes(inputDataCategories);
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.linkedDpiaId = linkedDpiaId;
        this.algorithmDescription = algorithmDescription;
        this.significanceForSubject = significanceForSubject;
        this.humanReviewMechanism = humanReviewMechanism;
        this.objectionMechanism = objectionMechanism;
        this.updatedAt = now;
    }

    /** Activation — applique les garde-fous Art. 22.2 et 22.3. */
    public void activate(Instant now) {
        ensureTransition(AutomatedDecisionStatus.ACTIVE);
        validateActivationInvariants();
        this.status = AutomatedDecisionStatus.ACTIVE;
        this.effectiveFrom = now;
        this.updatedAt = now;
    }

    public void deprecate(Instant now) {
        ensureTransition(AutomatedDecisionStatus.DEPRECATED);
        this.status = AutomatedDecisionStatus.DEPRECATED;
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        ensureTransition(AutomatedDecisionStatus.ARCHIVED);
        this.status = AutomatedDecisionStatus.ARCHIVED;
        this.effectiveTo = now;
        this.updatedAt = now;
    }

    public boolean isDraft()    { return status == AutomatedDecisionStatus.DRAFT; }
    public boolean isActive()   { return status == AutomatedDecisionStatus.ACTIVE; }
    public boolean isTerminal() { return status == AutomatedDecisionStatus.ARCHIVED; }

    private void validateActivationInvariants() {
        if (decisionType == AutomatedDecisionType.AUTOMATED_DECISION_WITH_LEGAL_EFFECT) {
            if (art22LawfulBasis == null) {
                throw new AutomatedDecisionStateException(
                        "AUTOMATED_DECISION_WITH_LEGAL_EFFECT requires art22LawfulBasis (Art. 22.2)");
            }
            if (humanReviewMechanism == null || humanReviewMechanism.isBlank()) {
                throw new AutomatedDecisionStateException(
                        "AUTOMATED_DECISION_WITH_LEGAL_EFFECT requires humanReviewMechanism (Art. 22.3)");
            }
            if (linkedDpiaId == null) {
                throw new AutomatedDecisionStateException(
                        "AUTOMATED_DECISION_WITH_LEGAL_EFFECT requires linkedDpiaId (Art. 35.3.a)");
            }
            if (art22LawfulBasis == Art22LawfulBasis.AUTHORIZED_BY_LAW
                    && (lawfulBasisDetails == null || lawfulBasisDetails.isBlank())) {
                throw new AutomatedDecisionStateException(
                        "AUTHORIZED_BY_LAW requires lawfulBasisDetails (citation légale)");
            }
        }
    }

    private void ensureTransition(AutomatedDecisionStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new AutomatedDecisionStateException(
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
    private static Set<String> sanitizeCodes(Set<String> input) {
        if (input == null) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        for (String c : input) {
            if (c == null) continue;
            String t = c.trim();
            if (t.isEmpty()) continue;
            if (!CODE_PATTERN.matcher(t).matches()) {
                throw new IllegalArgumentException(
                        "inputDataCategories: '" + t + "' must match [a-z][a-z0-9._-]{1,63}");
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
    public String getName() { return name; }
    public String getDescription() { return description; }
    public AutomatedDecisionType getDecisionType() { return decisionType; }
    public Art22LawfulBasis getArt22LawfulBasis() { return art22LawfulBasis; }
    public String getLawfulBasisDetails() { return lawfulBasisDetails; }
    public Set<String> getInputDataCategories() { return inputDataCategories; }
    public Set<UUID> getLinkedProcessingActivityIds() { return linkedProcessingActivityIds; }
    public UUID getLinkedDpiaId() { return linkedDpiaId; }
    public String getAlgorithmDescription() { return algorithmDescription; }
    public String getSignificanceForSubject() { return significanceForSubject; }
    public String getHumanReviewMechanism() { return humanReviewMechanism; }
    public String getObjectionMechanism() { return objectionMechanism; }
    public AutomatedDecisionStatus getStatus() { return status; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
