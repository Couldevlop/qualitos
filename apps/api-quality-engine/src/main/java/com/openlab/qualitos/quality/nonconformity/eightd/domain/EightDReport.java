package com.openlab.qualitos.quality.nonconformity.eightd.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Le rapport 8D d'une non-conformité : trois disciplines saisies, puis un
 * document scellé.
 *
 * <p>Agrégat au sens strict : il ne contient PAS les cinq disciplines agrégées
 * (elles vivent dans les autres modules et sont collectées à la demande), mais
 * seulement ce que lui seul détient — l'équipe (D1), l'endiguement (D3), la
 * reconnaissance (D8) — et, une fois émis, l'instantané figé des huit avec sa
 * preuve d'intégrité.
 *
 * <p>Invariants :
 * <ul>
 *   <li>{@code tenantId} vient du jeton, jamais du corps de requête (§18.2 #2) ;</li>
 *   <li>un rapport par non-conformité (contrainte d'unicité en base) ;</li>
 *   <li>à l'état {@code ISSUED}, les six champs de preuve sont tous présents :
 *       une empreinte sans signature, ou une signature sans ancrage, serait une
 *       demi-preuve — donc pas une preuve ;</li>
 *   <li>un rapport émis ne se modifie plus, ni dans ses saisies ni dans son
 *       instantané.</li>
 * </ul>
 */
public final class EightDReport {

    /** 64 caractères hexadécimaux minuscules — l'empreinte SHA-256 du PDF émis. */
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    /** Code de vérification porté par le QR code : opaque, non devinable. */
    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9_-]{16,64}$");

    /** Longueur maximale d'une discipline saisie, alignée sur la colonne. */
    public static final int MAX_SAISIE = 4000;

    private UUID id;
    private final UUID tenantId;
    private final UUID ncId;
    private EightDStatus status;

    private String team;
    private String containment;
    private String recognition;

    private String snapshotJson;
    private String sha256Hex;
    private String signature;
    private String anchorTxRef;
    private String verificationCode;
    private Instant issuedAt;
    private UUID issuedBy;
    private String issuedByName;

    private final Instant createdAt;
    private Instant updatedAt;

    private EightDReport(UUID id, UUID tenantId, UUID ncId, EightDStatus status,
                         String team, String containment, String recognition,
                         String snapshotJson, String sha256Hex, String signature,
                         String anchorTxRef, String verificationCode, Instant issuedAt,
                         UUID issuedBy, String issuedByName,
                         Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.ncId = Objects.requireNonNull(ncId, "ncId");
        this.status = Objects.requireNonNull(status, "status");
        this.team = trimToNull(team);
        this.containment = trimToNull(containment);
        this.recognition = trimToNull(recognition);
        this.snapshotJson = snapshotJson;
        this.sha256Hex = sha256Hex;
        this.signature = signature;
        this.anchorTxRef = anchorTxRef;
        this.verificationCode = verificationCode;
        this.issuedAt = issuedAt;
        this.issuedBy = issuedBy;
        this.issuedByName = issuedByName;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (status == EightDStatus.ISSUED) {
            requireSeal();
        }
    }

    /** Un brouillon vierge, attaché à une non-conformité. */
    public static EightDReport brouillon(UUID tenantId, UUID ncId, Instant now) {
        return new EightDReport(null, tenantId, ncId, EightDStatus.DRAFT,
                null, null, null, null, null, null, null, null, null, null, null, now, now);
    }

    /** Reconstitution depuis la persistance — aucun contrôle métier n'est relâché. */
    public static EightDReport reconstituer(
            UUID id, UUID tenantId, UUID ncId, EightDStatus status,
            String team, String containment, String recognition,
            String snapshotJson, String sha256Hex, String signature, String anchorTxRef,
            String verificationCode, Instant issuedAt, UUID issuedBy, String issuedByName,
            Instant createdAt, Instant updatedAt) {
        return new EightDReport(id, tenantId, ncId, status, team, containment, recognition,
                snapshotJson, sha256Hex, signature, anchorTxRef, verificationCode,
                issuedAt, issuedBy, issuedByName, createdAt, updatedAt);
    }

    /**
     * Renseigne les trois disciplines qui n'ont aucune source.
     *
     * <p>Chaque champ est facultatif : un rapport auquel manque la reconnaissance
     * de l'équipe s'émet quand même, et se déclare <b>partiel</b>. Exiger les
     * trois aurait allongé le geste de clôture jusqu'à le faire contourner, et un
     * document aux cases remplies à la hâte ment plus qu'un document qui dit ce
     * qui lui manque.
     *
     * @throws EightDStateException si le rapport est déjà émis
     */
    public void saisir(String team, String containment, String recognition, Instant now) {
        requireModifiable();
        this.team = limiter(team, "D1 (équipe)");
        this.containment = limiter(containment, "D3 (endiguement)");
        this.recognition = limiter(recognition, "D8 (reconnaissance)");
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    /**
     * Fige le contenu et y attache la preuve d'intégrité.
     *
     * <p>Un seul passage possible : réémettre produirait deux documents portant la
     * même référence et deux empreintes différentes, dont aucun ne serait
     * opposable.
     *
     * @throws EightDStateException si le rapport est déjà émis
     */
    public void emettre(String snapshotJson, String sha256Hex, String signature,
                        String anchorTxRef, String verificationCode,
                        UUID issuedBy, String issuedByName, Instant now) {
        requireModifiable();
        this.snapshotJson = requireText(snapshotJson, "snapshotJson");
        this.sha256Hex = requireSha256(sha256Hex);
        this.signature = requireText(signature, "signature");
        this.anchorTxRef = requireText(anchorTxRef, "anchorTxRef");
        this.verificationCode = requireCode(verificationCode);
        this.issuedBy = Objects.requireNonNull(issuedBy, "issuedBy");
        this.issuedByName = trimToNull(issuedByName);
        this.issuedAt = Objects.requireNonNull(now, "now");
        this.status = EightDStatus.ISSUED;
        this.updatedAt = now;
    }

    public void assignId(UUID id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public boolean estEmis() {
        return status == EightDStatus.ISSUED;
    }

    private void requireModifiable() {
        if (estEmis()) {
            throw new EightDStateException(
                    "Le rapport 8D est émis : son contenu est scellé et ne se modifie plus");
        }
    }

    private void requireSeal() {
        requireText(snapshotJson, "snapshotJson");
        requireSha256(sha256Hex);
        requireText(signature, "signature");
        requireText(anchorTxRef, "anchorTxRef");
        requireCode(verificationCode);
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(issuedBy, "issuedBy");
    }

    private static String limiter(String value, String champ) {
        String trimmed = trimToNull(value);
        if (trimmed != null && trimmed.length() > MAX_SAISIE) {
            throw new EightDValidationException(
                    champ + " dépasse " + MAX_SAISIE + " caractères");
        }
        return trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String requireSha256(String value) {
        if (value == null || !SHA256.matcher(value).matches()) {
            throw new IllegalArgumentException("sha256Hex : 64 caractères hexadécimaux minuscules");
        }
        return value;
    }

    private static String requireCode(String value) {
        if (value == null || !CODE.matcher(value).matches()) {
            throw new IllegalArgumentException("verificationCode : 16 à 64 caractères URL-safe");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " requis");
        }
        return value;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getNcId() { return ncId; }
    public EightDStatus getStatus() { return status; }
    public String getTeam() { return team; }
    public String getContainment() { return containment; }
    public String getRecognition() { return recognition; }
    public String getSnapshotJson() { return snapshotJson; }
    public String getSha256Hex() { return sha256Hex; }
    public String getSignature() { return signature; }
    public String getAnchorTxRef() { return anchorTxRef; }
    public String getVerificationCode() { return verificationCode; }
    public Instant getIssuedAt() { return issuedAt; }
    public UUID getIssuedBy() { return issuedBy; }
    public String getIssuedByName() { return issuedByName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
