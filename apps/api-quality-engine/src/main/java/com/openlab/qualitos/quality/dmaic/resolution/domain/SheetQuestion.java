package com.openlab.qualitos.quality.dmaic.resolution.domain;

/**
 * Une question du questionnaire de résolution.
 *
 * @param code       identifiant stable, auquel la réponse est rattachée : changer
 *                   le libellé ne détache pas les réponses déjà données
 * @param section    le bloc de la fiche où elle se pose
 * @param topic      la rubrique de définition (Quoi, Qui…), pour la phase
 *                   « Définir » seulement ; {@code null} ailleurs
 * @param label      la question telle que l'écran la pose
 * @param answerType la forme de réponse attendue
 */
public record SheetQuestion(String code, SheetSection section, DefineTopic topic, String label,
                            AnswerType answerType) {
}
