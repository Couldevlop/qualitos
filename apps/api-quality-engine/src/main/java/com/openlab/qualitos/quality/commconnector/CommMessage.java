package com.openlab.qualitos.quality.commconnector;

import java.util.List;
import java.util.Map;

/**
 * Message neutre à pousser vers un canal de communication. Chaque
 * {@link CommProviderClient} le traduit vers son format propre (MessageCard Teams,
 * blocks Slack, attachments Mattermost).
 *
 * @param title   titre court (gras, première ligne)
 * @param text    corps du message (markdown léger supporté par tous les providers)
 * @param severity sévérité → couleur/accent
 * @param linkLabel libellé du bouton/lien d'action (peut être null si pas de lien)
 * @param linkUrl   URL de l'action (deep-link QualitOS), null si absent
 * @param facts   paires clé→valeur affichées en liste (ex: "Sévérité"→"Critique")
 */
public record CommMessage(
        String title,
        String text,
        CommSeverity severity,
        String linkLabel,
        String linkUrl,
        List<Map.Entry<String, String>> facts
) {
    public CommMessage {
        if (severity == null) severity = CommSeverity.INFO;
        if (facts == null) facts = List.of();
    }
}
