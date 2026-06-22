package com.openlab.qualitos.quality.academy.domain;

import java.util.List;

/**
 * Correcteur de quiz — règle pure et déterministe (§19.3, « quiz avec scoring et
 * seuil de réussite »).
 *
 * <p>Le score est le pourcentage de points obtenus sur le total des points des
 * questions : une réponse dont l'index correspond à {@link QuizQuestion#getCorrectIndex()}
 * crédite {@link QuizQuestion#getPoints()} points. Le résultat est arrondi à
 * l'entier le plus proche puis comparé au seuil {@link Quiz#getPassScore()}.</p>
 *
 * <p>Fonction pure : aucune dépendance, aucun effet de bord ; le même couple
 * (questions, réponses) donne toujours le même résultat — testable unitairement.</p>
 */
public final class QuizGrader {

    private QuizGrader() {}

    /** Résultat de correction : score 0-100 et réussite vis-à-vis du seuil. */
    public record Result(int score, boolean passed, int earnedPoints, int totalPoints) {}

    /**
     * Corrige une tentative.
     *
     * @param questions   questions du quiz (ordre indifférent ; chaque question
     *                    porte son index correct et ses points)
     * @param answers     index choisis par l'apprenant, dans le MÊME ordre que
     *                    {@code questions} ; une valeur hors bornes ou absente est
     *                    comptée comme fausse
     * @param passScore   seuil de réussite (0-100)
     * @return le score (0-100), la réussite, et le détail des points
     */
    public static Result grade(List<QuizQuestion> questions, List<Integer> answers, int passScore) {
        if (questions == null || questions.isEmpty()) {
            // Quiz sans question : convention — réussite triviale à 100 %.
            return new Result(100, 100 >= passScore, 0, 0);
        }
        int total = 0;
        int earned = 0;
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            total += q.getPoints();
            Integer chosen = (answers != null && i < answers.size()) ? answers.get(i) : null;
            if (chosen != null && chosen == q.getCorrectIndex()) {
                earned += q.getPoints();
            }
        }
        int score = total == 0 ? 100 : (int) Math.round((earned * 100.0) / total);
        return new Result(score, score >= passScore, earned, total);
    }
}
