package com.openlab.qualitos.quality.nonconformity.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Déclenchement périodique du balayage des orphelins (§4.3).
 *
 * <p>Composant mince, désactivé en profil « test », sur le modèle de
 * {@code AuditReminderScheduler} : toute la logique — et donc tout ce qui se
 * teste — vit dans {@link OrphanObjectSweeper}.
 *
 * <p>Quotidien par défaut. Un orphelin ne coûte que du stockage, et il ne bouge
 * pas : balayer toutes les heures interrogerait le bucket vingt-quatre fois par
 * jour pour, presque toujours, ne rien trouver.
 *
 * <p>Plusieurs répliques peuvent balayer en même temps sans dommage, à la
 * différence du rappel d'audit : supprimer un objet déjà supprimé est sans
 * effet (le DELETE S3 est idempotent), et la seule conséquence est un décompte
 * un peu généreux dans le journal. Aucun verrou n'est donc nécessaire ici.
 */
@Component
@Profile("!test")
public class OrphanObjectSweepScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrphanObjectSweepScheduler.class);

    private final OrphanObjectSweeper sweeper;

    public OrphanObjectSweepScheduler(OrphanObjectSweeper sweeper) {
        this.sweeper = sweeper;
    }

    @Scheduled(
            initialDelayString = "${qualitos.storage.orphan-sweep.initial-delay-ms:300000}",
            fixedDelayString = "${qualitos.storage.orphan-sweep.fixed-delay-ms:86400000}")
    public void run() {
        try {
            sweeper.sweep();
        } catch (RuntimeException e) {
            // Une exception qui remonte à l'ordonnanceur Spring tue la tâche
            // planifiée pour de bon : plus aucun balayage jusqu'au redémarrage,
            // et rien pour le signaler.
            log.error("[orphan-sweep] passage interrompu : {}", e.getMessage(), e);
        }
    }
}
