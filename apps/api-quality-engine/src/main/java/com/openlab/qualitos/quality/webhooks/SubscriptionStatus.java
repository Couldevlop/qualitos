package com.openlab.qualitos.quality.webhooks;

public enum SubscriptionStatus {
    ACTIVE,
    PAUSED,
    /** Désactivé automatiquement après trop d'échecs consécutifs. */
    DISABLED_ON_ERRORS
}
