package com.openlab.qualitos.quality.apqp;

/**
 * Où en est un livrable, colonne « Status » du classeur du client.
 *
 * <p>Quatre états, dont un qui n'est pas un avancement : {@code BLOCKED} dit que
 * le livrable n'avance plus et que la cause est ailleurs. Le confondre avec
 * {@code IN_PROGRESS} reviendrait à cacher l'unique information qu'une revue de
 * projet cherche.
 *
 * <p>{@code DONE} n'est pas un statut comme les autres : il est lié à la case du
 * livrable, et le service tient les deux d'accord (ADR 0072). Cocher pose
 * {@code DONE} et 100 % ; décocher les ramène en arrière.
 */
public enum ApqpDeliverableStatus {

    /** Rien n'a commencé. Valeur d'amorçage, comme dans le classeur. */
    NOT_STARTED,

    IN_PROGRESS,

    /** Arrêté par une cause extérieure au livrable — attente client, fournisseur, décision. */
    BLOCKED,

    /** Acquis. Équivaut à la case cochée : le service ne laisse pas les deux diverger. */
    DONE
}
