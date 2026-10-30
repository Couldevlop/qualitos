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
 */
public record ActionLine(LocalDate createdOn, String action, Person responsible, ActionStatus status,
                         LocalDate plannedOn, LocalDate doneOn, int percent, String notes, UUID capaId,
                         boolean containment) {

    public ActionLine(LocalDate createdOn, String action, Person responsible, ActionStatus status,
                      LocalDate plannedOn, LocalDate doneOn, int percent, String notes, UUID capaId) {
        this(createdOn, action, responsible, status, plannedOn, doneOn, percent, notes, capaId, false);
    }

    public ActionLine(LocalDate createdOn, String action, Person responsible, ActionStatus status,
                      LocalDate plannedOn, LocalDate doneOn, int percent, String notes) {
        this(createdOn, action, responsible, status, plannedOn, doneOn, percent, notes, null);
    }
}
