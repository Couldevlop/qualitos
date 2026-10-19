package com.openlab.qualitos.quality.docs;

/**
 * Le statut tel que la liste l'affiche et le filtre (ADR 0094) : le cycle de vie,
 * sauf qu'une revue dépassée l'emporte, et qu'un document archivé n'est plus que cela.
 */
public enum DisplayStatus {
    DRAFT, IN_FORCE, IN_REVISION, REVIEW_OVERDUE, ARCHIVED
}
