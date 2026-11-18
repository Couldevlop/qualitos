package com.openlab.qualitos.core.billing.invoice;

/**
 * La messagerie de la <b>plateforme</b> : celle du déploiement, réglée par
 * variables d'environnement.
 *
 * <p>Ce marqueur existe pour une raison précise. Depuis qu'une organisation
 * peut régler la sienne, il y a deux envoyeurs possibles, et
 * {@code InvoiceMailPort} ne suffit plus à désigner celui qu'on veut : le
 * décorateur qui choisit entre les deux implémente lui aussi ce port, et
 * demander « un {@code InvoiceMailPort} » depuis le décorateur le ferait
 * s'injecter lui-même.
 *
 * <p>Un type distinct nomme sans ambiguïté le repli, là où un
 * {@code @Qualifier} par nom aurait dépendu de laquelle des deux
 * configurations — SMTP réglé ou envoi éteint — s'est trouvée active.
 */
public interface PlatformInvoiceMailPort extends InvoiceMailPort {
}
