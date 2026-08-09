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
