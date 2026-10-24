package com.openlab.qualitos.quality.dmaic.resolution.application;

import com.openlab.qualitos.quality.dmaic.resolution.domain.DefineTopic;

import java.time.LocalDate;
import java.util.List;

/**
 * Le rapport d'action corrective client (feuille « Client Claim Rapport » du
 * classeur), déjà résolu en texte : le moteur de mise en page n'a plus aucune
 * règle métier à connaître.
 *
 * @param definition les réponses de la phase « Définir », rubrique par rubrique
 */
public record ClaimReport(String clientName, String claimNumber, String projectName, String ownerName,
                          LocalDate openedOn, LocalDate closedOn, String severity, String failureType,
                          String customerSymptoms, List<Topic> definition, String problemStatement,
                          String measureSummary, String containmentMeasures, String rootCause,
                          String retainedSolutions, String surveillancePlan) {

    public ClaimReport {
        definition = List.copyOf(definition);
    }

    /** Une rubrique (Quoi, Qui…) et ses questions répondues. */
    public record Topic(DefineTopic topic, List<Answer> answers) {
        public Topic {
            answers = List.copyOf(answers);
        }
    }

    public record Answer(String question, String answer) {
    }
}
