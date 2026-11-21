package com.openlab.qualitos.quality.objective;

import com.openlab.qualitos.quality.capa.CapaCriticity;

/**
 * Le niveau qui a déclenché une proposition de CAPA.
 *
 * <p>Distinct de {@link ObjectiveStatus} : le statut dit où en est l'objectif
 * aujourd'hui, le niveau dit ce qui a déclenché, et reste écrit dans la
 * proposition même quand l'objectif est revenu au vert. C'est ce niveau que
 * l'auditeur relit pour savoir sur quoi la décision a été prise.
 *
 * <p>Les deux niveaux PROPOSENT, aucun n'impose : la différence est le nombre de
 * périodes qu'il faut pour déclencher (deux par défaut en alerte, une en
 * critique), pas le pouvoir de créer un dossier sans que personne ne l'ait
 * décidé (CLAUDE.md §12).
 */
public enum ObjectiveLevel {

    WARNING(CapaCriticity.MEDIUM),
    CRITICAL(CapaCriticity.CRITICAL);

    private final CapaCriticity criticity;

    ObjectiveLevel(CapaCriticity criticity) { this.criticity = criticity; }

    /**
     * La criticité du dossier CAPA que ce niveau propose.
     *
     * <p>Portée ici plutôt que décidée dans le service : ajouter un niveau sans
     * choisir ce qu'il vaut devient impossible à oublier.
     */
    public CapaCriticity criticity() { return criticity; }

    /** Le statut d'objectif correspondant à ce niveau. */
    public ObjectiveStatus asStatus() {
        return this == CRITICAL ? ObjectiveStatus.CRITICAL : ObjectiveStatus.WARNING;
    }
}
