package com.openlab.qualitos.quality.dmaic;

import java.util.List;
import java.util.UUID;

/**
 * Ce qui manque à une phase pour qu'on passe à la suivante : une phase dont le
 * questionnaire n'est pas rempli bloque l'avancement. Implémenté par la fiche
 * de résolution, qui porte ce questionnaire.
 */
public interface DmaicPhaseGate {

    /** Les libellés des questions et synthèses encore sans réponse ; vide quand la phase est complète. */
    List<String> missing(UUID projectId, DmaicPhase phase);

    /**
     * Ce qui empêche de CLORE le projet, en clair.
     *
     * <p>Clore n'est pas avancer d'une phase de plus : c'est déclarer le problème
     * résolu. Il y faut donc TOUTE la fiche renseignée — pas seulement la phase
     * en cours — et toutes les actions réellement faites, c'est-à-dire à 100 %.
     * Un projet clos avec un plan d'actions à 60 % affirme une résolution que
     * rien ne soutient, et c'est précisément ce qu'un auditeur vient vérifier.
     *
     * @return les raisons, vide quand le projet peut être clos
     */
    List<String> blockingCompletion(UUID projectId);
}
