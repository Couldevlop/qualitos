package com.openlab.qualitos.quality.dmaic.resolution.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ce que porte la fiche de résolution, feuille par feuille du classeur :
 * l'en-tête (client, réclamation, propriétaire, équipe, dates, sévérité, type
 * de défaillance), les réponses aux questions de chaque phase, les synthèses
 * que reprend le rapport client, l'analyse 5 pourquoi, le plan d'actions et le
 * diagramme d'Ishikawa lié.
 *
 * <p>L'énoncé du problème n'est pas ici : c'est celui du projet DMAIC, une seule
 * vérité pour la charte et le rapport.
 *
 * @param answers code de question → réponse (texte, ou code {@code YES}/{@code NO}…)
 */
public record ResolutionContent(String clientName, String claimNumber, Person problemOwner, List<Person> team,
                                LocalDate openedOn, LocalDate closedOn, Integer severity, String failureTypeCode,
                                String customerSymptoms, Map<String, String> answers, String measureSummary,
                                String containmentMeasures, String rootCause, String retainedSolutions,
                                String surveillancePlan, List<WhyChain> whys, List<ActionLine> actions,
                                UUID ishikawaId) {

    /**
     * Copies non modifiables qui TOLÈRENT un élément nul : une saisie mal formée
     * doit arriver jusqu'aux règles de la fiche, qui la refusent avec le nom du
     * champ, au lieu de tomber ici sur une erreur sans explication. Une réponse
     * nulle vaut « pas de réponse ».
     */
    public ResolutionContent {
        team = copy(team);
        answers = answers == null ? Map.of() : answers.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        whys = copy(whys);
        actions = copy(actions);
    }

    private static <T> List<T> copy(List<T> list) {
        return list == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(list));
    }

    /** La fiche d'un projet qu'on n'a pas encore remplie. */
    public static ResolutionContent empty() {
        return new ResolutionContent(null, null, null, List.of(), null, null, null, null, null, Map.of(),
                null, null, null, null, null, List.of(), List.of(), null);
    }
}
