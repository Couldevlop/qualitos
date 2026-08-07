package com.openlab.qualitos.quality.ishikawa;

/**
 * Avancement d'une action décidée à partir d'un diagramme.
 *
 * <p>Trois états, pas davantage : une action décidée en réunion se suit d'un coup
 * d'œil. Un cycle de vie plus fin — instruction, validation, preuve d'efficacité —
 * relève de la CAPA, vers laquelle l'action peut être escaladée (§3.6).
 */
public enum IshikawaActionStatus {
    TODO,
    IN_PROGRESS,
    DONE
}
