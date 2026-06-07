package com.openlab.qualitos.quality.erpconnector;

/**
 * Cycle de vie d'une connexion ERP (calqué sur le module itsm).
 */
public enum ErpConnectionStatus {
    /** Connexion configurée et active : peut être utilisée pour sync. */
    ACTIVE,
    /** Connexion désactivée explicitement par l'admin tenant. */
    DISABLED,
    /** Désactivée automatiquement après N échecs consécutifs (auth/réseau). */
    DISABLED_ON_ERRORS
}
