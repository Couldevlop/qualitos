package com.openlab.qualitos.quality.dmaic.resolution.application;

import java.time.LocalDate;
import java.util.List;

/**
 * Le rapport d'action corrective client (feuille « Client Claim Rapport » du
 * classeur), déjà résolu en texte : le moteur de mise en page n'a plus aucune
 * règle métier à connaître.
 *
 * <p>Ce qu'il reprend de chaque phase est celui du modèle client : les trois
 * questions clés de « Définir », la synthèse des mesures, le plan temporaire et
 * les mesures conservatoires, le diagramme d'Ishikawa et les 5 pourquoi, les
 * solutions retenues et leur plan d'actions, enfin le plan de surveillance.
 *
 * @param define         les questions clés de « Définir » (problème, pièce ou lot, quantité)
 * @param containmentPlan la réponse au plan d'action temporaire de confinement
 * @param ishikawaCauses les causes du diagramme lié : catégorie → cause
 * @param bestSolutions  la réponse « meilleures solutions sélectionnées »
 * @param surveillance   la réponse « le plan de surveillance élimine-t-il la récurrence ? »
 */
public record ClaimReport(String clientName, String claimNumber, String projectName, String ownerName,
                          LocalDate openedOn, LocalDate closedOn, String severity, String failureType,
                          List<Answer> define, String measureSummary, String containmentPlan,
                          String containmentMeasures, List<Answer> ishikawaCauses, List<Why> whys,
                          String rootCause, String bestSolutions, List<Action> actions, String surveillance) {

    public ClaimReport {
        define = List.copyOf(define);
        ishikawaCauses = List.copyOf(ishikawaCauses);
        whys = List.copyOf(whys);
        actions = List.copyOf(actions);
    }

    /** Une question et sa réponse, déjà en clair. */
    public record Answer(String question, String answer) {
    }

    /** Une chaîne de 5 pourquoi : son axe, son constat, ses pourquoi, sa cause racine et son action. */
    public record Why(String branch, String cause, List<String> whys, boolean rootCause, String action) {
        public Why {
            whys = List.copyOf(whys);
        }
    }

    /** Une ligne du plan d'actions, telle que le client la lit. */
    public record Action(String action, String responsible, String status, LocalDate plannedOn,
                         LocalDate doneOn, int percent) {
    }
}
