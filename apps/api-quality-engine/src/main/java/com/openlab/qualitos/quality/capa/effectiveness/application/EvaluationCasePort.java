package com.openlab.qualitos.quality.capa.effectiveness.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Port — les dossiers CAPA concernés par l'évaluation d'efficacité, avec de
 * quoi calculer leur état sans y revenir (ADR 0108).
 *
 * <p>Les décomptes d'actions sont résolus par l'adaptateur, au plus près des
 * données. Les faire remonter avec le dossier évite la question posée une fois
 * par ligne du tableau, qui est la façon la plus courante de transformer
 * l'affichage d'un écran en centaines de requêtes.
 */
public interface EvaluationCasePort {

    /**
     * Les dossiers d'un tenant qui entrent dans la boucle d'efficacité — tous
     * sauf les dossiers rejetés, qui n'ont jamais été instruits et dont il n'y a
     * donc rien à juger.
     *
     * <p>Non paginée, comme la mesure par récidive et pour la même raison : les
     * indicateurs portent sur l'ensemble, et un taux calculé sur une page
     * n'aurait aucun sens. Le service borne ensuite ce qu'il AFFICHE, et le dit.
     */
    List<EvaluationCase> findInScope(UUID tenantId);

    /**
     * @param reference      la référence de l'origine ({@code sourceRef}) : la
     *                       non-conformité, le constat d'audit ou le risque qui a
     *                       ouvert le dossier. {@code null} pour un dossier
     *                       ouvert sans origine référencée.
     * @param criterion      le critère d'efficacité posé sur le dossier, ou
     *                       {@code null} s'il n'a pas encore été écrit — c'est
     *                       alors ce qui manque pour pouvoir juger.
     * @param closed         dossier clos
     * @param resolved       dossier déclaré résolu, en attente de vérification
     * @param openNcCount    non-conformités rattachées encore ouvertes : elles
     *                       interdisent la clôture, et l'écran doit pouvoir le
     *                       dire avant le clic
     * @param containmentOnly le dossier ne porte que des mesures d'endiguement :
     *                       elles arrêtent l'effet sans supprimer la cause, et
     *                       interdisent donc elles aussi la clôture
     */
    record EvaluationCase(UUID id, String reference, String title, String criticity,
                          String ownerName, String criterion,
                          boolean closed, boolean resolved,
                          int actionsTotal, int actionsDone,
                          boolean containmentOnly, int openNcCount,
                          Instant dueAt, Instant updatedAt) {
    }
}
