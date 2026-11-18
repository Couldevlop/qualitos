package com.openlab.qualitos.core.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Quels serveurs sortants une organisation a le droit de désigner.
 *
 * <p>Sans cette règle, la fonctionnalité serait un <b>scanner de réseau
 * interne</b> offert à tout administrateur de client. Il suffirait de pointer
 * la messagerie sur {@code 10.0.0.7:22} et de lire le motif d'échec : « refusée »
 * et « délai dépassé » ne se ressemblent pas, et la différence dit si l'hôte
 * existe et si le port est ouvert. C'est précisément la faille SSRF
 * ({@code CLAUDE.md} §11.1 A10) — la particularité étant qu'ici le motif
 * détaillé, qui sert honnêtement au réglage, est aussi ce qui renseigne.
 *
 * <p>Alors en SaaS, un hôte qui résout vers une adresse <b>privée, locale,
 * de bouclage ou de lien-local</b> est refusé. L'adresse de métadonnées des
 * fournisseurs d'informatique en nuage ({@code 169.254.169.254}) tombe sous la
 * même règle, et c'est voulu.
 *
 * <p>En <b>on-premise</b>, elle ne s'applique pas : un relais interne y est le
 * cas NORMAL, pas l'exception. Refuser {@code smtp.intranet.local} sur une
 * installation qui vit entièrement sur le réseau de l'organisation rendrait la
 * fonctionnalité inutilisable là où elle sert le plus, et l'exploitant y a déjà
 * accès au réseau par construction — on ne lui apprendrait rien.
 */
@Component
public class MailHostPolicy {

    private static final Logger log = LoggerFactory.getLogger(MailHostPolicy.class);

    private final boolean autoriseLesAdressesInternes;

    public MailHostPolicy(@Value("${qualitos.edition:saas}") String edition,
                          @Value("${qualitos.mail.allow-internal-hosts:}") String surcharge) {
        boolean onPremise = "onprem".equalsIgnoreCase(edition == null ? "" : edition.strip());
        this.autoriseLesAdressesInternes = surcharge == null || surcharge.isBlank()
                ? onPremise
                : Boolean.parseBoolean(surcharge.strip());
    }

    /**
     * @return le motif du refus, ou {@code null} si l'hôte est acceptable
     */
    public String refus(String hote) {
        if (hote == null || hote.isBlank()) {
            return "Le serveur sortant est obligatoire.";
        }
        if (autoriseLesAdressesInternes) {
            return null;
        }
        InetAddress[] adresses;
        try {
            adresses = InetAddress.getAllByName(hote.strip());
        } catch (UnknownHostException e) {
            // Un nom qui ne resout pas est dit tel quel : c'est une faute de
            // frappe neuf fois sur dix, et ce motif-la ne renseigne sur rien
            // d'interne.
            return "Le serveur « " + hote + " » est introuvable (nom inconnu).";
        }
        for (InetAddress adresse : adresses) {
            if (estInterne(adresse)) {
                // Le journal garde l'hote pour l'exploitant ; la reponse, elle,
                // ne dit PAS vers quoi il a resolu -- ce serait rendre par le
                // message ce que le refus vient d'interdire.
                log.warn("mail.host.refused host={} raison=adresse-interne", hote);
                return "Ce serveur n'est pas joignable depuis la plateforme : "
                        + "seules les adresses publiques sont acceptees.";
            }
        }
        return null;
    }

    /**
     * Les familles d'adresses qu'on ne laisse pas atteindre.
     *
     * <p>On s'appuie sur les classifications de la bibliothèque standard plutôt
     * que sur des plages écrites à la main : une liste de préfixes se périme
     * (IPv6 unique-local, adresses mappées) et un oubli ne se voit pas.
     */
    private static boolean estInterne(InetAddress a) {
        return a.isLoopbackAddress()
                || a.isAnyLocalAddress()
                || a.isLinkLocalAddress()
                || a.isSiteLocalAddress()
                || a.isMulticastAddress()
                // IPv6 unique-local (fc00::/7), que isSiteLocalAddress ne couvre pas.
                || (a.getAddress().length == 16 && (a.getAddress()[0] & 0xFE) == 0xFC);
    }
}
