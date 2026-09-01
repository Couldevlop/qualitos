package com.openlab.qualitos.core.billing.invoice;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Le repli quand l'envoi de courriels est éteint
 * ({@code qualitos.mail.enabled} absent ou faux, donc pas de
 * {@link SmtpInvoiceMailer}).
 *
 * <p>Il en faut un : sans lui, {@link InvoiceService} n'a pas de
 * {@link InvoiceMailPort} et api-core refuse de démarrer — l'émission et la
 * consultation des factures tomberaient avec l'envoi, alors qu'elles ne
 * dépendent d'aucun serveur SMTP.
 *
 * <p><b>La condition est l'exacte MIROIR de celle de {@link SmtpInvoiceMailer}</b>,
 * et non un {@code @ConditionalOnMissingBean}. Ce dernier n'est fiable que dans
 * l'auto-configuration, où l'ordre d'évaluation est garanti ; dans une
 * {@code @Configuration} ordinaire, il dépend de l'ordre — indéterminé — dans
 * lequel le balayage de composants et la lecture des méthodes {@code @Bean} se
 * croisent. Deux conditions complémentaires sur la même propriété donnent
 * exactement un bean, toujours, sans dépendre d'un ordre.
 *
 * <p><b>Le repli refuse, il ne se tait pas.</b> Accepter l'appel et ne rien
 * envoyer marquerait la facture « envoyée » alors qu'elle n'est jamais partie :
 * le client ne la recevrait pas, la plateforme affirmerait le contraire, et
 * l'écart ne se verrait qu'à la relance. Ici l'appel échoue, la transaction
 * d'envoi est annulée, la facture reste renvoyable, et le message dit quel
 * réglage manque.
 */
@Configuration
@ConditionalOnProperty(prefix = "qualitos.mail", name = "enabled",
        havingValue = "false", matchIfMissing = true)
public class InvoiceMailFallbackConfig {

    @Bean
    public InvoiceMailPort disabledInvoiceMailer() {
        return (to, subject, body, pdf) -> {
            throw new IllegalStateException(
                    "Envoi de facture impossible : qualitos.mail.enabled n'est pas actif sur "
                            + "cette instance. La facture reste emise et telechargeable.");
        };
    }
}
