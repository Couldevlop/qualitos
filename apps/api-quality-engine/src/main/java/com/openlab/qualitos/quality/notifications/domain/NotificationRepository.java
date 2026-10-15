package com.openlab.qualitos.quality.notifications.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de persistance des notifications (implémenté en infrastructure).
 * Toutes les méthodes sont bornées au tenant pour garantir l'isolation (OWASP A01).
 */
public interface NotificationRepository {

    Notification save(Notification notification);

    /** Récupère une notification du tenant (Optional vide si absente ou autre tenant). */
    Optional<Notification> findByIdForTenant(UUID id, UUID tenantId);

    /** Les {@code limit} notifications les plus récentes visibles par l'utilisateur (destinataire + diffusions). */
    List<Notification> findRecentForRecipient(UUID tenantId, String userId, int limit);

    /** Nombre de notifications non lues visibles par l'utilisateur. */
    long countUnreadForRecipient(UUID tenantId, String userId);

    /** Marque toutes les non-lues visibles par l'utilisateur comme lues ; renvoie le nombre affecté. */
    int markAllReadForRecipient(UUID tenantId, String userId, Instant at);

    /**
     * Marque lues les notifications ADRESSÉES à cet utilisateur qui mènent à {@code link} :
     * ce qu'elles demandaient n'est plus à faire (une validation close par un refus,
     * une décision prise). Les diffusions au tenant ne sont pas touchées.
     */
    int markReadForRecipientAndLink(UUID tenantId, String userId, String link, Instant at);
}
