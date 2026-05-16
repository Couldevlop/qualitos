package com.openlab.qualitos.quality.processoragreements.domain;

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
 * Agrégat — accord de sous-traitance (DPA — RGPD Art. 28).
 *
 * Couvre les exigences minimales Art. 28§3 :
 *  (a) objet/durée/nature/finalité du traitement,
 *  (b) catégories de personnes concernées et types de données (via lien RoPA),
 *  (c) obligations du sous-traitant : confidentialité, sécurité (Art. 32),
 *      notification de violation, sous-traitants ultérieurs, audits, retour
 *      ou destruction des données en fin de contrat.
 *
 * Cycle de vie :
 *   DRAFT → ACTIVE (activate, exige signedAt + effectiveFrom)
 *   ACTIVE → TERMINATED (terminate explicite)
 *   ACTIVE → EXPIRED (expireIfDue automatique au-delà d'expirationDate)
 *   DRAFT → (deletable)
 *
 * Garde-fous (validés à chaque mutation + activation) :
 *  - thirdCountryTransfers non vides ⇒ transferSafeguards (Art. 44-49) requis
 *  - activation requiert processorContact, signedAt, effectiveFrom
 *  - breachNotificationCommitmentHours borné [1, 720] (max 30 jours)
 *
 * Privacy by design (OWASP A02) : aucune PII concernée ne transite par cet
 * agrégat — les informations concernent le sous-traitant (entreprise) et les
 * obligations contractuelles, pas les personnes concernées (référencées via
 * les UUID d'activités RoPA).
 */
public final class ProcessorAgreement {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");
    private static final Pattern COUNTRY_PATTERN = Pattern.compile("^[A-Z]{2}$");
    public static final int MIN_BREACH_HOURS = 1;
    public static final int MAX_BREACH_HOURS = 720;

    private static final Map<ProcessorAgreementStatus, Set<ProcessorAgreementStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(ProcessorAgreementStatus.class);
        ALLOWED.put(ProcessorAgreementStatus.DRAFT,
                EnumSet.of(ProcessorAgreementStatus.ACTIVE));
        ALLOWED.put(ProcessorAgreementStatus.ACTIVE,
                EnumSet.of(ProcessorAgreementStatus.TERMINATED,
                        ProcessorAgreementStatus.EXPIRED));
        ALLOWED.put(ProcessorAgreementStatus.TERMINATED,
                EnumSet.noneOf(ProcessorAgreementStatus.class));
        ALLOWED.put(ProcessorAgreementStatus.EXPIRED,
                EnumSet.noneOf(ProcessorAgreementStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private String processorName;
    private String processorLegalEntity;
    private String processorContact;
    private String processorDpoContact;
    private String processorCountry;
    private String servicesDescription;
    private Set<String> subProcessorCategories;
    private Set<UUID> linkedProcessingActivityIds;
    private Set<String> thirdCountryTransfers;
    private String transferSafeguards;
    private String contractDocumentUrl;
    private Instant signedAt;
    private Instant effectiveFrom;
    private Instant expirationDate;
    private String securityMeasures;
    private int breachNotificationCommitmentHours;
    private boolean auditRights;
    private String auditRightsNotes;
    private String dataReturnOrDeletionTerms;
    private ProcessorAgreementStatus status;
    private Instant terminatedAt;
    private String terminationReason;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public ProcessorAgreement(UUID id, UUID tenantId, String reference,
                              String processorName, String processorLegalEntity,
                              String processorContact, String processorDpoContact,
                              String processorCountry, String servicesDescription,
                              Set<String> subProcessorCategories,
                              Set<UUID> linkedProcessingActivityIds,
                              Set<String> thirdCountryTransfers, String transferSafeguards,
                              String contractDocumentUrl,
                              Instant signedAt, Instant effectiveFrom, Instant expirationDate,
                              String securityMeasures, int breachNotificationCommitmentHours,
                              boolean auditRights, String auditRightsNotes,
                              String dataReturnOrDeletionTerms,
                              ProcessorAgreementStatus status,
                              Instant terminatedAt, String terminationReason,
                              UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.processorName = requireText(processorName, "processorName", 250);
        this.processorLegalEntity = processorLegalEntity;
        this.processorContact = processorContact;
        this.processorDpoContact = processorDpoContact;
        this.processorCountry = sanitizeCountry(processorCountry);
        this.servicesDescription = requireText(servicesDescription, "servicesDescription", 4000);
        this.subProcessorCategories = sanitizeCodes(subProcessorCategories);
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.thirdCountryTransfers = sanitizeCountries(thirdCountryTransfers);
        this.transferSafeguards = transferSafeguards;
        this.contractDocumentUrl = contractDocumentUrl;
        this.signedAt = signedAt;
        this.effectiveFrom = effectiveFrom;
        this.expirationDate = expirationDate;
        this.securityMeasures = securityMeasures;
        this.breachNotificationCommitmentHours = requireBreachHours(breachNotificationCommitmentHours);
        this.auditRights = auditRights;
        this.auditRightsNotes = auditRightsNotes;
        this.dataReturnOrDeletionTerms = dataReturnOrDeletionTerms;
        this.status = Objects.requireNonNull(status, "status");
        this.terminatedAt = terminatedAt;
        this.terminationReason = terminationReason;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
        validateTransferInvariant();
    }

    /** Factory — création en état DRAFT. */
    public static ProcessorAgreement draft(UUID tenantId, String reference,
                                           String processorName, String processorLegalEntity,
                                           String processorContact, String processorDpoContact,
                                           String processorCountry, String servicesDescription,
                                           Set<String> subProcessorCategories,
                                           Set<UUID> linkedProcessingActivityIds,
                                           Set<String> thirdCountryTransfers,
                                           String transferSafeguards,
                                           String contractDocumentUrl,
                                           Instant signedAt, Instant effectiveFrom,
                                           Instant expirationDate,
                                           String securityMeasures,
                                           int breachNotificationCommitmentHours,
                                           boolean auditRights, String auditRightsNotes,
                                           String dataReturnOrDeletionTerms,
                                           UUID createdByUserId, Instant now) {
        return new ProcessorAgreement(null, tenantId, reference,
                processorName, processorLegalEntity,
                processorContact, processorDpoContact, processorCountry,
                servicesDescription, subProcessorCategories, linkedProcessingActivityIds,
                thirdCountryTransfers, transferSafeguards, contractDocumentUrl,
                signedAt, effectiveFrom, expirationDate,
                securityMeasures, breachNotificationCommitmentHours,
                auditRights, auditRightsNotes, dataReturnOrDeletionTerms,
                ProcessorAgreementStatus.DRAFT, null, null,
                createdByUserId, now, now);
    }

    /** Édition — DRAFT uniquement. */
    public void editDraft(String processorName, String processorLegalEntity,
                          String processorContact, String processorDpoContact,
                          String processorCountry, String servicesDescription,
                          Set<String> subProcessorCategories,
                          Set<UUID> linkedProcessingActivityIds,
                          Set<String> thirdCountryTransfers, String transferSafeguards,
                          String contractDocumentUrl,
                          Instant signedAt, Instant effectiveFrom, Instant expirationDate,
                          String securityMeasures, int breachNotificationCommitmentHours,
                          boolean auditRights, String auditRightsNotes,
                          String dataReturnOrDeletionTerms, Instant now) {
        if (status != ProcessorAgreementStatus.DRAFT) {
            throw new ProcessorAgreementStateException("Only DRAFT agreements can be edited");
        }
        this.processorName = requireText(processorName, "processorName", 250);
        this.processorLegalEntity = processorLegalEntity;
        this.processorContact = processorContact;
        this.processorDpoContact = processorDpoContact;
        this.processorCountry = sanitizeCountry(processorCountry);
        this.servicesDescription = requireText(servicesDescription, "servicesDescription", 4000);
        this.subProcessorCategories = sanitizeCodes(subProcessorCategories);
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.thirdCountryTransfers = sanitizeCountries(thirdCountryTransfers);
        this.transferSafeguards = transferSafeguards;
        this.contractDocumentUrl = contractDocumentUrl;
        this.signedAt = signedAt;
        this.effectiveFrom = effectiveFrom;
        this.expirationDate = expirationDate;
        this.securityMeasures = securityMeasures;
        this.breachNotificationCommitmentHours = requireBreachHours(breachNotificationCommitmentHours);
        this.auditRights = auditRights;
        this.auditRightsNotes = auditRightsNotes;
        this.dataReturnOrDeletionTerms = dataReturnOrDeletionTerms;
        this.updatedAt = now;
        validateTransferInvariant();
    }

    /** Activation — exige signature + entrée en vigueur + contact processeur. */
    public void activate(Instant now) {
        ensureTransition(ProcessorAgreementStatus.ACTIVE);
        if (signedAt == null) {
            throw new ProcessorAgreementStateException("signedAt required to activate");
        }
        if (effectiveFrom == null) {
            throw new ProcessorAgreementStateException("effectiveFrom required to activate");
        }
        if (processorContact == null || processorContact.isBlank()) {
            throw new ProcessorAgreementStateException("processorContact required to activate");
        }
        validateTransferInvariant();
        this.status = ProcessorAgreementStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void terminate(String reason, Instant now) {
        ensureTransition(ProcessorAgreementStatus.TERMINATED);
        if (reason == null || reason.isBlank()) {
            throw new ProcessorAgreementStateException("termination reason required");
        }
        this.terminationReason = reason;
        this.terminatedAt = now;
        this.status = ProcessorAgreementStatus.TERMINATED;
        this.updatedAt = now;
    }

    /** Auto-expiration si {@code expirationDate} dépassée. No-op sinon. */
    public void expireIfDue(Instant now) {
        if (status == ProcessorAgreementStatus.ACTIVE
                && expirationDate != null && !now.isBefore(expirationDate)) {
            this.status = ProcessorAgreementStatus.EXPIRED;
            this.terminatedAt = now;
            this.updatedAt = now;
        }
    }

    public boolean isDraft()    { return status == ProcessorAgreementStatus.DRAFT; }
    public boolean isActive()   { return status == ProcessorAgreementStatus.ACTIVE; }
    public boolean isTerminal() {
        return status == ProcessorAgreementStatus.TERMINATED
                || status == ProcessorAgreementStatus.EXPIRED;
    }
    public boolean isExpirable(Instant ref) {
        return isActive() && expirationDate != null && !ref.isBefore(expirationDate);
    }

    private void validateTransferInvariant() {
        if (!thirdCountryTransfers.isEmpty()
                && (transferSafeguards == null || transferSafeguards.isBlank())) {
            throw new ProcessorAgreementStateException(
                    "thirdCountryTransfers requires transferSafeguards (Art. 44-49)");
        }
    }

    private void ensureTransition(ProcessorAgreementStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new ProcessorAgreementStateException(
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
    private static int requireBreachHours(int h) {
        if (h < MIN_BREACH_HOURS || h > MAX_BREACH_HOURS) {
            throw new IllegalArgumentException(
                    "breachNotificationCommitmentHours must be in [1, 720]");
        }
        return h;
    }
    private static String sanitizeCountry(String v) {
        if (v == null) return null;
        String t = v.trim().toUpperCase();
        if (t.isEmpty()) return null;
        if (!COUNTRY_PATTERN.matcher(t).matches()) {
            throw new IllegalArgumentException(
                    "processorCountry must be ISO 3166-1 alpha-2");
        }
        return t;
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
                        "thirdCountryTransfers: '" + t + "' must be ISO 3166-1 alpha-2");
            }
            out.add(t);
        }
        return Collections.unmodifiableSet(out);
    }
    private static Set<String> sanitizeCodes(Set<String> input) {
        if (input == null) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        Pattern code = Pattern.compile("^[a-z][a-z0-9._-]{1,63}$");
        for (String c : input) {
            if (c == null) continue;
            String t = c.trim();
            if (t.isEmpty()) continue;
            if (!code.matcher(t).matches()) {
                throw new IllegalArgumentException(
                        "subProcessorCategories: '" + t + "' must match [a-z][a-z0-9._-]{1,63}");
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
    public String getProcessorName() { return processorName; }
    public String getProcessorLegalEntity() { return processorLegalEntity; }
    public String getProcessorContact() { return processorContact; }
    public String getProcessorDpoContact() { return processorDpoContact; }
    public String getProcessorCountry() { return processorCountry; }
    public String getServicesDescription() { return servicesDescription; }
    public Set<String> getSubProcessorCategories() { return subProcessorCategories; }
    public Set<UUID> getLinkedProcessingActivityIds() { return linkedProcessingActivityIds; }
    public Set<String> getThirdCountryTransfers() { return thirdCountryTransfers; }
    public String getTransferSafeguards() { return transferSafeguards; }
    public String getContractDocumentUrl() { return contractDocumentUrl; }
    public Instant getSignedAt() { return signedAt; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getExpirationDate() { return expirationDate; }
    public String getSecurityMeasures() { return securityMeasures; }
    public int getBreachNotificationCommitmentHours() { return breachNotificationCommitmentHours; }
    public boolean isAuditRights() { return auditRights; }
    public String getAuditRightsNotes() { return auditRightsNotes; }
    public String getDataReturnOrDeletionTerms() { return dataReturnOrDeletionTerms; }
    public ProcessorAgreementStatus getStatus() { return status; }
    public Instant getTerminatedAt() { return terminatedAt; }
    public String getTerminationReason() { return terminationReason; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
