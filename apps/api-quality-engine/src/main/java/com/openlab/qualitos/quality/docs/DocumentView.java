package com.openlab.qualitos.quality.docs;

/** Les vues rapides de la GED (ADR 0094). */
public enum DocumentView {
    /** Les documents actifs. */
    ALL,
    /** Ceux dont l'utilisateur est le propriétaire. */
    MINE,
    /** Ceux dont une version en revue attend la décision de l'utilisateur. */
    TO_VALIDATE,
    /** Ceux dont la revue périodique est dépassée ou proche. */
    REVIEW_DUE,
    /** Les documents archivés. */
    ARCHIVED
}
