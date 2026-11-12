package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.webhooks.EventType;

/**
 * Les moments de la vie d'un dossier CAPA qui méritent d'être consignés (§11.5)
 * et, pour certains, annoncés aux systèmes abonnés (§13.2).
 *
 * <p>Une énumération plutôt que des appels dispersés dans le service : la liste
 * de ce qu'on inscrit et de ce qu'on publie se lit ici, d'un seul tenant. Ajouter
 * une transition sans lui donner de trace devient un oubli visible.
 *
 * <p>Toutes n'ont pas d'événement sortant. Le démarrage du traitement ou une
 * modification de champ intéressent l'auditeur, pas un système tiers : publier
 * tout ce qui bouge noierait l'abonné et l'obligerait à filtrer ce qu'on aurait
 * dû ne pas envoyer. Le journal, lui, ne laisse rien passer.
 */
public enum CapaTransition {

    OPENED("capa.case.opened", EventType.CAPA_CASE_OPENED,
            "Dossier CAPA ouvert"),
    UPDATED("capa.case.updated", null,
            "Dossier CAPA modifié"),
    STARTED("capa.case.started", null,
            "Traitement du dossier CAPA démarré"),
    RESOLVED("capa.case.resolved", EventType.CAPA_CASE_RESOLVED,
            "Dossier CAPA résolu"),
    /** Vérification concluante : le dossier se clôt sur une efficacité démontrée. */
    CLOSED("capa.case.closed", EventType.CAPA_CASE_CLOSED,
            "Dossier CAPA clos après vérification d'efficacité"),
    /**
     * Vérification non concluante. Le dossier RETOURNE en traitement : c'est un
     * fait au moins aussi important que la clôture, puisqu'il dit que l'action
     * corrective n'a pas produit son effet.
     */
    EFFECTIVENESS_REJECTED("capa.case.effectiveness-rejected", EventType.CAPA_EFFECTIVENESS_VERIFIED,
            "Efficacité non démontrée : le dossier CAPA repart en traitement"),
    /**
     * Efficacité PARTIELLE (ADR 0108) : l'action a produit un effet réel sans
     * atteindre le critère.
     *
     * <p>Comme {@code EFFECTIVENESS_REJECTED}, le dossier repart en traitement —
     * et c'est la seule chose que les deux ont en commun. Ici la cause était la
     * bonne et l'action ne va pas assez loin : on en ajoute une. Là, c'est la
     * cause qu'il faut reprendre. Les confondre au journal reviendrait à
     * effacer, pour l'auditeur, la différence entre un système qui affine et un
     * système qui s'est trompé.
     */
    EFFECTIVENESS_PARTIAL("capa.case.effectiveness-partial", EventType.CAPA_EFFECTIVENESS_VERIFIED,
            "Efficacité partielle : une action complémentaire est attendue sur le dossier CAPA"),
    /**
     * Le dossier REPART en traitement parce que ses actions l'y ramènent : une
     * action ajoutée à un dossier résolu, ou une action réalisée qu'on rouvre.
     *
     * <p>Distincte de {@code EFFECTIVENESS_REJECTED} et de
     * {@code EFFECTIVENESS_PARTIAL}, qui disent la même chose du dossier mais
     * pour une tout autre raison : là, une vérification a conclu que l'effet
     * n'était pas au rendez-vous. Ici, personne n'a rien vérifié — le travail
     * n'est simplement plus fini. Les confondre au journal ferait lire à
     * l'auditeur un échec d'efficacité là où il n'y a qu'une action de plus.
     */
    REOPENED("capa.case.reopened", null,
            "Dossier CAPA revenu en traitement : une action reste à réaliser"),
    REJECTED("capa.case.rejected", null,
            "Dossier CAPA rejeté"),
    DELETED("capa.case.deleted", null,
            "Dossier CAPA supprimé");

    private final String auditAction;
    private final EventType eventType;
    private final String summary;

    CapaTransition(String auditAction, EventType eventType, String summary) {
        this.auditAction = auditAction;
        this.eventType = eventType;
        this.summary = summary;
    }

    /** Action inscrite au journal chaîné du tenant, au format module.ressource.action. */
    public String auditAction() {
        return auditAction;
    }

    /** Événement annoncé aux abonnés, ou {@code null} si la transition reste interne. */
    public EventType eventType() {
        return eventType;
    }

    public String summary() {
        return summary;
    }
}
