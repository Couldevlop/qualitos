package com.openlab.qualitos.quality.stakeholders.infrastructure;

import com.openlab.qualitos.quality.capa.CapaTransition;
import com.openlab.qualitos.quality.capa.CapaTransitionEvent;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Referme la boucle entre une action corrective et l'exigence qui l'a motivée.
 *
 * <p>Ouvrir une action depuis une exigence la fait passer « à réévaluer ». Sans
 * retour, il fallait ensuite se souvenir d'aller la remettre « maîtrisée » dans
 * un second tableau — une tenue manuelle que personne ne fait, et qui laisse le
 * registre afficher des écarts déjà traités. La clôture du dossier suffit
 * désormais.
 *
 * <p><b>Après validation, jamais pendant</b>, comme {@code CapaWebhookRelay} :
 * une clôture que la base finit par annuler ne doit pas avoir déjà requalifié
 * l'exigence. Le client est repris de l'événement, pas du contexte du fil
 * d'exécution : à ce moment la requête HTTP est finie.
 */
@Component
public class StakeholderCapaListener {

    private static final Logger log = LoggerFactory.getLogger(StakeholderCapaListener.class);

    private final StakeholderService stakeholders;

    public StakeholderCapaListener(StakeholderService stakeholders) {
        this.stakeholders = stakeholders;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCapaTransition(CapaTransitionEvent event) {
        if (event.transition() != CapaTransition.CLOSED) {
            return;
        }
        UUID capaId = uuid(event.payload().get("id"));
        if (capaId == null) {
            return;
        }
        try {
            int touched = stakeholders.onCapaClosed(event.tenantId(), capaId);
            if (touched > 0) {
                log.info("Clôture du dossier CAPA {} : {} fiche(s) de partie intéressée requalifiée(s)",
                        capaId, touched);
            }
        } catch (RuntimeException ex) {
            // La clôture est validée et ne se rejoue pas : l'échec d'un effet de
            // bord ne doit pas la faire paraître manquée. Le registre reste
            // modifiable à la main, le journal dit quoi reprendre.
            log.warn("Requalification des exigences du dossier CAPA {} impossible : {}",
                    capaId, ex.getMessage());
        }
    }

    private static UUID uuid(Object value) {
        if (value instanceof UUID id) {
            return id;
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        return null;
    }
}
