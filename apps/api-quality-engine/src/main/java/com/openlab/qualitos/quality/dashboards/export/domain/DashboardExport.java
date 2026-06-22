package com.openlab.qualitos.quality.dashboards.export.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Aggregate — an official, signed PDF export of a dashboard (CLAUDE.md §7.3 / §7.4).
 *
 * <p>An export is an <b>auditable, immutable proof artifact</b> : it carries the
 * SHA-256 fingerprint of the rendered PDF, the hybrid Ed25519+ML-DSA-65 signature
 * envelope (ADR 0011) over that fingerprint, and the blockchain anchor reference
 * (§11.3). It is addressable by a short, opaque {@code verificationCode} embedded
 * in the QR code of the PDF, which the public verify endpoint resolves
 * (§7.4 — "QR code blockchain").
 *
 * <p>Invariants :
 * <ul>
 *   <li>{@code tenantId} always from the JWT — never the request body (§18.2 #2).</li>
 *   <li>{@code verificationCode} is a 32-char URL-safe random token (the authority).</li>
 *   <li>{@code sha256Hex} is the lowercase hex digest of the PDF bytes.</li>
 *   <li>once created the record is never mutated (proof immutability).</li>
 * </ul>
 */
public final class DashboardExport {

    /** 64-hex-char lowercase SHA-256. */
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");
    /** 32-char URL-safe verification code (Base64URL alphabet without padding). */
    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9_-]{16,64}$");

    private UUID id;
    private final UUID tenantId;
    private final UUID userId;
    private final UUID dashboardId;
    private final String dashboardName;
    private final String verificationCode;
    private final String sha256Hex;
    private final String signatureEnvelope;
    private final String anchorTxRef;
    private final Instant createdAt;

    public DashboardExport(UUID id, UUID tenantId, UUID userId, UUID dashboardId,
                           String dashboardName, String verificationCode,
                           String sha256Hex, String signatureEnvelope,
                           String anchorTxRef, Instant createdAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.dashboardId = Objects.requireNonNull(dashboardId, "dashboardId");
        this.dashboardName = requireText(dashboardName, "dashboardName");
        this.verificationCode = requireCode(verificationCode);
        this.sha256Hex = requireSha256(sha256Hex);
        this.signatureEnvelope = requireText(signatureEnvelope, "signatureEnvelope");
        this.anchorTxRef = requireText(anchorTxRef, "anchorTxRef");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static DashboardExport create(UUID tenantId, UUID userId, UUID dashboardId,
                                         String dashboardName, String verificationCode,
                                         String sha256Hex, String signatureEnvelope,
                                         String anchorTxRef, Instant now) {
        return new DashboardExport(null, tenantId, userId, dashboardId, dashboardName,
                verificationCode, sha256Hex, signatureEnvelope, anchorTxRef, now);
    }

    public void assignId(UUID id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    private static String requireSha256(String s) {
        if (s == null || !SHA256.matcher(s).matches()) {
            throw new IllegalArgumentException("sha256Hex must be 64 lowercase hex chars");
        }
        return s;
    }

    private static String requireCode(String s) {
        if (s == null || !CODE.matcher(s).matches()) {
            throw new IllegalArgumentException("verificationCode must be 16..64 URL-safe chars");
        }
        return s;
    }

    private static String requireText(String s, String field) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException(field + " required");
        }
        return s;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
    public UUID getDashboardId() { return dashboardId; }
    public String getDashboardName() { return dashboardName; }
    public String getVerificationCode() { return verificationCode; }
    public String getSha256Hex() { return sha256Hex; }
    public String getSignatureEnvelope() { return signatureEnvelope; }
    public String getAnchorTxRef() { return anchorTxRef; }
    public Instant getCreatedAt() { return createdAt; }
}
