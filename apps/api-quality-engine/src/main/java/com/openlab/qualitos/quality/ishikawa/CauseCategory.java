package com.openlab.qualitos.quality.ishikawa;

/**
 * Catégories de causes du diagramme d'Ishikawa.
 *
 * Modèle 6M (norme historique, secteurs industriels):
 *   METHODS, MANPOWER, MACHINES, MATERIALS, MEASUREMENTS, ENVIRONMENT.
 *
 * Le 7M ajoute MANAGEMENT. Le 8M ajoute MONEY.
 *
 * Le diagramme stocke un mode (6M / 7M / 8M) ; ce sont les catégories autorisées par mode.
 */
public enum CauseCategory {
    METHODS,
    MANPOWER,
    MACHINES,
    MATERIALS,
    MEASUREMENTS,
    ENVIRONMENT,
    MANAGEMENT,
    MONEY
}
