package com.openlab.qualitos.quality.stakeholders.domain;

/**
 * La pertinence d'une partie intéressée pour le système de management.
 *
 * <p>« À déterminer » est un état plein, pas un vide : l'ISO 9001 §4.2 demande
 * de déterminer les parties intéressées PERTINENTES, et un auditeur lit la
 * décision — y compris la décision de non-pertinence, qui doit être justifiée.
 */
public enum Relevance {
    RELEVANT,
    NOT_RELEVANT,
    TO_DETERMINE
}
