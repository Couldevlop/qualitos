package com.openlab.qualitos.quality.capa.effectiveness.application;

import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EvaluationStage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Ce que l'API rend sur le plan de travail des évaluations d'efficacité (ADR 0108). */
public final class EffectivenessBoardDto {

    private EffectivenessBoardDto() {
    }

    /**
     * Une évaluation rendue telle qu'elle a été enregistrée.
     *
     * <p>Le critère est celui qui figurait sur le dossier au moment du jugement,
     * et non celui qui y figure aujourd'hui : c'est sur ce critère-là que
     * l'évaluation s'est prononcée.
     */
    public record Review(UUID id, EffectivenessVerdict verdict, String criterion,
                         String baseline, String target, String observed,
                         String comment, String reviewerName, Instant reviewedAt) {
    }

    /**
     * Une ligne du plan de travail.
     *
     * @param stage            calculé, jamais saisi : un état d'avancement qu'on
     *                         peut poser à la main se fixe le jour où quelqu'un y
     *                         pense et reste ensuite périmé
     * @param criterion        le critère actuellement posé sur le dossier ;
     *                         {@code null} veut dire qu'il n'a pas été écrit, et
     *                         c'est alors ce qui manque pour juger
     * @param lastReview       la dernière évaluation, ou {@code null}
     * @param reviewCount      le nombre d'évaluations déjà rendues. Deux et plus
     *                         disent un dossier qui a été repris — l'information
     *                         qu'un dossier « efficace » du premier coup ne donne
     *                         pas.
     * @param closureBlocked   la clôture est-elle encore empêchée par autre chose
     *                         que le jugement ? Permet à l'écran de dire quoi,
     *                         avant le clic plutôt qu'en 409 après.
     */
    /**
     * Une ligne du plan de travail.
     *
     * <p>{@code evaluable} est une COMPOSANTE et non une méthode dérivée, pour
     * une raison de sérialisation : Jackson ne publie que les composantes d'un
     * enregistrement, et une méthode {@code evaluable()} calculée n'aurait
     * jamais atteint l'écran. L'écran aurait alors refait la règle lui-même —
     * « l'étape vaut-elle AWAITING_EVALUATION ? » — et le jour où le domaine en
     * ajoute une, deux réponses existeraient, dont une fausse. La valeur est
     * calculée une fois, par {@code EvaluationStage#allowsEvaluation()}.
     */
    public record Row(UUID capaId, String reference, String title, String criticity,
                      String ownerName, EvaluationStage stage, boolean evaluable,
                      int actionsTotal, int actionsDone,
                      String criterion, Review lastReview, int reviewCount,
                      boolean closureBlocked, boolean containmentOnly, int openNcCount,
                      Instant updatedAt) {
    }

    /**
     * La synthèse — les quatre chiffres de l'en-tête.
     *
     * @param effectiveRatePercent part des dossiers JUGÉS dont le verdict est
     *                             « efficace », en pourcentage entier ;
     *                             {@code null} tant qu'aucun dossier n'a été
     *                             jugé. Un dossier en attente de jugement n'y
     *                             entre pas : l'y compter ferait baisser le taux
     *                             à chaque dossier ouvert, c'est-à-dire punirait
     *                             le fait de déclarer des écarts.
     * @param awaitingEvaluation   dossiers prêts à être jugés. C'est le seul
     *                             chiffre sur lequel on agit, et il est donc
     *                             rendu même quand il vaut zéro.
     * @param stillOpen            dossiers dont l'efficacité n'est pas démontrée
     * @param reopened             dossiers jugés inefficaces et rouverts
     * @param partial              dossiers en attente d'une action complémentaire
     * @param missingCriterion     dossiers prêts à être jugés mais sans critère
     *                             écrit. Un jugement sans critère annoncé revient
     *                             à choisir le critère en regardant le résultat ;
     *                             ce chiffre dit combien de dossiers y exposent.
     * @param truncated            vrai quand le périmètre dépassait la borne
     *                             d'affichage. Les INDICATEURS, eux, portent
     *                             toujours sur l'ensemble : une synthèse calculée
     *                             sur une page serait un mensonge par omission.
     */
    public record Summary(Integer effectiveRatePercent, int evaluated,
                          int awaitingEvaluation, int stillOpen,
                          int reopened, int partial, int missingCriterion,
                          int total, boolean truncated, List<Row> rows) {
    }
}
