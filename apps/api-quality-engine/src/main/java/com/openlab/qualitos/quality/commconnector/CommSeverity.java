package com.openlab.qualitos.quality.commconnector;

/**
 * Sévérité d'un événement notifié, utilisée pour la couleur/accent du message premium
 * (barre Slack/Mattermost, themeColor Teams).
 */
public enum CommSeverity {
    INFO("36A2EB"),
    WARNING("F5A623"),
    CRITICAL("D0021B");

    /** Couleur hex (sans '#') pour les attachments/cards. */
    private final String hex;

    CommSeverity(String hex) { this.hex = hex; }

    public String hex() { return hex; }

    /** Préfixe d'emoji premium pour le titre. */
    public String emoji() {
        return switch (this) {
            case INFO -> "ℹ️";       // ℹ️
            case WARNING -> "⚠️";    // ⚠️
            case CRITICAL -> "🔴";   // 🔴
        };
    }
}
