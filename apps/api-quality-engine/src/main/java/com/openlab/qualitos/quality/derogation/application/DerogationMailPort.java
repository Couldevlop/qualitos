package com.openlab.qualitos.quality.derogation.application;

import java.util.UUID;

/**
 * L'envoi de courriel du processus de dérogation.
 *
 * <p><b>Deux courriels, deux portées.</b> Celui du manager est interne : il part
 * de l'initiateur vers son manager, à l'intérieur de l'organisation. Celui du
 * client s'adresse à un tiers et <b>engage le client</b> : il part donc de la
 * messagerie que l'administrateur du client a réglée (ADR 0127), avec
 * l'initiateur en copie cachée.
 *
 * <p><b>Pourquoi un port et pas un appel SMTP direct.</b> Les réglages de
 * messagerie par client vivent dans {@code api-core}, avec leur secret, dans une
 * autre base que celle du moteur : le moteur ne peut pas les lire, et ne doit
 * pas en recopier le mot de passe. C'est donc {@code api-core} qui envoie, et
 * l'adaptateur de ce port l'appelle. Le service, lui, ignore tout de ce chemin.
 */
public interface DerogationMailPort {

    /**
     * Au manager désigné : un dossier attend sa signature, avec le lien qui
     * l'ouvre dans QualitOS.
     *
     * <p>Interne, donc de la messagerie du déploiement. Une panne de messagerie
     * ne doit <b>pas</b> faire échouer la demande : la notification interne est
     * déjà posée, et c'est elle qui porte l'alerte rouge.
     */
    void notifyManager(UUID managerAccountId, String reference, String link);

    /**
     * Au client : un simple courriel, la dérogation en pièce jointe.
     *
     * <p>Corps en <b>texte brut</b> à dessein : il porte un commentaire saisi par
     * un utilisateur, qu'il faudrait échapper en HTML — et un oubli
     * d'échappement dans un courriel se voit rarement avant qu'il ne serve
     * (OWASP A03). Le texte brut retire le problème au lieu de le gérer.
     *
     * <p>Toute panne remonte en exception : l'état du dossier ne doit pas passer
     * à « En attente client » si rien n'est parti.
     */
    void sendToCustomer(CustomerMail courriel);

    /**
     * Ce qui part au client.
     *
     * @param bccAccountId l'initiateur, en copie cachée — son adresse se lit dans
     *                     l'annuaire, jamais dans le corps de la requête
     */
    record CustomerMail(
            UUID tenantId,
            String to,
            UUID bccAccountId,
            String subject,
            String body,
            String attachmentName,
            byte[] pdf
    ) {}
}
