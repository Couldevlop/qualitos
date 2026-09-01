package com.openlab.qualitos.core.billing.invoice;

/**
 * Port sortant : l'envoi de la facture, PDF en pièce jointe.
 *
 * <p>Même motif que {@code AuditReminderMailer} dans le moteur de qualité :
 * une interface pour que la règle « une facture ne se renvoie pas seule » se
 * teste sans serveur SMTP, et pour qu'aucun courriel ne parte d'une suite de
 * tests.
 *
 * <p>Le corps est du TEXTE BRUT — il porte la raison sociale du client, saisie
 * par un humain. En HTML il faudrait l'échapper, et un oubli d'échappement
 * dans un courriel se voit rarement avant qu'il ne serve (OWASP A03).
 */
public interface InvoiceMailPort {

    /**
     * @param to          le destinataire de FACTURATION, jamais l'administrateur
     *                    du tenant
     * @param subject     ligne unique — un saut de ligne y permettrait de forger
     *                    d'autres en-têtes
     * @param body        texte brut
     * @param pdf         la facture, en pièce jointe
     */
    void send(String to, String subject, String body, byte[] pdf);
}
