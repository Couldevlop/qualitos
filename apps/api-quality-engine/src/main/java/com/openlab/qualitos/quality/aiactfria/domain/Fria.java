package com.openlab.qualitos.quality.aiactfria.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat — Fundamental Rights Impact Assessment (AI Act, Art. 27).
 *
 * Les déployeurs d'un système d'IA HIGH risk doivent réaliser une FRIA avant
 * première utilisation, décrivant : le processus, la durée d'usage, les
 * catégories de personnes concernées, les risques spécifiques aux droits
 * fondamentaux, les mesures de mitigation, les mesures de supervision
 * humaine, et le mécanisme de plainte interne.
 *
 * Cycle de vie : DRAFT → SUBMITTED → APPROVED → ARCHIVED
 *                DRAFT|SUBMITTED → DRAFT (renvoi)
 *
 * Garde-fous (dupliqués DB) :
 *  - approval requires mitigationMeasures + humanOversightMeasures
 *  - archived requires effective_to
 *  - reference must be canonical
 */
public final class Fria {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");

    private static final Map<FriaStatus, Set<FriaStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(FriaStatus.class);
        ALLOWED.put(FriaStatus.DRAFT,     EnumSet.of(FriaStatus.SUBMITTED));
        ALLOWED.put(FriaStatus.SUBMITTED, EnumSet.of(FriaStatus.APPROVED, FriaStatus.DRAFT));
        ALLOWED.put(FriaStatus.APPROVED,  EnumSet.of(FriaStatus.ARCHIVED));
        ALLOWED.put(FriaStatus.ARCHIVED,  EnumSet.noneOf(FriaStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private final UUID aiSystemId;
    private String processDescription;
    private String deploymentDurationDescription;
    private String affectedPersonsCategories;
    private String specificRisks;
    private String mitigationMeasures;
    private String humanOversightMeasures;
    private String complaintMechanismDescription;
    private FriaStatus status;
    private Instant submittedAt;
    private UUID submittedByUserId;
    private Instant approvedAt;
    private UUID approvedByUserId;
    private String approvalNotes;
    private Instant effectiveTo;
    private String archivedReason;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public Fria(UUID id, UUID tenantId, String reference, UUID aiSystemId,
                String processDescription, String deploymentDurationDescription,
                String affectedPersonsCategories, String specificRisks,
                String mitigationMeasures, String humanOversightMeasures,
                String complaintMechanismDescription,
                FriaStatus status,
                Instant submittedAt, UUID submittedByUserId,
                Instant approvedAt, UUID approvedByUserId, String approvalNotes,
                Instant effectiveTo, String archivedReason,
                UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.aiSystemId = Objects.requireNonNull(aiSystemId, "aiSystemId");
        this.processDescription = requireText(processDescription, "processDescription", 4000);
        this.deploymentDurationDescription = deploymentDurationDescription;
        this.affectedPersonsCategories = requireText(
                affectedPersonsCategories, "affectedPersonsCategories", 4000);
        this.specificRisks = requireText(specificRisks, "specificRisks", 4000);
        this.mitigationMeasures = mitigationMeasures;
        this.humanOversightMeasures = humanOversightMeasures;
        this.complaintMechanismDescription = complaintMechanismDescription;
        this.status = Objects.requireNonNull(status, "status");
        this.submittedAt = submittedAt;
        this.submittedByUserId = submittedByUserId;
        this.approvedAt = approvedAt;
        this.approvedByUserId = approvedByUserId;
        this.approvalNotes = approvalNotes;
        this.effectiveTo = effectiveTo;
        this.archivedReason = archivedReason;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    public static Fria draft(UUID tenantId, String reference, UUID aiSystemId,
                             String processDescription, String deploymentDurationDescription,
                             String affectedPersonsCategories, String specificRisks,
                             String mitigationMeasures, String humanOversightMeasures,
                             String complaintMechanismDescription,
                             UUID createdByUserId, Instant now) {
        return new Fria(null, tenantId, reference, aiSystemId,
                processDescription, deploymentDurationDescription,
                affectedPersonsCategories, specificRisks,
                mitigationMeasures, humanOversightMeasures, complaintMechanismDescription,
                FriaStatus.DRAFT,
                null, null, null, null, null, null, null,
                createdByUserId, now, now);
    }

    public void editDraft(String processDescription, String deploymentDurationDescription,
                          String affectedPersonsCategories, String specificRisks,
                          String mitigationMeasures, String humanOversightMeasures,
                          String complaintMechanismDescription, Instant now) {
        if (status != FriaStatus.DRAFT) {
            throw new FriaStateException("Only DRAFT FRIA can be edited");
        }
        this.processDescription = requireText(processDescription, "processDescription", 4000);
        this.deploymentDurationDescription = deploymentDurationDescription;
        this.affectedPersonsCategories = requireText(
                affectedPersonsCategories, "affectedPersonsCategories", 4000);
        this.specificRisks = requireText(specificRisks, "specificRisks", 4000);
        this.mitigationMeasures = mitigationMeasures;
        this.humanOversightMeasures = humanOversightMeasures;
        this.complaintMechanismDescription = complaintMechanismDescription;
        this.updatedAt = now;
    }

    public void submit(UUID submitterId, Instant now) {
        ensureTransition(FriaStatus.SUBMITTED);
        Objects.requireNonNull(submitterId, "submittedByUserId");
        if (mitigationMeasures == null || mitigationMeasures.isBlank()) {
            throw new FriaStateException("mitigationMeasures required before submission");
        }
        if (humanOversightMeasures == null || humanOversightMeasures.isBlank()) {
            throw new FriaStateException("humanOversightMeasures required before submission");
        }
        this.status = FriaStatus.SUBMITTED;
        this.submittedAt = now;
        this.submittedByUserId = submitterId;
        this.updatedAt = now;
    }

    public void approve(UUID approverId, String notes, Instant now) {
        ensureTransition(FriaStatus.APPROVED);
        Objects.requireNonNull(approverId, "approvedByUserId");
        if (submittedByUserId != null && submittedByUserId.equals(approverId)) {
            throw new FriaStateException(
                    "Approver must differ from submitter (segregation of duties)");
        }
        this.status = FriaStatus.APPROVED;
        this.approvedAt = now;
        this.approvedByUserId = approverId;
        this.approvalNotes = notes;
        this.updatedAt = now;
    }

    public void returnToDraft(String reason, Instant now) {
        if (status != FriaStatus.SUBMITTED) {
            throw new FriaStateException("Only SUBMITTED FRIA can be returned to draft");
        }
        if (reason == null || reason.isBlank()) {
            throw new FriaStateException("return reason required");
        }
        this.status = FriaStatus.DRAFT;
        this.approvalNotes = reason;
        this.updatedAt = now;
    }

    public void archive(String reason, Instant now) {
        ensureTransition(FriaStatus.ARCHIVED);
        if (reason == null || reason.isBlank()) {
            throw new FriaStateException("archive reason required");
        }
        this.status = FriaStatus.ARCHIVED;
        this.effectiveTo = now;
        this.archivedReason = reason;
        this.updatedAt = now;
    }

    public boolean isDraft()    { return status == FriaStatus.DRAFT; }
    public boolean isApproved() { return status == FriaStatus.APPROVED; }
    public boolean isArchived() { return status == FriaStatus.ARCHIVED; }

    private void ensureTransition(FriaStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new FriaStateException(
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

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getReference() { return reference; }
    public UUID getAiSystemId() { return aiSystemId; }
    public String getProcessDescription() { return processDescription; }
    public String getDeploymentDurationDescription() { return deploymentDurationDescription; }
    public String getAffectedPersonsCategories() { return affectedPersonsCategories; }
    public String getSpecificRisks() { return specificRisks; }
    public String getMitigationMeasures() { return mitigationMeasures; }
    public String getHumanOversightMeasures() { return humanOversightMeasures; }
    public String getComplaintMechanismDescription() { return complaintMechanismDescription; }
    public FriaStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public UUID getSubmittedByUserId() { return submittedByUserId; }
    public Instant getApprovedAt() { return approvedAt; }
    public UUID getApprovedByUserId() { return approvedByUserId; }
    public String getApprovalNotes() { return approvalNotes; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public String getArchivedReason() { return archivedReason; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
