package com.openlab.qualitos.quality.dmaic.resolution.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Une ligne du plan d'actions de la fiche.
 *
 * @param percent avancement de 0 à 100
 * @param capaId  le dossier CAPA ouvert pour cette action, ou {@code null}
 * @param containment une action de confinement (phase Analyser) plutôt qu'une action
 *                    du plan d'amélioration
 * @param nature   la nature portée par le dossier CAPA, recopiée à l'ouverture ;
 *                 {@code null} sur les lignes antérieures à cette donnée
 * @param criticity la criticité portée par le dossier CAPA, recopiée de même
 */
public record ActionLine(LocalDate createdOn, String action, Person responsible, ActionStatus status,
                         LocalDate plannedOn, LocalDate doneOn, int percent, String notes, UUID capaId,
                         boolean containment, ActionNature nature, ActionCriticity criticity) {

    public ActionLine(LocalDate createdOn, String action, Person responsible, ActionStatus status,
                      LocalDate plannedOn, LocalDate doneOn, int percent, String notes, UUID capaId,
                      boolean containment) {
        this(createdOn, action, responsible, status, plannedOn, doneOn, percent, notes, capaId, containment,
                null, null);
    }

    public ActionLine(LocalDate createdOn, String action, Person responsible, ActionStatus status,
                      LocalDate plannedOn, LocalDate doneOn, int percent, String notes, UUID capaId) {
        this(createdOn, action, responsible, status, plannedOn, doneOn, percent, notes, capaId, false);
    }

    public ActionLine(LocalDate createdOn, String action, Person responsible, ActionStatus status,
                      LocalDate plannedOn, LocalDate doneOn, int percent, String notes) {
        this(createdOn, action, responsible, status, plannedOn, doneOn, percent, notes, null);
    }

    /**
     * La nature à afficher : celle recopiée du dossier, sinon celle que la place
     * de la ligne implique — une action de confinement endigue, une action du
     * plan d'amélioration corrige.
     */
    public ActionNature effectiveNature() {
        if (nature != null) {
            return nature;
        }
        return containment ? ActionNature.CONTAINMENT : ActionNature.CORRECTIVE;
    }
}
