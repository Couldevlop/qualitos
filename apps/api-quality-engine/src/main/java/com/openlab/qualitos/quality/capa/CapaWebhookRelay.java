package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.webhooks.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Annonce aux systèmes abonnés les transitions d'un dossier CAPA (§13.2).
 *
 * <p><b>Après validation, jamais pendant.</b> Une requête HTTP ne se rattrape pas :
 * prévenir un ERP ou un outil ITSM d'une clôture que la base finit par annuler
 * laisserait le tiers avec une information fausse et aucun moyen de le savoir.
 * Publier hors transaction évite aussi de la tenir ouverte pendant un aller-retour
 * réseau, sur des points d'entrée qui écrivent en base.
 *
 * <p>Le tenant est REPOSÉ depuis l'événement avant l'appel. Le contexte du fil
 * d'exécution est encore là au moment du commit, mais s'y fier reviendrait à faire
 * dépendre une annonce d'un état ambiant : on préfère le rendre explicite, et le
 * restaurer ensuite pour ne rien changer à ce que voit la suite du traitement.
 */
@Component
public class CapaWebhookRelay {

    private static final Logger log = LoggerFactory.getLogger(CapaWebhookRelay.class);

    private final WebhookService webhooks;

    public CapaWebhookRelay(WebhookService webhooks) {
        this.webhooks = webhooks;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransition(CapaTransitionEvent event) {
        if (event.transition().eventType() == null) {
            return;
        }
        String previous = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(event.tenantId().toString());
            webhooks.publish(event.transition().eventType(), event.payload());
        } catch (RuntimeException ex) {
            // Un abonné injoignable ou une souscription mal formée ne doit pas
            // remonter ici : la transition métier est validée, elle ne se rejoue
            // pas. Les échecs de livraison ont déjà leur propre suivi (statut de
            // livraison, relances, file de rebut).
            log.warn("Publication de l'événement CAPA {} impossible pour le tenant {} : {}",
                    event.transition().eventType().wire(), event.tenantId(), ex.getMessage());
        } finally {
            if (previous == null) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(previous);
            }
        }
    }
}
