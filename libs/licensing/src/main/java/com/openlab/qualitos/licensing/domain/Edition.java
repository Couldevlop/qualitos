package com.openlab.qualitos.licensing.domain;

import java.util.Locale;

/**
 * L'édition d'une installation (ADR 0082).
 *
 * <p>{@link #SAAS} : la plateforme de l'éditeur, plusieurs clients, la console
 * éditeur et la facturation. {@link #ONPREM} : une installation chez un client,
 * un seul client, des modules ouverts par la licence.
 */
public enum Edition {

    SAAS,
    ONPREM;

    /**
     * Lit le réglage {@code QUALITOS_EDITION}. Vide : SaaS, l'édition historique.
     * Une valeur inconnue empêche le démarrage : retomber en silence sur SaaS
     * ouvrirait la console éditeur chez un client.
     */
    public static Edition parse(String raw) {
        String v = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (v.isEmpty()) {
            return SAAS;
        }
        if (v.equals("ON_PREM") || v.equals("ONPREMISE") || v.equals("ON_PREMISE")) {
            return ONPREM;
        }
        try {
            return valueOf(v);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "QUALITOS_EDITION invalide : '" + raw + "' (attendu : saas | onprem)", e);
        }
    }
}
