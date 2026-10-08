package com.openlab.qualitos.licensing.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Ce qu'une licence accorde à une installation on-premise (ADR 0082).
 *
 * @param licenseId  identifiant de la licence, cité dans le journal et au support
 * @param customer   le client, tel qu'il s'affiche dans l'application
 * @param tenantId   l'identifiant du client dans l'installation : figé par la
 *                   licence pour que les données restent rattachées au même client
 *                   d'une licence à la suivante
 * @param tier       le palier commercial (FREE, STANDARD, PRO, ENTERPRISE)
 * @param modules    les codes des modules ouverts ; {@code *} les ouvre tous
 * @param maxUsers   comptes actifs autorisés ; 0 = sans limite
 * @param graceDays  jours pendant lesquels tout fonctionne encore après l'échéance
 */
public record License(String licenseId, String customer, UUID tenantId, String tier, Set<String> modules,
                      int maxUsers, Instant issuedAt, Instant notBefore, Instant expiresAt, int graceDays) {

    public static final String ALL_MODULES = "*";
    public static final int MAX_GRACE_DAYS = 90;
    private static final Pattern TIER = Pattern.compile("FREE|STANDARD|PRO|ENTERPRISE");
    private static final Pattern MODULE = Pattern.compile("\\*|[a-z][a-z0-9-]{0,63}");

    public License {
        licenseId = requireText(licenseId, "licenseId", 64);
        customer = requireText(customer, "customer", 200);
        if (tenantId == null) {
            throw new LicenseException("Champ obligatoire : tenantId");
        }
        if (tier == null || !TIER.matcher(tier).matches()) {
            throw new LicenseException("Palier inconnu : " + tier);
        }
        if (modules == null || modules.isEmpty()) {
            throw new LicenseException("La licence n'ouvre aucun module.");
        }
        for (String m : modules) {
            if (m == null || !MODULE.matcher(m).matches()) {
                throw new LicenseException("Code de module invalide : " + m);
            }
        }
        modules = Set.copyOf(modules);
        if (maxUsers < 0) {
            throw new LicenseException("maxUsers ne peut pas être négatif.");
        }
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(notBefore, "notBefore");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(notBefore)) {
            throw new LicenseException("L'échéance doit suivre la date de début.");
        }
        if (graceDays < 0 || graceDays > MAX_GRACE_DAYS) {
            throw new LicenseException("Délai de grâce hors bornes (0 à " + MAX_GRACE_DAYS + " jours).");
        }
    }

    public boolean allowsModule(String code) {
        return modules.contains(ALL_MODULES) || modules.contains(code);
    }

    public boolean unlimitedUsers() {
        return maxUsers == 0;
    }

    /** La fin du délai de grâce : au-delà, l'installation passe en lecture seule. */
    public Instant graceEndsAt() {
        return expiresAt.plus(Duration.ofDays(graceDays));
    }

    /** Où en est la licence à cet instant. */
    public LicenseStatus statusAt(Instant now) {
        if (now.isBefore(notBefore)) {
            return LicenseStatus.NOT_YET_VALID;
        }
        if (now.isBefore(expiresAt)) {
            return LicenseStatus.VALID;
        }
        return now.isBefore(graceEndsAt()) ? LicenseStatus.GRACE : LicenseStatus.EXPIRED;
    }

    private static String requireText(String v, String field, int max) {
        if (v == null || v.isBlank()) {
            throw new LicenseException("Champ obligatoire : " + field);
        }
        String s = v.strip();
        if (s.length() > max) {
            throw new LicenseException("Champ trop long : " + field);
        }
        return s;
    }
}
