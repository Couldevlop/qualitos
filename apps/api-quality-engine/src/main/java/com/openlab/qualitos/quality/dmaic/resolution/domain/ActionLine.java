package com.openlab.qualitos.quality.dmaic.resolution.domain;

import java.time.LocalDate;

/**
 * Une ligne du plan d'actions de la fiche.
 *
 * @param percent avancement de 0 à 100
 */
public record ActionLine(LocalDate createdOn, String action, Person responsible, ActionStatus status,
                         LocalDate plannedOn, LocalDate doneOn, int percent, String notes) {
}
