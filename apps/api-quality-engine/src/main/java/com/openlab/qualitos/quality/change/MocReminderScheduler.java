package com.openlab.qualitos.quality.change;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Déclenchement périodique du rappel aux validateurs (ADR 0087).
 *
 * <p>Même forme que {@code AuditReminderScheduler} : mince, inactif en profil
 * « test », {@code fixedDelay} pour qu'un passage lent ne chevauche pas le
 * suivant. Le délai se compte en jours : un balayage horaire suffit, et la
 * réservation en base écarte tout doublon entre répliques.
 */
@Component
@Profile("!test")
public class MocReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(MocReminderScheduler.class);

    private final MocReminderService service;

    public MocReminderScheduler(MocReminderService service) {
        this.service = service;
    }

    @Scheduled(
            initialDelayString = "${qualitos.moc.reminder.initial-delay-ms:180000}",
            fixedDelayString = "${qualitos.moc.reminder.fixed-delay-ms:3600000}")
    public void run() {
        try {
            service.sendDueReminders();
        } catch (RuntimeException e) {
            // Une exception remontée à l'ordonnanceur arrêterait la tâche pour de bon.
            log.error("[moc-reminder] passage interrompu : {}", e.getMessage(), e);
        }
    }
}
