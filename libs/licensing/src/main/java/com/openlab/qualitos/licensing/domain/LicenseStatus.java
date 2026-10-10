package com.openlab.qualitos.licensing.domain;

/**
 * L'état d'une licence, tel qu'une installation le constate.
 *
 * <p>Seuls {@link #VALID}, {@link #GRACE} et {@link #NOT_REQUIRED} autorisent les
 * écritures. Les lectures et les exports restent toujours possibles : un client
 * dont la licence a expiré garde l'accès à ses enregistrements qualité, qui sont
 * des preuves.
 */
public enum LicenseStatus {

    /** Signée, en cours de validité. */
    VALID(true),
    /** Échue, mais dans le délai de grâce : tout fonctionne, l'écran prévient. */
    GRACE(true),
    /** Délai de grâce passé : lecture seule. */
    EXPIRED(false),
    /** Pas encore en vigueur. */
    NOT_YET_VALID(false),
    /** Illisible ou mal signée. */
    INVALID(false),
    /** Aucune licence installée. */
    MISSING(false),
    /** Édition SaaS : la licence ne s'applique pas. */
    NOT_REQUIRED(true);

    private final boolean allowsWrites;

    LicenseStatus(boolean allowsWrites) {
        this.allowsWrites = allowsWrites;
    }

    public boolean allowsWrites() {
        return allowsWrites;
    }
}
