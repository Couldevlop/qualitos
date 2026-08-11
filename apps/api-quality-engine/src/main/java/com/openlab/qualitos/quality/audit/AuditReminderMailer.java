package com.openlab.qualitos.quality.audit;

/**
 * Port d'envoi du rappel par courriel (§4.4).
 *
 * <p>Une interface, et non un appel direct à {@code JavaMailSender} : l'envoi est
 * DÉSACTIVÉ par défaut. Sans configuration SMTP, aucune implémentation n'est
 * instanciée, le service n'en trouve pas — et la notification interne part quand
 * même. La messagerie est un complément du rappel, jamais sa condition.
 */
public interface AuditReminderMailer {

    /**
     * Envoie un rappel. Toute panne du serveur SMTP remonte en exception non
     * vérifiée : c'est l'appelant qui décide quoi en faire, et il a déjà posé la
     * notification interne.
     */
    void send(String to, String subject, String body);
}
