package com.openlab.qualitos.quality.crossbordertransfers.domain;

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
 * Agrégat — transfert international de données personnelles (RGPD Chapitre V,
 * Art. 44-49).
 *
 * Décrit un flux de données hors UE/EEE avec son mécanisme de garantie et les
 * pays destinataires. Plusieurs transferts peuvent coexister pour un même
 * traitement (RoPA) si plusieurs sous-traitants/destinataires sont impliqués.
 *
 * Cycle de vie :
 *   DRAFT → ACTIVE → SUSPENDED → ACTIVE (réactivation) → TERMINATED
 *   ACTIVE → TERMINATED (raccourci)
 *   DRAFT → (deletable)
 *
 * Garde-fous métier :
 *  - Activation exige : effectiveFrom, mechanism, destinationCountries non vides,
 *    safeguardsDescription non vide.
 *  - DEROGATION_ART49 ⇒ derogationJustification obligatoire (Art. 49 = exception,
 *    doit être motivée par une situation spécifique).
 *  - SUSPENDED ⇒ suspensionReason.
 *  - TERMINATED ⇒ terminationReason.
 *
 * Privacy by design (OWASP A02) : pas de PII concrète — l'agrégat décrit la
 * structure du flux (où, qui, comment), pas des individus.
 */
public final class CrossBorderTransfer {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");
    private static final Pattern COUNTRY_PATTERN = Pattern.compile("^[A-Z]{2}$");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("^[a-z][a-z0-9._-]{1,63}$");

    private static final Map<CrossBorderTransferStatus, Set<CrossBorderTransferStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(CrossBorderTransferStatus.class);
        ALLOWED.put(CrossBorderTransferStatus.DRAFT,
                EnumSet.of(CrossBorderTransferStatus.ACTIVE));
        ALLOWED.put(CrossBorderTransferStatus.ACTIVE,
                EnumSet.of(CrossBorderTransferStatus.SUSPENDED,
                        CrossBorderTransferStatus.TERMINATED));
        ALLOWED.put(CrossBorderTransferStatus.SUSPENDED,
                EnumSet.of(CrossBorderTransferStatus.ACTIVE,
                        CrossBorderTransferStatus.TERMINATED));
        ALLOWED.put(CrossBorderTransferStatus.TERMINATED,
                EnumSet.noneOf(CrossBorderTransferStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private String recipientName;
    private String recipientLegalEntity;
    private String recipientContact;
    private Set<String> destinationCountries;
    private TransferMechanism mechanism;
    private String safeguardsDescription;
    private String safeguardsDocumentUrl;
    private String derogationJustification;
    private Set<String> dataCategories;
    private Set<UUID> linkedProcessingActivityIds;
    private Set<UUID> linkedProcessorAgreementIds;
    private CrossBorderTransferStatus status;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private Instant suspendedAt;
    private String suspensionReason;
    private String terminationReason;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public CrossBorderTransfer(UUID id, UUID tenantId, String reference,
                               String recipientName, String recipientLegalEntity,
                               String recipientContact,
                               Set<String> destinationCountries,
                               TransferMechanism mechanism,
                               String safeguardsDescription, String safeguardsDocumentUrl,
                               String derogationJustification,
                               Set<String> dataCategories,
                               Set<UUID> linkedProcessingActivityIds,
                               Set<UUID> linkedProcessorAgreementIds,
                               CrossBorderTransferStatus status,
                               Instant effectiveFrom, Instant effectiveTo,
                               Instant suspendedAt, String suspensionReason,
                               String terminationReason,
                               UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.recipientName = requireText(recipientName, "recipientName", 250);
        this.recipientLegalEntity = recipientLegalEntity;
        this.recipientContact = recipientContact;
        this.destinationCountries = sanitizeCountries(destinationCountries);
        this.mechanism = Objects.requireNonNull(mechanism, "mechanism");
        this.safeguardsDescription = safeguardsDescription;
        this.safeguardsDocumentUrl = safeguardsDocumentUrl;
        this.derogationJustification = derogationJustification;
        this.dataCategories = sanitizeCodes(dataCategories);
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.linkedProcessorAgreementIds = sanitizeIds(linkedProcessorAgreementIds);
        this.status = Objects.requireNonNull(status, "status");
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.suspendedAt = suspendedAt;
        this.suspensionReason = suspensionReason;
        this.terminationReason = terminationReason;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    public static CrossBorderTransfer draft(UUID tenantId, String reference,
                                            String recipientName, String recipientLegalEntity,
                                            String recipientContact,
                                            Set<String> destinationCountries,
                                            TransferMechanism mechanism,
                                            String safeguardsDescription,
                                            String safeguardsDocumentUrl,
                                            String derogationJustification,
                                            Set<String> dataCategories,
                                            Set<UUID> linkedProcessingActivityIds,
                                            Set<UUID> linkedProcessorAgreementIds,
                                            UUID createdByUserId, Instant now) {
        return new CrossBorderTransfer(null, tenantId, reference,
                recipientName, recipientLegalEntity, recipientContact,
                destinationCountries, mechanism,
                safeguardsDescription, safeguardsDocumentUrl, derogationJustification,
                dataCategories, linkedProcessingActivityIds, linkedProcessorAgreementIds,
                CrossBorderTransferStatus.DRAFT, null, null,
                null, null, null,
                createdByUserId, now, now);
    }

    public void editDraft(String recipientName, String recipientLegalEntity,
                          String recipientContact,
                          Set<String> destinationCountries,
                          TransferMechanism mechanism,
                          String safeguardsDescription, String safeguardsDocumentUrl,
                          String derogationJustification,
                          Set<String> dataCategories,
                          Set<UUID> linkedProcessingActivityIds,
                          Set<UUID> linkedProcessorAgreementIds,
                          Instant now) {
        if (status != CrossBorderTransferStatus.DRAFT) {
            throw new CrossBorderTransferStateException("Only DRAFT transfers can be edited");
        }
        this.recipientName = requireText(recipientName, "recipientName", 250);
        this.recipientLegalEntity = recipientLegalEntity;
        this.recipientContact = recipientContact;
        this.destinationCountries = sanitizeCountries(destinationCountries);
        this.mechanism = Objects.requireNonNull(mechanism, "mechanism");
        this.safeguardsDescription = safeguardsDescription;
        this.safeguardsDocumentUrl = safeguardsDocumentUrl;
        this.derogationJustification = derogationJustification;
        this.dataCategories = sanitizeCodes(dataCategories);
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.linkedProcessorAgreementIds = sanitizeIds(linkedProcessorAgreementIds);
        this.updatedAt = now;
    }

    public void activate(Instant now) {
        ensureTransition(CrossBorderTransferStatus.ACTIVE);
        validateActivationInvariants();
        this.status = CrossBorderTransferStatus.ACTIVE;
        if (this.effectiveFrom == null) this.effectiveFrom = now;
        // Réactivation depuis SUSPENDED : on efface la suspension.
        this.suspendedAt = null;
        this.suspensionReason = null;
        this.updatedAt = now;
    }

    public void suspend(String reason, Instant now) {
        ensureTransition(CrossBorderTransferStatus.SUSPENDED);
        if (reason == null || reason.isBlank()) {
            throw new CrossBorderTransferStateException("suspension reason required");
        }
        this.suspensionReason = reason;
        this.suspendedAt = now;
        this.status = CrossBorderTransferStatus.SUSPENDED;
        this.updatedAt = now;
    }

    public void terminate(String reason, Instant now) {
        ensureTransition(CrossBorderTransferStatus.TERMINATED);
        if (reason == null || reason.isBlank()) {
            throw new CrossBorderTransferStateException("termination reason required");
        }
        this.terminationReason = reason;
        this.effectiveTo = now;
        this.status = CrossBorderTransferStatus.TERMINATED;
        this.updatedAt = now;
    }

    public boolean isDraft()     { return status == CrossBorderTransferStatus.DRAFT; }
    public boolean isActive()    { return status == CrossBorderTransferStatus.ACTIVE; }
    public boolean isSuspended() { return status == CrossBorderTransferStatus.SUSPENDED; }
    public boolean isTerminal()  { return status == CrossBorderTransferStatus.TERMINATED; }

    private void validateActivationInvariants() {
        if (destinationCountries.isEmpty()) {
            throw new CrossBorderTransferStateException(
                    "destinationCountries required to activate");
        }
        if (safeguardsDescription == null || safeguardsDescription.isBlank()) {
            throw new CrossBorderTransferStateException(
                    "safeguardsDescription required to activate (Art. 44)");
        }
        if (mechanism.requiresDerogationJustification()
                && (derogationJustification == null || derogationJustification.isBlank())) {
            throw new CrossBorderTransferStateException(
                    "DEROGATION_ART49 requires derogationJustification (Art. 49)");
        }
    }

    private void ensureTransition(CrossBorderTransferStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new CrossBorderTransferStateException(
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
    private static Set<String> sanitizeCountries(Set<String> input) {
        if (input == null) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        for (String c : input) {
            if (c == null) continue;
            String t = c.trim().toUpperCase();
            if (t.isEmpty()) continue;
            if (!COUNTRY_PATTERN.matcher(t).matches()) {
                throw new IllegalArgumentException(
                        "destinationCountries: '" + t + "' must be ISO 3166-1 alpha-2");
            }
            out.add(t);
        }
        return Collections.unmodifiableSet(out);
    }
    private static Set<String> sanitizeCodes(Set<String> input) {
        if (input == null) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        for (String c : input) {
            if (c == null) continue;
            String t = c.trim();
            if (t.isEmpty()) continue;
            if (!CATEGORY_PATTERN.matcher(t).matches()) {
                throw new IllegalArgumentException(
                        "dataCategories: '" + t + "' must match [a-z][a-z0-9._-]{1,63}");
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
    public String getRecipientName() { return recipientName; }
    public String getRecipientLegalEntity() { return recipientLegalEntity; }
    public String getRecipientContact() { return recipientContact; }
    public Set<String> getDestinationCountries() { return destinationCountries; }
    public TransferMechanism getMechanism() { return mechanism; }
    public String getSafeguardsDescription() { return safeguardsDescription; }
    public String getSafeguardsDocumentUrl() { return safeguardsDocumentUrl; }
    public String getDerogationJustification() { return derogationJustification; }
    public Set<String> getDataCategories() { return dataCategories; }
    public Set<UUID> getLinkedProcessingActivityIds() { return linkedProcessingActivityIds; }
    public Set<UUID> getLinkedProcessorAgreementIds() { return linkedProcessorAgreementIds; }
    public CrossBorderTransferStatus getStatus() { return status; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public Instant getSuspendedAt() { return suspendedAt; }
    public String getSuspensionReason() { return suspensionReason; }
    public String getTerminationReason() { return terminationReason; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
