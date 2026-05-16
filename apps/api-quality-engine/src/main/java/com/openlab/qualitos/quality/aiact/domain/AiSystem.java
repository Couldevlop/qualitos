package com.openlab.qualitos.quality.aiact.domain;

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
 * Agrégat — système d'IA au sens AI Act (UE 2024/1689).
 *
 * Cycle de vie :
 *   DRAFT → REGISTERED → IN_USE → DECOMMISSIONED
 *   DRAFT|REGISTERED → WITHDRAWN
 *
 * Garde-fous métier (validés en domaine ET en DB) :
 *  - UNACCEPTABLE : interdiction de passer en REGISTERED ou IN_USE (Art. 5).
 *  - HIGH en IN_USE : exige conformityAssessmentEvidenceUrl,
 *    humanOversightDescription et transparencyMeasures (Art. 9-15).
 *  - LIMITED en IN_USE : exige transparencyMeasures (Art. 50).
 *  - {@code generalPurpose=true} (GPAI) : on ne contraint pas la classification —
 *    un même GPAI peut être à différents niveaux selon les déploiements.
 *
 * Privacy by design (OWASP A02) : aucun PII — l'agrégat décrit le système,
 * pas les utilisateurs finaux (référencés via RoPA UUID).
 */
public final class AiSystem {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[^\\s]{1,1022}$");

    private static final Map<AiSystemStatus, Set<AiSystemStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(AiSystemStatus.class);
        ALLOWED.put(AiSystemStatus.DRAFT,
                EnumSet.of(AiSystemStatus.REGISTERED, AiSystemStatus.WITHDRAWN));
        ALLOWED.put(AiSystemStatus.REGISTERED,
                EnumSet.of(AiSystemStatus.IN_USE, AiSystemStatus.WITHDRAWN));
        ALLOWED.put(AiSystemStatus.IN_USE,
                EnumSet.of(AiSystemStatus.DECOMMISSIONED));
        ALLOWED.put(AiSystemStatus.DECOMMISSIONED,
                EnumSet.noneOf(AiSystemStatus.class));
        ALLOWED.put(AiSystemStatus.WITHDRAWN,
                EnumSet.noneOf(AiSystemStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private String name;
    private String description;
    private String providerName;
    private String intendedPurpose;
    private AiRiskClassification riskClassification;
    private AiSystemRole role;
    private boolean generalPurpose;
    private AiSystemStatus status;
    private String conformityAssessmentEvidenceUrl;
    private String ceMarkingNumber;
    private String humanOversightDescription;
    private String transparencyMeasures;
    private String dataGovernanceNotes;
    private UUID linkedDpiaId;
    private Set<UUID> linkedProcessingActivityIds;
    private Set<UUID> linkedAutomatedDecisionIds;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private String withdrawalReason;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public AiSystem(UUID id, UUID tenantId, String reference,
                    String name, String description,
                    String providerName, String intendedPurpose,
                    AiRiskClassification riskClassification, AiSystemRole role,
                    boolean generalPurpose, AiSystemStatus status,
                    String conformityAssessmentEvidenceUrl, String ceMarkingNumber,
                    String humanOversightDescription, String transparencyMeasures,
                    String dataGovernanceNotes,
                    UUID linkedDpiaId,
                    Set<UUID> linkedProcessingActivityIds,
                    Set<UUID> linkedAutomatedDecisionIds,
                    Instant effectiveFrom, Instant effectiveTo,
                    String withdrawalReason,
                    UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.name = requireText(name, "name", 250);
        this.description = description;
        this.providerName = providerName;
        this.intendedPurpose = requireText(intendedPurpose, "intendedPurpose", 4000);
        this.riskClassification = Objects.requireNonNull(riskClassification, "riskClassification");
        this.role = Objects.requireNonNull(role, "role");
        this.generalPurpose = generalPurpose;
        this.status = Objects.requireNonNull(status, "status");
        this.conformityAssessmentEvidenceUrl = sanitizeUrl(conformityAssessmentEvidenceUrl);
        this.ceMarkingNumber = ceMarkingNumber;
        this.humanOversightDescription = humanOversightDescription;
        this.transparencyMeasures = transparencyMeasures;
        this.dataGovernanceNotes = dataGovernanceNotes;
        this.linkedDpiaId = linkedDpiaId;
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.linkedAutomatedDecisionIds = sanitizeIds(linkedAutomatedDecisionIds);
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.withdrawalReason = withdrawalReason;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    public static AiSystem draft(UUID tenantId, String reference,
                                 String name, String description,
                                 String providerName, String intendedPurpose,
                                 AiRiskClassification riskClassification,
                                 AiSystemRole role, boolean generalPurpose,
                                 String conformityAssessmentEvidenceUrl, String ceMarkingNumber,
                                 String humanOversightDescription, String transparencyMeasures,
                                 String dataGovernanceNotes,
                                 UUID linkedDpiaId,
                                 Set<UUID> linkedProcessingActivityIds,
                                 Set<UUID> linkedAutomatedDecisionIds,
                                 UUID createdByUserId, Instant now) {
        return new AiSystem(null, tenantId, reference, name, description,
                providerName, intendedPurpose, riskClassification, role, generalPurpose,
                AiSystemStatus.DRAFT,
                conformityAssessmentEvidenceUrl, ceMarkingNumber,
                humanOversightDescription, transparencyMeasures, dataGovernanceNotes,
                linkedDpiaId, linkedProcessingActivityIds, linkedAutomatedDecisionIds,
                null, null, null, createdByUserId, now, now);
    }

    public void editDraft(String name, String description,
                          String providerName, String intendedPurpose,
                          AiRiskClassification riskClassification,
                          AiSystemRole role, boolean generalPurpose,
                          String conformityAssessmentEvidenceUrl, String ceMarkingNumber,
                          String humanOversightDescription, String transparencyMeasures,
                          String dataGovernanceNotes,
                          UUID linkedDpiaId,
                          Set<UUID> linkedProcessingActivityIds,
                          Set<UUID> linkedAutomatedDecisionIds,
                          Instant now) {
        if (status != AiSystemStatus.DRAFT) {
            throw new AiSystemStateException("Only DRAFT systems can be edited");
        }
        this.name = requireText(name, "name", 250);
        this.description = description;
        this.providerName = providerName;
        this.intendedPurpose = requireText(intendedPurpose, "intendedPurpose", 4000);
        this.riskClassification = Objects.requireNonNull(riskClassification, "riskClassification");
        this.role = Objects.requireNonNull(role, "role");
        this.generalPurpose = generalPurpose;
        this.conformityAssessmentEvidenceUrl = sanitizeUrl(conformityAssessmentEvidenceUrl);
        this.ceMarkingNumber = ceMarkingNumber;
        this.humanOversightDescription = humanOversightDescription;
        this.transparencyMeasures = transparencyMeasures;
        this.dataGovernanceNotes = dataGovernanceNotes;
        this.linkedDpiaId = linkedDpiaId;
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.linkedAutomatedDecisionIds = sanitizeIds(linkedAutomatedDecisionIds);
        this.updatedAt = now;
    }

    public void register(Instant now) {
        ensureTransition(AiSystemStatus.REGISTERED);
        validateNotProhibited("register");
        this.status = AiSystemStatus.REGISTERED;
        this.updatedAt = now;
    }

    public void putInUse(Instant now) {
        ensureTransition(AiSystemStatus.IN_USE);
        validateNotProhibited("put in use");
        validateInUseInvariants();
        this.status = AiSystemStatus.IN_USE;
        this.effectiveFrom = now;
        this.updatedAt = now;
    }

    public void decommission(Instant now) {
        ensureTransition(AiSystemStatus.DECOMMISSIONED);
        this.status = AiSystemStatus.DECOMMISSIONED;
        this.effectiveTo = now;
        this.updatedAt = now;
    }

    public void withdraw(String reason, Instant now) {
        ensureTransition(AiSystemStatus.WITHDRAWN);
        if (reason == null || reason.isBlank()) {
            throw new AiSystemStateException("withdrawal reason required");
        }
        this.withdrawalReason = reason;
        this.status = AiSystemStatus.WITHDRAWN;
        this.effectiveTo = now;
        this.updatedAt = now;
    }

    public boolean isDraft()       { return status == AiSystemStatus.DRAFT; }
    public boolean isInUse()       { return status == AiSystemStatus.IN_USE; }
    public boolean isTerminal()    {
        return status == AiSystemStatus.DECOMMISSIONED || status == AiSystemStatus.WITHDRAWN;
    }

    private void validateNotProhibited(String action) {
        if (riskClassification.isProhibited()) {
            throw new AiSystemStateException(
                    "Cannot " + action + " an AI system classified UNACCEPTABLE "
                    + "(prohibited practice, AI Act Art. 5)");
        }
    }

    private void validateInUseInvariants() {
        if (riskClassification.requiresConformityAssessment()) {
            if (conformityAssessmentEvidenceUrl == null
                    || conformityAssessmentEvidenceUrl.isBlank()) {
                throw new AiSystemStateException(
                        "HIGH risk requires conformityAssessmentEvidenceUrl (Art. 43)");
            }
            if (humanOversightDescription == null || humanOversightDescription.isBlank()) {
                throw new AiSystemStateException(
                        "HIGH risk requires humanOversightDescription (Art. 14)");
            }
        }
        if (riskClassification.requiresTransparency()
                && (transparencyMeasures == null || transparencyMeasures.isBlank())) {
            throw new AiSystemStateException(
                    "Risk level " + riskClassification
                    + " requires transparencyMeasures (Art. 13 / 50)");
        }
    }

    private void ensureTransition(AiSystemStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new AiSystemStateException(
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
    private static String sanitizeUrl(String v) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        if (!URL_PATTERN.matcher(t).matches()) {
            throw new IllegalArgumentException(
                    "conformityAssessmentEvidenceUrl must be a valid http(s) URL");
        }
        return t;
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
    public String getProviderName() { return providerName; }
    public String getIntendedPurpose() { return intendedPurpose; }
    public AiRiskClassification getRiskClassification() { return riskClassification; }
    public AiSystemRole getRole() { return role; }
    public boolean isGeneralPurpose() { return generalPurpose; }
    public AiSystemStatus getStatus() { return status; }
    public String getConformityAssessmentEvidenceUrl() { return conformityAssessmentEvidenceUrl; }
    public String getCeMarkingNumber() { return ceMarkingNumber; }
    public String getHumanOversightDescription() { return humanOversightDescription; }
    public String getTransparencyMeasures() { return transparencyMeasures; }
    public String getDataGovernanceNotes() { return dataGovernanceNotes; }
    public UUID getLinkedDpiaId() { return linkedDpiaId; }
    public Set<UUID> getLinkedProcessingActivityIds() { return linkedProcessingActivityIds; }
    public Set<UUID> getLinkedAutomatedDecisionIds() { return linkedAutomatedDecisionIds; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public String getWithdrawalReason() { return withdrawalReason; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
