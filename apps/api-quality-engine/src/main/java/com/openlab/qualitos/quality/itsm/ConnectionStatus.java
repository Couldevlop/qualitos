package com.openlab.qualitos.quality.itsm;

public enum ConnectionStatus {
    /** Connexion configurée et active : peut être utilisée pour sync. */
    ACTIVE,
    /** Connexion désactivée explicitement par l'admin tenant. */
    DISABLED,
    /** Désactivée automatiquement après N échecs consécutifs d'authentification ou de réseau. */
    DISABLED_ON_ERRORS
}
