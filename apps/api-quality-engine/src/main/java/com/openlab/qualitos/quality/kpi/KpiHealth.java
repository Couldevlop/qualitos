package com.openlab.qualitos.quality.kpi;

/**
 * Statut santé d'une mesure par rapport aux seuils.
 * UNKNOWN : pas de seuil défini ou pas de mesure.
 */
public enum KpiHealth {
    OK,
    WARNING,
    CRITICAL,
    UNKNOWN
}
