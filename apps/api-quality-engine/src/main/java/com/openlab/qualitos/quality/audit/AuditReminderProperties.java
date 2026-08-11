package com.openlab.qualitos.quality.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Réglages du rappel d'échéance des audits (§4.4).
 *
 * <p>Tout vient de variables d'environnement (cf. application.yml) : changer le
 * délai de rappel ou le tempo du balayage ne doit pas demander une recompilation
 * (§18.2-9). Aucun secret ici — l'adresse d'expédition et les identifiants SMTP
 * relèvent de {@code spring.mail.*} et de {@link AuditMailProperties}.
 */
@Component
@ConfigurationProperties(prefix = "qualitos.audit.reminder")
public class AuditReminderProperties {

    /** Nombre maximal d'audits traités par passage — garde-fou, pas une cible. */
    private static final int MAX_BATCH = 500;

    /**
     * Délai avant échéance qui déclenche le rappel. 30 jours : un audit se prépare
     * (convocation, preuves, agendas), et prévenir la veille n'aide personne.
     */
    private int daysBefore = 30;

    /**
     * Nombre d'audits traités par passage. Borné : un tenant qui importerait mille
     * plans d'un coup ne doit pas transformer un passage d'ordonnanceur en rafale
     * de mille courriels dans la même minute — le reste part au passage suivant.
     */
    private int batchSize = 200;

    public int getDaysBefore() {
        return daysBefore;
    }

    /**
     * Un délai nul ou négatif ramènerait le rappel au jour même, voire à une date
     * passée : on le refuse au démarrage plutôt que de laisser un dispositif muet
     * se croire actif.
     */
    public void setDaysBefore(int daysBefore) {
        if (daysBefore <= 0) {
            throw new IllegalArgumentException(
                    "qualitos.audit.reminder.days-before doit être strictement positif");
        }
        this.daysBefore = daysBefore;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "qualitos.audit.reminder.batch-size doit être strictement positif");
        }
        this.batchSize = Math.min(batchSize, MAX_BATCH);
    }
}
