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
}
