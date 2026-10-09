package com.openlab.qualitos.licensing.domain;

import java.time.Instant;
import java.util.Optional;

/**
 * Ce qu'une installation sait de sa licence à un instant donné.
 *
 * @param license la licence lue et vérifiée ; absente si elle manque ou ne se vérifie pas
 * @param reason  pourquoi la licence n'est pas utilisable, à montrer à l'administrateur
 */
public record LicenseState(Edition edition, LicenseStatus status, License license, String reason,
                           Instant evaluatedAt) {

    public static LicenseState notRequired(Instant now) {
        return new LicenseState(Edition.SAAS, LicenseStatus.NOT_REQUIRED, null, null, now);
    }

    public static LicenseState missing(Instant now) {
        return new LicenseState(Edition.ONPREM, LicenseStatus.MISSING, null, "Aucune licence installée.", now);
    }

    public static LicenseState invalid(String reason, Instant now) {
        return new LicenseState(Edition.ONPREM, LicenseStatus.INVALID, null, reason, now);
    }

    public static LicenseState of(License license, Instant now) {
        return new LicenseState(Edition.ONPREM, license.statusAt(now), license, null, now);
    }

    public Optional<License> licenseOpt() {
        return Optional.ofNullable(license);
    }

    public boolean allowsWrites() {
        return status.allowsWrites();
    }

    /** Vrai si le module est ouvert : toujours en SaaS, selon la licence en on-premise. */
    public boolean allowsModule(String code) {
        if (edition == Edition.SAAS) {
            return true;
        }
        return license != null && license.allowsModule(code);
    }
}
