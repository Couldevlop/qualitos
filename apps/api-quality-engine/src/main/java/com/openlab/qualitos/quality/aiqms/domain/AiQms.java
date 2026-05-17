package com.openlab.qualitos.quality.aiqms.domain;

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
 * Agrégat — système de management de la qualité IA (AI Act Art. 17).
 *
 * Les fournisseurs de systèmes IA HIGH-risk doivent disposer d'un QMS
 * documenté couvrant : stratégie de conformité réglementaire, conception &
 * développement, contrôle qualité, gestion des données, gestion des risques,
 * post-market monitoring, communication avec les régulateurs, gestion des
 * ressources, surveillance des fournisseurs, traçabilité, gestion des
 * incidents, audits internes.
 *
 * Cycle de vie :
 *   DRAFT → APPROVED → IN_FORCE → SUPERSEDED
 *   DRAFT|APPROVED → ARCHIVED
 *
 * Garde-fous (dupliqués DB) :
 *  - approval requires regulatoryComplianceStrategy + designControlDescription
 *    + qualityControlDescription + dataManagementDescription
 *    + riskManagementDescription + pmmDescription + regulatorCommunicationDescription
 *  - APPROVED/IN_FORCE/SUPERSEDED require approver + approvalDate
 *  - segregation of duties : approver ≠ submitter
 *  - SUPERSEDED requires supersededBy
 *  - effective_from/to invariants
 */
public final class AiQms {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+(\\.\\d+)?$");

    private static final Map<AiQmsStatus, Set<AiQmsStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(AiQmsStatus.class);
        ALLOWED.put(AiQmsStatus.DRAFT,
                EnumSet.of(AiQmsStatus.APPROVED, AiQmsStatus.ARCHIVED));
        ALLOWED.put(AiQmsStatus.APPROVED,
                EnumSet.of(AiQmsStatus.IN_FORCE, AiQmsStatus.ARCHIVED));
        ALLOWED.put(AiQmsStatus.IN_FORCE,
                EnumSet.of(AiQmsStatus.SUPERSEDED, AiQmsStatus.ARCHIVED));
        ALLOWED.put(AiQmsStatus.SUPERSEDED, EnumSet.noneOf(AiQmsStatus.class));
        ALLOWED.put(AiQmsStatus.ARCHIVED,   EnumSet.noneOf(AiQmsStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private final String version;
    private String name;
    private String description;
    private String regulatoryComplianceStrategy;
    private String designControlDescription;
    private String qualityControlDescription;
    private String dataManagementDescription;
    private String riskManagementDescription;
    private String pmmDescription;
    private String regulatorCommunicationDescription;
    private String resourceManagementDescription;
    private String supplierMonitoringDescription;
    private Set<UUID> coveredAiSystemIds;
    private AiQmsStatus status;
    private Instant submittedAt;
    private UUID submittedByUserId;
    private Instant approvedAt;
    private UUID approvedByUserId;
    private String approvalNotes;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private UUID supersededByQmsId;
    private String archivedReason;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public AiQms(UUID id, UUID tenantId, String reference, String version,
                 String name, String description,
                 String regulatoryComplianceStrategy, String designControlDescription,
                 String qualityControlDescription, String dataManagementDescription,
                 String riskManagementDescription, String pmmDescription,
                 String regulatorCommunicationDescription,
                 String resourceManagementDescription, String supplierMonitoringDescription,
                 Set<UUID> coveredAiSystemIds,
                 AiQmsStatus status,
                 Instant submittedAt, UUID submittedByUserId,
                 Instant approvedAt, UUID approvedByUserId, String approvalNotes,
                 Instant effectiveFrom, Instant effectiveTo,
                 UUID supersededByQmsId, String archivedReason,
                 UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.version = requireVersion(version);
        this.name = requireText(name, "name", 250);
        this.description = description;
        this.regulatoryComplianceStrategy = regulatoryComplianceStrategy;
        this.designControlDescription = designControlDescription;
        this.qualityControlDescription = qualityControlDescription;
        this.dataManagementDescription = dataManagementDescription;
        this.riskManagementDescription = riskManagementDescription;
        this.pmmDescription = pmmDescription;
        this.regulatorCommunicationDescription = regulatorCommunicationDescription;
        this.resourceManagementDescription = resourceManagementDescription;
        this.supplierMonitoringDescription = supplierMonitoringDescription;
        this.coveredAiSystemIds = sanitizeIds(coveredAiSystemIds);
        this.status = Objects.requireNonNull(status, "status");
        this.submittedAt = submittedAt;
        this.submittedByUserId = submittedByUserId;
        this.approvedAt = approvedAt;
        this.approvedByUserId = approvedByUserId;
        this.approvalNotes = approvalNotes;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.supersededByQmsId = supersededByQmsId;
        this.archivedReason = archivedReason;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    public static AiQms draft(UUID tenantId, String reference, String version,
                              String name, String description,
                              String regulatoryComplianceStrategy, String designControlDescription,
                              String qualityControlDescription, String dataManagementDescription,
                              String riskManagementDescription, String pmmDescription,
                              String regulatorCommunicationDescription,
                              String resourceManagementDescription,
                              String supplierMonitoringDescription,
                              Set<UUID> coveredAiSystemIds,
                              UUID createdByUserId, Instant now) {
        return new AiQms(null, tenantId, reference, version, name, description,
                regulatoryComplianceStrategy, designControlDescription,
                qualityControlDescription, dataManagementDescription,
                riskManagementDescription, pmmDescription, regulatorCommunicationDescription,
                resourceManagementDescription, supplierMonitoringDescription,
                coveredAiSystemIds, AiQmsStatus.DRAFT,
                null, null, null, null, null, null, null, null, null,
                createdByUserId, now, now);
    }

    public void editDraft(String name, String description,
                          String regulatoryComplianceStrategy, String designControlDescription,
                          String qualityControlDescription, String dataManagementDescription,
                          String riskManagementDescription, String pmmDescription,
                          String regulatorCommunicationDescription,
                          String resourceManagementDescription,
                          String supplierMonitoringDescription,
                          Set<UUID> coveredAiSystemIds, Instant now) {
        if (status != AiQmsStatus.DRAFT) {
            throw new AiQmsStateException("Only DRAFT QMS can be edited");
        }
        this.name = requireText(name, "name", 250);
        this.description = description;
        this.regulatoryComplianceStrategy = regulatoryComplianceStrategy;
        this.designControlDescription = designControlDescription;
        this.qualityControlDescription = qualityControlDescription;
        this.dataManagementDescription = dataManagementDescription;
        this.riskManagementDescription = riskManagementDescription;
        this.pmmDescription = pmmDescription;
        this.regulatorCommunicationDescription = regulatorCommunicationDescription;
        this.resourceManagementDescription = resourceManagementDescription;
        this.supplierMonitoringDescription = supplierMonitoringDescription;
        this.coveredAiSystemIds = sanitizeIds(coveredAiSystemIds);
        this.updatedAt = now;
    }

    public void approve(UUID submitterId, UUID approverId, String notes, Instant now) {
        ensureTransition(AiQmsStatus.APPROVED);
        Objects.requireNonNull(submitterId, "submitterId");
        Objects.requireNonNull(approverId, "approverId");
        if (submitterId.equals(approverId)) {
            throw new AiQmsStateException(
                    "Approver must differ from submitter (segregation of duties)");
        }
        validateMandatoryDescriptions();
        this.submittedByUserId = submitterId;
        this.submittedAt = now;
        this.approvedByUserId = approverId;
        this.approvedAt = now;
        this.approvalNotes = notes;
        this.status = AiQmsStatus.APPROVED;
        this.updatedAt = now;
    }

    public void putInForce(Instant now) {
        ensureTransition(AiQmsStatus.IN_FORCE);
        this.status = AiQmsStatus.IN_FORCE;
        this.effectiveFrom = now;
        this.updatedAt = now;
    }

    public void supersede(UUID newQmsId, Instant now) {
        ensureTransition(AiQmsStatus.SUPERSEDED);
        Objects.requireNonNull(newQmsId, "supersededByQmsId");
        if (newQmsId.equals(this.id)) {
            throw new AiQmsStateException("supersededBy must reference a different QMS");
        }
        this.supersededByQmsId = newQmsId;
        this.effectiveTo = now;
        this.status = AiQmsStatus.SUPERSEDED;
        this.updatedAt = now;
    }

    public void archive(String reason, Instant now) {
        ensureTransition(AiQmsStatus.ARCHIVED);
        if (reason == null || reason.isBlank()) {
            throw new AiQmsStateException("archive reason required");
        }
        this.archivedReason = reason;
        this.effectiveTo = now;
        this.status = AiQmsStatus.ARCHIVED;
        this.updatedAt = now;
    }

    public boolean isDraft()      { return status == AiQmsStatus.DRAFT; }
    public boolean isInForce()    { return status == AiQmsStatus.IN_FORCE; }
    public boolean isSuperseded() { return status == AiQmsStatus.SUPERSEDED; }
    public boolean isArchived()   { return status == AiQmsStatus.ARCHIVED; }

    private void validateMandatoryDescriptions() {
        requireNonBlank(regulatoryComplianceStrategy, "regulatoryComplianceStrategy");
        requireNonBlank(designControlDescription, "designControlDescription");
        requireNonBlank(qualityControlDescription, "qualityControlDescription");
        requireNonBlank(dataManagementDescription, "dataManagementDescription");
        requireNonBlank(riskManagementDescription, "riskManagementDescription");
        requireNonBlank(pmmDescription, "pmmDescription");
        requireNonBlank(regulatorCommunicationDescription, "regulatorCommunicationDescription");
    }

    private static void requireNonBlank(String v, String f) {
        if (v == null || v.isBlank()) {
            throw new AiQmsStateException(f + " required for approval");
        }
    }

    private void ensureTransition(AiQmsStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new AiQmsStateException(
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
    private static String requireVersion(String v) {
        if (v == null || !VERSION_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException(
                    "version must match X.Y or X.Y.Z");
        }
        return v;
    }
    private static String requireText(String v, String f, int maxLen) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " required");
        if (v.length() > maxLen) throw new IllegalArgumentException(
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
    public String getVersion() { return version; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getRegulatoryComplianceStrategy() { return regulatoryComplianceStrategy; }
    public String getDesignControlDescription() { return designControlDescription; }
    public String getQualityControlDescription() { return qualityControlDescription; }
    public String getDataManagementDescription() { return dataManagementDescription; }
    public String getRiskManagementDescription() { return riskManagementDescription; }
    public String getPmmDescription() { return pmmDescription; }
    public String getRegulatorCommunicationDescription() { return regulatorCommunicationDescription; }
    public String getResourceManagementDescription() { return resourceManagementDescription; }
    public String getSupplierMonitoringDescription() { return supplierMonitoringDescription; }
    public Set<UUID> getCoveredAiSystemIds() { return coveredAiSystemIds; }
    public AiQmsStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public UUID getSubmittedByUserId() { return submittedByUserId; }
    public Instant getApprovedAt() { return approvedAt; }
    public UUID getApprovedByUserId() { return approvedByUserId; }
    public String getApprovalNotes() { return approvalNotes; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public UUID getSupersededByQmsId() { return supersededByQmsId; }
    public String getArchivedReason() { return archivedReason; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
