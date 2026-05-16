package com.openlab.qualitos.quality.consent.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat Consentement RGPD (Art. 7).
 *
 * Sémantique (Art. 7) :
 *  - "Libre, spécifique, éclairé, univoque" — l'agrégat enregistre la
 *    {@code purposeCode}, la {@code purposeVersion} (version du texte affiché)
 *    et la {@code source} (canal de collecte).
 *  - "Retrait à tout moment, aussi simple que de donner" — méthode
 *    {@link #withdraw}. Terminal et irréversible : un nouveau consentement
 *    nécessite un nouvel enregistrement (nouvelle row).
 *  - "Charge de la preuve sur le responsable" — {@code evidenceUrl}
 *    (PDF/image signé), {@code ipAddress} et {@code userAgent} optionnels.
 *
 * Privacy by design (OWASP A02/A04) :
 *  - {@code subjectIdentifierHash} (SHA-256 hex) — la PII en clair n'est
 *    JAMAIS persistée par l'agrégat. La normalisation et le hash sont
 *    effectués au niveau service application avant construction.
 */
public final class Consent {

    private static final Pattern PURPOSE_CODE = Pattern.compile("^[a-z][a-z0-9._-]{1,63}$");
    private static final Pattern PURPOSE_VERSION = Pattern.compile("^[a-zA-Z0-9._:-]{1,32}$");
    private static final Pattern HEX_HASH = Pattern.compile("^[0-9a-f]{64}$");

    private UUID id;
    private final UUID tenantId;
    private final String subjectIdentifierHash;
    private final String subjectIdentifierLabel;
    private final String purposeCode;
    private final String purposeVersion;
    private final ConsentSource source;
    private final String evidenceUrl;
    private final String ipAddress;
    private final String userAgent;
    private final UUID grantedByUserId;
    private final Instant grantedAt;
    private final Instant expiresAt;
    private ConsentStatus status;
    private Instant withdrawnAt;
    private UUID withdrawnByUserId;
    private String withdrawalReason;
    private Instant updatedAt;

    public Consent(UUID id, UUID tenantId,
                   String subjectIdentifierHash, String subjectIdentifierLabel,
                   String purposeCode, String purposeVersion,
                   ConsentSource source, String evidenceUrl,
                   String ipAddress, String userAgent,
                   UUID grantedByUserId, Instant grantedAt, Instant expiresAt,
                   ConsentStatus status, Instant withdrawnAt,
                   UUID withdrawnByUserId, String withdrawalReason,
                   Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.subjectIdentifierHash = requireHash(subjectIdentifierHash);
        this.subjectIdentifierLabel = subjectIdentifierLabel;
        this.purposeCode = requirePurposeCode(purposeCode);
        this.purposeVersion = requirePurposeVersion(purposeVersion);
        this.source = Objects.requireNonNull(source, "source");
        this.evidenceUrl = evidenceUrl;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.grantedByUserId = grantedByUserId;
        this.grantedAt = Objects.requireNonNull(grantedAt, "grantedAt");
        this.expiresAt = expiresAt;
        this.status = Objects.requireNonNull(status, "status");
        this.withdrawnAt = withdrawnAt;
        this.withdrawnByUserId = withdrawnByUserId;
        this.withdrawalReason = withdrawalReason;
        this.updatedAt = updatedAt != null ? updatedAt : grantedAt;
        validateInvariants();
    }

    /** Factory — création d'un consentement à l'état GRANTED. */
    public static Consent grant(UUID tenantId,
                                String subjectIdentifierHash,
                                String subjectIdentifierLabel,
                                String purposeCode, String purposeVersion,
                                ConsentSource source, String evidenceUrl,
                                String ipAddress, String userAgent,
                                UUID grantedByUserId, Instant grantedAt,
                                Instant expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(grantedAt)) {
            throw new ConsentStateException("expiresAt must be after grantedAt");
        }
        return new Consent(null, tenantId,
                subjectIdentifierHash, subjectIdentifierLabel,
                purposeCode, purposeVersion, source, evidenceUrl,
                ipAddress, userAgent,
                grantedByUserId, grantedAt, expiresAt,
                ConsentStatus.GRANTED, null, null, null, grantedAt);
    }

    /** Retrait du consentement — terminal (Art. 7§3). */
    public void withdraw(UUID byUserId, String reason, Instant now) {
        if (status != ConsentStatus.GRANTED) {
            throw new ConsentStateException("Cannot withdraw consent in status " + status);
        }
        this.status = ConsentStatus.WITHDRAWN;
        this.withdrawnAt = Objects.requireNonNull(now, "now");
        this.withdrawnByUserId = byUserId;
        this.withdrawalReason = reason;
        this.updatedAt = now;
    }

    /** Marque le consentement comme expiré si la date d'expiration est passée. */
    public void expireIfDue(Instant now) {
        if (status == ConsentStatus.GRANTED && expiresAt != null && !now.isBefore(expiresAt)) {
            this.status = ConsentStatus.EXPIRED;
            this.updatedAt = now;
        }
    }

    public boolean isActive(Instant ref) {
        if (status != ConsentStatus.GRANTED) return false;
        return expiresAt == null || ref.isBefore(expiresAt);
    }

    public boolean isTerminal() {
        return status == ConsentStatus.WITHDRAWN || status == ConsentStatus.EXPIRED;
    }

    private void validateInvariants() {
        if (status == ConsentStatus.WITHDRAWN && withdrawnAt == null) {
            throw new IllegalStateException("withdrawn consent must have withdrawnAt");
        }
        if (withdrawnAt != null && withdrawnAt.isBefore(grantedAt)) {
            throw new IllegalStateException("withdrawnAt cannot precede grantedAt");
        }
    }

    private static String requireHash(String v) {
        if (v == null || !HEX_HASH.matcher(v).matches()) {
            throw new IllegalArgumentException("subjectIdentifierHash must be 64-hex SHA-256");
        }
        return v;
    }
    private static String requirePurposeCode(String v) {
        if (v == null || !PURPOSE_CODE.matcher(v).matches()) {
            throw new IllegalArgumentException(
                    "purposeCode must match [a-z][a-z0-9._-]{1,63}");
        }
        return v;
    }
    private static String requirePurposeVersion(String v) {
        if (v == null || !PURPOSE_VERSION.matcher(v).matches()) {
            throw new IllegalArgumentException(
                    "purposeVersion must be 1..32 chars in [A-Za-z0-9._:-]");
        }
        return v;
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getSubjectIdentifierHash() { return subjectIdentifierHash; }
    public String getSubjectIdentifierLabel() { return subjectIdentifierLabel; }
    public String getPurposeCode() { return purposeCode; }
    public String getPurposeVersion() { return purposeVersion; }
    public ConsentSource getSource() { return source; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public UUID getGrantedByUserId() { return grantedByUserId; }
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public ConsentStatus getStatus() { return status; }
    public Instant getWithdrawnAt() { return withdrawnAt; }
    public UUID getWithdrawnByUserId() { return withdrawnByUserId; }
    public String getWithdrawalReason() { return withdrawalReason; }
    public Instant getUpdatedAt() { return updatedAt; }
}
