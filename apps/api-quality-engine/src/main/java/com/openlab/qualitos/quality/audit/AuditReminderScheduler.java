package com.openlab.qualitos.quality.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Déclenchement périodique du rappel d'échéance des audits (§4.4).
 *
 * <p>Même forme que {@code AnchoringScheduler} : un composant mince, désactivé en
 * profil « test », dont le seul rôle est d'appeler le service à intervalle réglé.
 * Toute la logique — et donc tout ce qui se teste — vit dans
 * {@link AuditReminderService}, appelable sans horloge de framework.
 *
 * <p>{@code fixedDelay} et non {@code fixedRate} : avec {@code fixedRate}, un
 * passage plus long que la période (relais SMTP lent) déclencherait le suivant
 * avant la fin du précédent, et deux passages simultanés se disputeraient les
 * mêmes lignes. La réservation en base tiendrait, mais autant ne pas la solliciter
 * pour rien.
 *
 * <p>Défaut à l'heure : le rappel se joue à 30 jours, pas à la minute. Balayer
 * plus souvent ne rapprocherait rien et interrogerait la base pour rien.
 */
@Component
@Profile("!test")
public class AuditReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuditReminderScheduler.class);

    private final AuditReminderService service;

    public AuditReminderScheduler(AuditReminderService service) {
        this.service = service;
    }

    @Scheduled(
            initialDelayString = "${qualitos.audit.reminder.initial-delay-ms:120000}",
            fixedDelayString = "${qualitos.audit.reminder.fixed-delay-ms:3600000}")
    public void run() {
        try {
            service.sendDueReminders();
        } catch (RuntimeException e) {
            // Une exception qui remonte à l'ordonnanceur Spring tue la tâche
            // planifiée pour de bon : plus aucun rappel jusqu'au redémarrage, et
            // rien pour le signaler. On l'arrête ici.
            log.error("[audit-reminder] passage interrompu : {}", e.getMessage(), e);
        }
    }
}
