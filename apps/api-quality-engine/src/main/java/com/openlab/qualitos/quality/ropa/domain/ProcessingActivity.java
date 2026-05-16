package com.openlab.qualitos.quality.ropa.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat — activité de traitement de données personnelles (RGPD Art. 30§1).
 *
 * Une activité de traitement décrit, pour un tenant, l'ensemble des finalités,
 * bases légales, catégories de données et de personnes concernées, destinataires,
 * transferts hors UE et mesures de sécurité associées à un traitement opéré.
 *
 * Cycle de vie :
 *   DRAFT  → ACTIVE  (activate) → ARCHIVED (archive)
 *   DRAFT  → (deletable)
 *
 * Invariants :
 *  - DRAFT : éditable.
 *  - ACTIVE / ARCHIVED : immutable (la mise à jour requiert l'archivage et la
 *    création d'une nouvelle DRAFT — préserve la traçabilité historique).
 *  - lawfulBasis obligatoire ; si LEGITIMATE_INTERESTS, lawfulBasisDetails
 *    (LIA — Legitimate Interests Assessment) doit être renseigné.
 *  - Si specialCategoriesProcessed (Art. 9), specialCategoriesJustification
 *    doit documenter la dérogation Art. 9§2.
 *  - Si transferts hors UE, transferSafeguards doit décrire les garanties
 *    (BCR / SCC / décision d'adéquation, Art. 44-49).
 *
 * Privacy by design : les "categories" sont des CODES (regex strict), pas des
 * identifiants individuels — aucun PII concret ne doit transiter par cet
 * agrégat (OWASP A02).
 */
public final class ProcessingActivity {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9._-]{1,63}$");
    private static final Pattern COUNTRY_PATTERN = Pattern.compile("^[A-Z]{2}$"); // ISO 3166-1 alpha-2

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private String name;
    private String purposes;
    private LawfulBasis lawfulBasis;
    private String lawfulBasisDetails;
    private String controllerName;
    private String controllerContact;
    private String dpoContact;
    private String jointControllerName;
    private String jointControllerContact;
    private Set<String> dataSubjectCategories;
    private Set<String> dataCategories;
    private boolean specialCategoriesProcessed;
    private String specialCategoriesJustification;
    private Set<String> recipientCategories;
    private Set<String> thirdCountryTransfers;
    private String transferSafeguards;
    private Set<UUID> linkedRetentionRuleIds;
    private String technicalMeasures;
    private String organizationalMeasures;
    private ProcessingActivityStatus status;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public ProcessingActivity(UUID id, UUID tenantId, String reference, String name,
                              String purposes, LawfulBasis lawfulBasis, String lawfulBasisDetails,
                              String controllerName, String controllerContact, String dpoContact,
                              String jointControllerName, String jointControllerContact,
                              Set<String> dataSubjectCategories, Set<String> dataCategories,
                              boolean specialCategoriesProcessed, String specialCategoriesJustification,
                              Set<String> recipientCategories, Set<String> thirdCountryTransfers,
                              String transferSafeguards, Set<UUID> linkedRetentionRuleIds,
                              String technicalMeasures, String organizationalMeasures,
                              ProcessingActivityStatus status,
                              Instant effectiveFrom, Instant effectiveTo,
                              UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.name = requireText(name, "name", 250);
        this.purposes = requireText(purposes, "purposes", 4000);
        this.lawfulBasis = Objects.requireNonNull(lawfulBasis, "lawfulBasis");
        this.lawfulBasisDetails = lawfulBasisDetails;
        this.controllerName = requireText(controllerName, "controllerName", 250);
        this.controllerContact = requireText(controllerContact, "controllerContact", 250);
        this.dpoContact = dpoContact;
        this.jointControllerName = jointControllerName;
        this.jointControllerContact = jointControllerContact;
        this.dataSubjectCategories = sanitizeCodes(dataSubjectCategories, "dataSubjectCategories");
        this.dataCategories = sanitizeCodes(dataCategories, "dataCategories");
        this.specialCategoriesProcessed = specialCategoriesProcessed;
        this.specialCategoriesJustification = specialCategoriesJustification;
        this.recipientCategories = sanitizeCodes(recipientCategories, "recipientCategories");
        this.thirdCountryTransfers = sanitizeCountries(thirdCountryTransfers);
        this.transferSafeguards = transferSafeguards;
        this.linkedRetentionRuleIds = sanitizeIds(linkedRetentionRuleIds);
        this.technicalMeasures = technicalMeasures;
        this.organizationalMeasures = organizationalMeasures;
        this.status = Objects.requireNonNull(status, "status");
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
        validateConditionalInvariants();
    }

    /** Factory — création d'une activité DRAFT. */
    public static ProcessingActivity draft(UUID tenantId, String reference, String name,
                                           String purposes, LawfulBasis lawfulBasis,
                                           String lawfulBasisDetails,
                                           String controllerName, String controllerContact,
                                           String dpoContact,
                                           String jointControllerName, String jointControllerContact,
                                           Set<String> dataSubjectCategories,
                                           Set<String> dataCategories,
                                           boolean specialCategoriesProcessed,
                                           String specialCategoriesJustification,
                                           Set<String> recipientCategories,
                                           Set<String> thirdCountryTransfers,
                                           String transferSafeguards,
                                           Set<UUID> linkedRetentionRuleIds,
                                           String technicalMeasures, String organizationalMeasures,
                                           UUID createdByUserId, Instant now) {
        return new ProcessingActivity(null, tenantId, reference, name, purposes,
                lawfulBasis, lawfulBasisDetails,
                controllerName, controllerContact, dpoContact,
                jointControllerName, jointControllerContact,
                dataSubjectCategories, dataCategories,
                specialCategoriesProcessed, specialCategoriesJustification,
                recipientCategories, thirdCountryTransfers, transferSafeguards,
                linkedRetentionRuleIds, technicalMeasures, organizationalMeasures,
                ProcessingActivityStatus.DRAFT, null, null,
                createdByUserId, now, now);
    }

    /** Édition (seulement DRAFT). */
    public void editDraft(String name, String purposes,
                          LawfulBasis lawfulBasis, String lawfulBasisDetails,
                          String controllerName, String controllerContact, String dpoContact,
                          String jointControllerName, String jointControllerContact,
                          Set<String> dataSubjectCategories, Set<String> dataCategories,
                          boolean specialCategoriesProcessed, String specialCategoriesJustification,
                          Set<String> recipientCategories, Set<String> thirdCountryTransfers,
                          String transferSafeguards, Set<UUID> linkedRetentionRuleIds,
                          String technicalMeasures, String organizationalMeasures, Instant now) {
        if (status != ProcessingActivityStatus.DRAFT) {
            throw new ProcessingActivityStateException("Only DRAFT activities can be edited");
        }
        this.name = requireText(name, "name", 250);
        this.purposes = requireText(purposes, "purposes", 4000);
        this.lawfulBasis = Objects.requireNonNull(lawfulBasis, "lawfulBasis");
        this.lawfulBasisDetails = lawfulBasisDetails;
        this.controllerName = requireText(controllerName, "controllerName", 250);
        this.controllerContact = requireText(controllerContact, "controllerContact", 250);
        this.dpoContact = dpoContact;
        this.jointControllerName = jointControllerName;
        this.jointControllerContact = jointControllerContact;
        this.dataSubjectCategories = sanitizeCodes(dataSubjectCategories, "dataSubjectCategories");
        this.dataCategories = sanitizeCodes(dataCategories, "dataCategories");
        this.specialCategoriesProcessed = specialCategoriesProcessed;
        this.specialCategoriesJustification = specialCategoriesJustification;
        this.recipientCategories = sanitizeCodes(recipientCategories, "recipientCategories");
        this.thirdCountryTransfers = sanitizeCountries(thirdCountryTransfers);
        this.transferSafeguards = transferSafeguards;
        this.linkedRetentionRuleIds = sanitizeIds(linkedRetentionRuleIds);
        this.technicalMeasures = technicalMeasures;
        this.organizationalMeasures = organizationalMeasures;
        this.updatedAt = now;
        validateConditionalInvariants();
    }

    public void activate(Instant now) {
        if (status != ProcessingActivityStatus.DRAFT) {
            throw new ProcessingActivityStateException("Only DRAFT activities can be activated");
        }
        validateConditionalInvariants();
        this.status = ProcessingActivityStatus.ACTIVE;
        this.effectiveFrom = now;
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        if (status != ProcessingActivityStatus.ACTIVE) {
            throw new ProcessingActivityStateException("Only ACTIVE activities can be archived");
        }
        this.status = ProcessingActivityStatus.ARCHIVED;
        this.effectiveTo = now;
        this.updatedAt = now;
    }

    public boolean isDraft()    { return status == ProcessingActivityStatus.DRAFT; }
    public boolean isActive()   { return status == ProcessingActivityStatus.ACTIVE; }
    public boolean isArchived() { return status == ProcessingActivityStatus.ARCHIVED; }

    /** Invariants conditionnels — vérifiés à chaque mutation et à l'activation. */
    private void validateConditionalInvariants() {
        if (lawfulBasis == LawfulBasis.LEGITIMATE_INTERESTS
                && (lawfulBasisDetails == null || lawfulBasisDetails.isBlank())) {
            throw new ProcessingActivityStateException(
                    "LEGITIMATE_INTERESTS requires lawfulBasisDetails (LIA documentation)");
        }
        if (specialCategoriesProcessed
                && (specialCategoriesJustification == null
                        || specialCategoriesJustification.isBlank())) {
            throw new ProcessingActivityStateException(
                    "specialCategoriesProcessed requires specialCategoriesJustification (Art. 9§2)");
        }
        if (!thirdCountryTransfers.isEmpty()
                && (transferSafeguards == null || transferSafeguards.isBlank())) {
            throw new ProcessingActivityStateException(
                    "thirdCountryTransfers requires transferSafeguards (Art. 44-49)");
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
    private static Set<String> sanitizeCodes(Set<String> input, String field) {
        if (input == null) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        for (String c : input) {
            if (c == null) continue;
            String t = c.trim();
            if (t.isEmpty()) continue;
            if (!CODE_PATTERN.matcher(t).matches()) {
                throw new IllegalArgumentException(field + ": '" + t
                        + "' must match [a-z][a-z0-9._-]{1,63}");
            }
            out.add(t);
        }
        return Collections.unmodifiableSet(out);
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
    public String getPurposes() { return purposes; }
    public LawfulBasis getLawfulBasis() { return lawfulBasis; }
    public String getLawfulBasisDetails() { return lawfulBasisDetails; }
    public String getControllerName() { return controllerName; }
    public String getControllerContact() { return controllerContact; }
    public String getDpoContact() { return dpoContact; }
    public String getJointControllerName() { return jointControllerName; }
    public String getJointControllerContact() { return jointControllerContact; }
    public Set<String> getDataSubjectCategories() { return dataSubjectCategories; }
    public Set<String> getDataCategories() { return dataCategories; }
    public boolean isSpecialCategoriesProcessed() { return specialCategoriesProcessed; }
    public String getSpecialCategoriesJustification() { return specialCategoriesJustification; }
    public Set<String> getRecipientCategories() { return recipientCategories; }
    public Set<String> getThirdCountryTransfers() { return thirdCountryTransfers; }
    public String getTransferSafeguards() { return transferSafeguards; }
    public Set<UUID> getLinkedRetentionRuleIds() { return linkedRetentionRuleIds; }
    public String getTechnicalMeasures() { return technicalMeasures; }
    public String getOrganizationalMeasures() { return organizationalMeasures; }
    public ProcessingActivityStatus getStatus() { return status; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
