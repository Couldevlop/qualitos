package com.openlab.qualitos.quality.supplier;

/**
 * Cycle de vie d'un fournisseur (§4.6).
 *
 * PROSPECT     : en cours d'évaluation, ne peut pas être source d'achats fermes.
 * APPROVED     : actif, conforme.
 * CONDITIONAL  : actif mais sous surveillance (NC ouvertes, audit défavorable).
 * SUSPENDED    : commandes gelées, retour possible vers APPROVED après remédiation.
 * BLACKLISTED  : exclu, transition terminale (toute réintégration passe par un
 *                nouvel onboarding — code différent).
 */
public enum SupplierStatus {
    PROSPECT,
    APPROVED,
    CONDITIONAL,
    SUSPENDED,
    BLACKLISTED
}
