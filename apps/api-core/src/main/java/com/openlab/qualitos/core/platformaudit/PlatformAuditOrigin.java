package com.openlab.qualitos.core.platformaudit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * D'où vient l'acte : l'adresse qui l'a émis et le logiciel employé.
 *
 * <p><b>Pourquoi lire la requête ici plutôt que de la passer en paramètre.</b>
 * Les actes consignés naissent au fond des services métier
 * ({@code TenantService}, {@code SubscriptionService}…), à deux ou trois
 * appels du contrôleur. Faire descendre un {@code HttpServletRequest}
 * jusque-là reviendrait à changer la signature de toutes les méthodes
 * traversées, et surtout à mettre l'origine entre les mains de l'appelant —
 * donc à rendre possible de la lui faire dire. Elle est lue du contexte de
 * requête, comme l'identité l'est du jeton : l'appelant ne la fournit pas, il
 * ne peut donc pas la falsifier (§18.2 règle 2, appliquée à l'origine plutôt
 * qu'au client).
 *
 * <p>Hors requête HTTP — une tâche planifiée, une migration — il n'y a pas
 * d'origine, et le journal porte {@code null} plutôt qu'une valeur inventée.
 */
final class PlatformAuditOrigin {

    /** Au-delà, la colonne tronquerait : on coupe ici, en le sachant. */
    private static final int MAX_ADRESSE = 64;
    private static final int MAX_AGENT = 256;

    private final String adresse;
    private final String agent;

    private PlatformAuditOrigin(String adresse, String agent) {
        this.adresse = adresse;
        this.agent = agent;
    }

    /**
     * L'origine de la requête en cours, ou une origine vide hors contexte web.
     */
    static PlatformAuditOrigin courante() {
        RequestAttributes attributs = RequestContextHolder.getRequestAttributes();
        if (!(attributs instanceof ServletRequestAttributes servlet)) {
            return new PlatformAuditOrigin(null, null);
        }
        HttpServletRequest requete = servlet.getRequest();
        return new PlatformAuditOrigin(
                tronque(adresseDe(requete), MAX_ADRESSE),
                tronque(requete.getHeader("User-Agent"), MAX_AGENT));
    }

    /**
     * L'adresse de l'appelant, derrière un répartiteur de charge.
     *
     * <p>En grappe, {@code getRemoteAddr()} rend l'adresse de l'ingress, la
     * même pour tout le monde — inutile dans une piste d'audit. La première
     * entrée de {@code X-Forwarded-For} est l'adresse du client telle que le
     * premier relais l'a vue.
     *
     * <p><b>Limite assumée, et c'est pour cela qu'elle est écrite ici :</b>
     * cet en-tête est fourni par le client et un appelant direct peut donc
     * l'inventer. Il ne vaut que parce que l'ingress de QualitOS le RÉÉCRIT
     * avant de transmettre. Le champ est un indice de provenance, pas une
     * preuve d'identité — l'identité, elle, vient du jeton signé.
     */
    private static String adresseDe(HttpServletRequest requete) {
        String transmise = requete.getHeader("X-Forwarded-For");
        if (transmise != null && !transmise.isBlank()) {
            int virgule = transmise.indexOf(',');
            return (virgule >= 0 ? transmise.substring(0, virgule) : transmise).trim();
        }
        return requete.getRemoteAddr();
    }

    private static String tronque(String valeur, int maximum) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return valeur.length() <= maximum ? valeur : valeur.substring(0, maximum);
    }

    String adresse() {
        return adresse;
    }

    String agent() {
        return agent;
    }
}
