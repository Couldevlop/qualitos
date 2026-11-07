package com.openlab.qualitos.quality.capa.effectiveness.domain;

/**
 * Où en est un dossier CAPA du point de vue de l'évaluation d'efficacité
 * (ADR 0108).
 *
 * <p><b>Jamais saisi, toujours calculé.</b> Un état d'avancement qu'on peut
 * poser à la main finit par mentir : il se fixe le jour où quelqu'un y pense, et
 * reste ensuite en place pendant que les actions, elles, avancent. Ici il se
 * déduit de trois faits vérifiables — le statut du dossier, ses actions
 * réalisées, et le dernier verdict rendu — et il ne peut donc pas être périmé.
 *
 * <p>C'est aussi ce qui permet à l'écran d'annoncer le verrou <em>avant</em> le
 * clic plutôt qu'un refus après : {@link #AWAITING_EVALUATION} est exactement la
 * condition dans laquelle le formulaire s'ouvre, et c'est la même que celle que
 * le service vérifiera.
 */
public enum EvaluationStage {

    /**
     * Des actions restent à mener, ou le dossier n'a pas encore été déclaré
     * résolu. L'évaluation est verrouillée : juger l'effet d'une action qu'on
     * n'a pas menée n'a aucun sens.
     */
    ACTIONS_IN_PROGRESS,

    /**
     * Dossier résolu, toutes ses actions réalisées. C'est le travail en attente,
     * et le seul chiffre de cet écran sur lequel on agit.
     */
    AWAITING_EVALUATION,

    /** Jugé efficace : le critère est atteint, le dossier est clos. */
    CLOSED_EFFECTIVE,

    /**
     * Jugé partiellement efficace : une action complémentaire est attendue, et
     * le dossier est reparti en traitement.
     *
     * <p>Distinct de {@link #REOPENED}, et pas par souci de nuance : ici la
     * cause était la bonne et l'action ne va pas assez loin ; là, c'est la cause
     * qu'il faut reprendre. Deux travaux différents, confiés à deux moments
     * différents du raisonnement.
     *
     * <p>Cet état n'est pas définitif : dès que l'action complémentaire est
     * réalisée et le dossier de nouveau résolu, il repasse en
     * {@link #AWAITING_EVALUATION}. C'est pourquoi le statut du dossier compte
     * autant que le verdict — sans lui, un dossier qui a repris sa marche
     * resterait affiché comme en attente d'une action déjà faite.
     */
    COMPLEMENTARY_ACTION_EXPECTED,

    /** Jugé inefficace : le dossier est rouvert, l'analyse des causes est à refaire. */
    REOPENED;

    /**
     * L'état d'un dossier, déduit de son statut, de ses actions et de son
     * dernier verdict.
     *
     * <p>L'ordre de lecture n'est pas indifférent. Le statut tranche d'abord :
     * clos, le dossier l'est ; résolu, il attend un jugement. Ce n'est que
     * lorsqu'il est reparti en traitement que le dernier verdict dit
     * <em>pourquoi</em> il y est reparti — et cette raison-là est l'information
     * que l'écran doit montrer, parce qu'elle désigne le travail à faire.
     *
     * @param closed       le dossier est-il clos ?
     * @param resolved     le dossier est-il déclaré résolu (en attente de
     *                     vérification) ?
     * @param lastVerdict  le dernier verdict rendu, ou {@code null} si aucune
     *                     évaluation n'a eu lieu
     * @param actionsTotal nombre d'actions portées par le dossier
     * @param actionsDone  nombre d'actions réalisées
     */
    public static EvaluationStage of(boolean closed, boolean resolved,
                                     EffectivenessVerdict lastVerdict,
                                     int actionsTotal, int actionsDone) {
        if (actionsTotal < 0 || actionsDone < 0 || actionsDone > actionsTotal) {
            throw new IllegalArgumentException(
                    "Décompte d'actions incohérent : " + actionsDone + " / " + actionsTotal);
        }
        if (closed && resolved) {
            throw new IllegalArgumentException("Un dossier ne peut être à la fois clos et résolu");
        }
        if (closed) {
            return CLOSED_EFFECTIVE;
        }
        if (resolved) {
            // Un dossier SANS action n'est pas « prêt à évaluer » : il n'y a
            // rien dont on puisse vérifier l'efficacité. Et une action ajoutée
            // APRÈS la résolution reverrouille le formulaire — c'est le même
            // raisonnement que ClosureBlockerCode.ACTIONS_NOT_DONE, et la même
            // réponse.
            boolean allDone = actionsTotal > 0 && actionsDone == actionsTotal;
            return allDone ? AWAITING_EVALUATION : ACTIONS_IN_PROGRESS;
        }
        if (lastVerdict == null) {
            return ACTIONS_IN_PROGRESS;
        }
        return switch (lastVerdict) {
            // Un verdict d'efficacité sur un dossier qui n'est plus clos : le
            // dossier a été repris après sa clôture. Le travail en cours primant
            // sur le jugement passé, on le lit comme tel.
            case EFFECTIVE -> ACTIONS_IN_PROGRESS;
            case PARTIAL -> COMPLEMENTARY_ACTION_EXPECTED;
            case INEFFECTIVE -> REOPENED;
        };
    }

    /**
     * Le formulaire d'évaluation est-il ouvert dans cet état ?
     *
     * <p>Un seul état l'ouvre, et c'est volontaire. Un dossier clos n'a plus à
     * être jugé ; un dossier reparti en traitement doit d'abord être résolu de
     * nouveau — sans quoi on jugerait l'action complémentaire avant de l'avoir
     * menée, ce qui est précisément ce que le verrou empêche.
     */
    public boolean allowsEvaluation() {
        return this == AWAITING_EVALUATION;
    }

    /** Le dossier compte-t-il encore parmi les sujets ouverts ? */
    public boolean stillOpen() {
        return this != CLOSED_EFFECTIVE;
    }
}
