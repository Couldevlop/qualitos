package com.openlab.qualitos.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * La mise en forme commune des courriels sortants.
 *
 * <p>Tous les courriels du produit partaient en texte brut : ni logo, ni
 * couleurs, rien qui les rattache à la plateforme ni à l'organisation qui les
 * envoie. Un seul gabarit plutôt qu'un par envoi, parce qu'une charte graphique
 * appliquée à un courriel sur cinq n'est pas une charte.
 *
 * <p><b>Deux en-têtes, et la différence n'est pas cosmétique.</b>
 * {@link #plateforme} porte le logo QualitOS : c'est la plateforme qui parle, à
 * ses propres utilisateurs. {@link #organisation} porte le nom de l'organisation
 * émettrice, sans logo QualitOS : ce courriel-là part vers le client d'un
 * client, sous une adresse que ce client a réglée, et il l'engage. Signer d'un
 * logo qui n'est pas le sien tromperait le destinataire sur qui lui écrit.
 *
 * <p><b>Le HTML ne remplace pas le texte, il s'y ajoute.</b> L'appelant pose les
 * deux versions ; le client de messagerie choisit. Un lecteur qui refuse le HTML,
 * une passerelle qui le dépouille ou un lecteur d'écran retombent sur un message
 * complet, et non sur une page vide.
 *
 * <p><b>Tout ce qui vient d'ailleurs est échappé.</b> Un corps de message porte
 * ce qu'un utilisateur a saisi, et un nom d'organisation ce qu'un administrateur
 * a réglé : sans échappement, l'un comme l'autre pourrait injecter du balisage
 * dans le courriel d'un tiers (OWASP A03).
 *
 * <p>Classe sans état et sans dépendance au cadre : elle se teste en JUnit nu.
 */
public final class MailTemplate {

    /**
     * L'identifiant du logo dans le message.
     *
     * <p>Le logo voyage <b>dans</b> le courriel et non au bout d'un lien : la
     * plupart des messageries bloquent les images distantes par défaut, et
     * celles qui les chargent signalent au passage que le message a été ouvert.
     */
    public static final String LOGO_CID = "logo-qualitos";

    /** Le chemin de la ressource du logo, pour l'appelant qui l'attache. */
    public static final String LOGO_RESOURCE = "/com/openlab/qualitos/mail/logo-qualitos.png";

    private static final String ACCENT = "#2563d6";
    private static final String TEXTE = "#1f2733";
    private static final String TEXTE_DOUX = "#6b7382";
    private static final String BORDURE = "#e3e6eb";
    private static final String FOND = "#f9fafb";
    private static final String BLANC = "#ffffff";

    /** Une adresse web dans un corps de message, à rendre cliquable. */
    private static final Pattern LIEN = Pattern.compile("https?://[^\\s<>\"]+");

    private MailTemplate() {
    }

    /**
     * Un courriel de la plateforme, en-tête au logo QualitOS.
     *
     * <p>L'appelant attache le logo sous {@link #LOGO_CID} ; s'il ne le fait
     * pas, le destinataire voit le texte de remplacement et le message reste
     * lisible.
     *
     * @param corps le message en texte brut, tel qu'il part aussi en version texte
     */
    public static String plateforme(String corps) {
        String entete = "<img src=\"cid:" + LOGO_CID + "\" alt=\"QualitOS\""
                + " width=\"132\" style=\"display:block;border:0;width:132px;height:auto;\">";
        return document(entete, corps);
    }

    /**
     * Un courriel qui part au nom d'une organisation, sans logo QualitOS.
     *
     * @param nom   le nom de l'organisation émettrice ; vide, l'en-tête se réduit
     *              au filet de couleur plutôt que d'afficher un cadre vide
     * @param corps le message en texte brut
     */
    public static String organisation(String nom, String corps) {
        String propre = nom == null ? "" : nom.strip();
        String entete = propre.isEmpty() ? "" : "<div style=\"font:600 18px/1.3"
                + " -apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;"
                + "letter-spacing:.02em;color:" + TEXTE + ";\">" + echapper(propre) + "</div>";
        return document(entete, corps);
    }

    /** Le document complet : en-tête, corps, pied. */
    private static String document(String entete, String corps) {
        return """
                <!DOCTYPE html>
                <html lang="fr"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                </head>
                <body style="margin:0;padding:0;background:%s;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" \
                border="0" style="background:%s;"><tr><td align="center" style="padding:24px 12px;">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" \
                style="width:100%%;max-width:600px;background:%s;border:1px solid %s;\
                border-radius:12px;overflow:hidden;">
                <tr><td style="padding:28px 32px 0 32px;">%s</td></tr>
                <tr><td style="padding:20px 32px 0 32px;">\
                <div style="height:3px;width:56px;background:%s;border-radius:2px;"></div></td></tr>
                <tr><td style="padding:20px 32px 32px 32px;font:400 15px/1.65 \
                -apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;color:%s;">%s</td></tr>
                <tr><td style="padding:16px 32px 24px 32px;border-top:1px solid %s;\
                font:400 12px/1.5 -apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;\
                color:%s;">Message envoyé par QualitOS. Merci de ne pas répondre \
                à cette adresse si une adresse de réponse figure ci-dessus.</td></tr>
                </table></td></tr></table></body></html>"""
                .formatted(FOND, FOND, BLANC, BORDURE, entete, ACCENT, TEXTE,
                        paragraphes(corps), BORDURE, TEXTE_DOUX);
    }

    /**
     * Le texte brut rendu en paragraphes.
     *
     * <p>Une ligne vide sépare deux paragraphes, un simple retour reste un
     * retour. Sans cela, le HTML écraserait toute la mise en page du message en
     * un seul bloc, et un courriel structuré deviendrait illisible.
     */
    private static String paragraphes(String corps) {
        if (corps == null || corps.isBlank()) {
            return "";
        }
        String[] blocs = corps.strip().replace("\r\n", "\n").replace('\r', '\n').split("\n{2,}");
        StringBuilder rendu = new StringBuilder();
        for (int i = 0; i < blocs.length; i++) {
            String bloc = liens(echapper(blocs[i])).replace("\n", "<br>");
            rendu.append("<p style=\"margin:0 0 ")
                    .append(i == blocs.length - 1 ? "0" : "14px")
                    .append(" 0;\">").append(bloc).append("</p>");
        }
        return rendu.toString();
    }

    /**
     * Les adresses web rendues cliquables.
     *
     * <p>Appliqué APRÈS l'échappement : le lien est construit sur le texte déjà
     * échappé, de sorte qu'aucune saisie ne peut refermer l'attribut {@code href}
     * pour en ouvrir un autre.
     */
    private static String liens(String echappe) {
        Matcher m = LIEN.matcher(echappe);
        StringBuilder sortie = new StringBuilder();
        while (m.find()) {
            String url = m.group();
            m.appendReplacement(sortie, Matcher.quoteReplacement(
                    "<a href=\"" + url + "\" style=\"color:" + ACCENT + ";\">" + url + "</a>"));
        }
        m.appendTail(sortie);
        return sortie.toString();
    }

    /**
     * Les octets du logo, à attacher en ligne sous {@link #LOGO_CID}.
     *
     * <p>Rendus en octets plutôt qu'en ressource du cadre : cette bibliothèque
     * sert deux services et ne dépend d'aucun, et l'appelant enveloppe comme il
     * lui convient.
     *
     * @throws UncheckedIOException si la ressource manque du jar — c'est une
     *                              erreur d'empaquetage, pas un incident
     *                              d'exécution, et elle doit se voir au premier
     *                              envoi plutôt que d'arriver muette chez le
     *                              destinataire
     */
    public static byte[] logo() {
        try (InputStream flux = MailTemplate.class.getResourceAsStream(LOGO_RESOURCE)) {
            if (flux == null) {
                throw new UncheckedIOException(new IOException(
                        "Logo absent du jar : " + LOGO_RESOURCE));
            }
            return flux.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Ce qui vient d'ailleurs n'ouvre pas de balise. */
    private static String echapper(String texte) {
        return texte.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
