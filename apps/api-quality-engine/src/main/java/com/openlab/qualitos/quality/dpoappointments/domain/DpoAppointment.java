package com.openlab.qualitos.quality.dpoappointments.domain;

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
 * Agrégat — désignation d'un Délégué à la Protection des Données (DPO),
 * RGPD Art. 37-39.
 *
 * Cycle de vie :
 *   PROPOSED → ACTIVE  (activate — exige notification autorité Art. 37.7)
 *   PROPOSED → CANCELLED (cancel avant prise d'effet)
 *   ACTIVE   → ENDED (end — fin de mandat, exige raison)
 *
 * Garde-fous :
 *  - Activation exige : effectiveFrom, regulatorNotifiedAt + reference,
 *    dpoEmail, fullName.
 *  - Si dpoType=EXTERNAL : externalCompanyName obligatoire (Art. 37.6).
 *  - Invariant transverse "1 ACTIVE par (tenant, scope)" garanti par le
 *    service (auto-end du précédent) et l'index partiel DB.
 *
 * Privacy by design (OWASP A02) : les données contiennent du contact pro
 * (email DPO, téléphone) qui sont des données personnelles professionnelles,
 * pas des PII sensibles. Le payload d'audit n'inclut PAS le contact en clair.
 */
public final class DpoAppointment {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");
    private static final Pattern SCOPE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{0,63}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Map<DpoAppointmentStatus, Set<DpoAppointmentStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(DpoAppointmentStatus.class);
        ALLOWED.put(DpoAppointmentStatus.PROPOSED,
                EnumSet.of(DpoAppointmentStatus.ACTIVE, DpoAppointmentStatus.CANCELLED));
        ALLOWED.put(DpoAppointmentStatus.ACTIVE,
                EnumSet.of(DpoAppointmentStatus.ENDED));
        ALLOWED.put(DpoAppointmentStatus.ENDED,
                EnumSet.noneOf(DpoAppointmentStatus.class));
        ALLOWED.put(DpoAppointmentStatus.CANCELLED,
                EnumSet.noneOf(DpoAppointmentStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private String dpoFullName;
    private String dpoEmail;
    private String dpoPhone;
    private DpoType dpoType;
    private String externalCompanyName;
    private String qualifications;
    private final String scope;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private Instant regulatorNotifiedAt;
    private String regulatorNotificationReference;
    private Set<UUID> linkedProcessingActivityIds;
    private DpoAppointmentStatus status;
    private String endReason;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public DpoAppointment(UUID id, UUID tenantId, String reference,
                          String dpoFullName, String dpoEmail, String dpoPhone,
                          DpoType dpoType, String externalCompanyName, String qualifications,
                          String scope,
                          Instant effectiveFrom, Instant effectiveTo,
                          Instant regulatorNotifiedAt, String regulatorNotificationReference,
                          Set<UUID> linkedProcessingActivityIds,
                          DpoAppointmentStatus status, String endReason,
                          UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.dpoFullName = requireText(dpoFullName, "dpoFullName", 250);
        this.dpoEmail = requireEmail(dpoEmail);
        this.dpoPhone = dpoPhone;
        this.dpoType = Objects.requireNonNull(dpoType, "dpoType");
        this.externalCompanyName = externalCompanyName;
        this.qualifications = qualifications;
        this.scope = requireScope(scope);
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.regulatorNotifiedAt = regulatorNotifiedAt;
        this.regulatorNotificationReference = regulatorNotificationReference;
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.status = Objects.requireNonNull(status, "status");
        this.endReason = endReason;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
        validateTypeInvariant();
    }

    public static DpoAppointment propose(UUID tenantId, String reference,
                                         String dpoFullName, String dpoEmail, String dpoPhone,
                                         DpoType dpoType, String externalCompanyName,
                                         String qualifications, String scope,
                                         Set<UUID> linkedProcessingActivityIds,
                                         UUID createdByUserId, Instant now) {
        return new DpoAppointment(null, tenantId, reference,
                dpoFullName, dpoEmail, dpoPhone, dpoType, externalCompanyName,
                qualifications, scope, null, null, null, null,
                linkedProcessingActivityIds,
                DpoAppointmentStatus.PROPOSED, null,
                createdByUserId, now, now);
    }

    public void editProposed(String dpoFullName, String dpoEmail, String dpoPhone,
                             DpoType dpoType, String externalCompanyName,
                             String qualifications,
                             Set<UUID> linkedProcessingActivityIds, Instant now) {
        if (status != DpoAppointmentStatus.PROPOSED) {
            throw new DpoAppointmentStateException("Only PROPOSED appointments can be edited");
        }
        this.dpoFullName = requireText(dpoFullName, "dpoFullName", 250);
        this.dpoEmail = requireEmail(dpoEmail);
        this.dpoPhone = dpoPhone;
        this.dpoType = Objects.requireNonNull(dpoType, "dpoType");
        this.externalCompanyName = externalCompanyName;
        this.qualifications = qualifications;
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.updatedAt = now;
        validateTypeInvariant();
    }

    public void activate(Instant effectiveFrom,
                         Instant regulatorNotifiedAt, String regulatorNotificationReference,
                         Instant now) {
        ensureTransition(DpoAppointmentStatus.ACTIVE);
        if (effectiveFrom == null) {
            throw new DpoAppointmentStateException("effectiveFrom required to activate");
        }
        if (regulatorNotifiedAt == null) {
            throw new DpoAppointmentStateException(
                    "regulatorNotifiedAt required (Art. 37.7 — DPA notification)");
        }
        if (regulatorNotificationReference == null
                || regulatorNotificationReference.isBlank()) {
            throw new DpoAppointmentStateException(
                    "regulatorNotificationReference required (Art. 37.7)");
        }
        validateTypeInvariant();
        this.effectiveFrom = effectiveFrom;
        this.regulatorNotifiedAt = regulatorNotifiedAt;
        this.regulatorNotificationReference = regulatorNotificationReference;
        this.status = DpoAppointmentStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void end(String reason, Instant effectiveTo, Instant now) {
        ensureTransition(DpoAppointmentStatus.ENDED);
        if (reason == null || reason.isBlank()) {
            throw new DpoAppointmentStateException("end reason required");
        }
        if (effectiveTo == null) {
            throw new DpoAppointmentStateException("effectiveTo required");
        }
        if (effectiveFrom != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new DpoAppointmentStateException("effectiveTo must be >= effectiveFrom");
        }
        this.endReason = reason;
        this.effectiveTo = effectiveTo;
        this.status = DpoAppointmentStatus.ENDED;
        this.updatedAt = now;
    }

    public void cancel(String reason, Instant now) {
        ensureTransition(DpoAppointmentStatus.CANCELLED);
        if (reason == null || reason.isBlank()) {
            throw new DpoAppointmentStateException("cancellation reason required");
        }
        this.endReason = reason;
        this.status = DpoAppointmentStatus.CANCELLED;
        this.updatedAt = now;
    }

    public boolean isProposed() { return status == DpoAppointmentStatus.PROPOSED; }
    public boolean isActive()   { return status == DpoAppointmentStatus.ACTIVE; }
    public boolean isTerminal() {
        return status == DpoAppointmentStatus.ENDED || status == DpoAppointmentStatus.CANCELLED;
    }

    private void validateTypeInvariant() {
        if (dpoType == DpoType.EXTERNAL
                && (externalCompanyName == null || externalCompanyName.isBlank())) {
            throw new DpoAppointmentStateException(
                    "EXTERNAL DPO requires externalCompanyName (Art. 37.6)");
        }
    }

    private void ensureTransition(DpoAppointmentStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new DpoAppointmentStateException(
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
    private static String requireScope(String v) {
        if (v == null || !SCOPE_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException(
                    "scope must match [A-Z][A-Z0-9_-]{0,63}");
        }
        return v;
    }
    private static String requireText(String v, String f, int maxLen) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " required");
        if (v.length() > maxLen) throw new IllegalArgumentException(
                f + " too long (max " + maxLen + ")");
        return v;
    }
    private static String requireEmail(String v) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException("dpoEmail required");
        if (v.length() > 320) throw new IllegalArgumentException("dpoEmail too long");
        if (!EMAIL_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException("dpoEmail must be a valid email");
        }
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
    public String getDpoFullName() { return dpoFullName; }
    public String getDpoEmail() { return dpoEmail; }
    public String getDpoPhone() { return dpoPhone; }
    public DpoType getDpoType() { return dpoType; }
    public String getExternalCompanyName() { return externalCompanyName; }
    public String getQualifications() { return qualifications; }
    public String getScope() { return scope; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public Instant getRegulatorNotifiedAt() { return regulatorNotifiedAt; }
    public String getRegulatorNotificationReference() { return regulatorNotificationReference; }
    public Set<UUID> getLinkedProcessingActivityIds() { return linkedProcessingActivityIds; }
    public DpoAppointmentStatus getStatus() { return status; }
    public String getEndReason() { return endReason; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
