package com.openlab.qualitos.quality.capa.effectiveness.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Mesure l'efficacité d'une CAPA close en confrontant deux périodes de même
 * durée : celle qui a précédé l'ouverture du dossier, et celle qui a suivi sa
 * clôture.
 *
 * <p><b>Pourquoi ce calcul et pas la case « efficacité vérifiée » du dossier :</b>
 * cette case dit qu'un responsable a estimé l'action efficace. C'est une opinion,
 * datée du jour de la clôture, portée par la personne qui a mené l'action. Ici,
 * c'est le terrain qui répond, et il répond plus tard.
 *
 * <p><b>La formule, en toutes lettres</b> (CLAUDE.md §18.2 #8 : aucun indicateur
 * sans définition explicite) :
 *
 * <pre>
 *   taux = 1 − (récidives après clôture ÷ occurrences avant ouverture)
 * </pre>
 *
 * borné à l'intervalle [0, 1], exprimé en pourcentage entier, arrondi au plus
 * proche. Les deux décomptes portent sur des fenêtres de durée identique.
 *
 * <p><b>Ce que le calcul refuse de dire</b>, et c'est le plus important :
 * sans occurrence antérieure, il n'y a pas de réduction à mesurer ; et tant que
 * la fenêtre d'observation n'est pas écoulée, comparer une période partielle à
 * une période entière flatterait systématiquement le résultat. Dans ces deux cas
 * le taux est absent — les décomptes, eux, restent rendus.
 */
public final class EffectivenessCalculator {

    private EffectivenessCalculator() {
    }

    /**
     * @param closedAt          date de clôture du dossier ; obligatoire
     * @param now               instant de la mesure
     * @param window            durée d'observation, appliquée des deux côtés
     * @param occurrencesBefore occurrences sur la fenêtre précédant l'ouverture
     * @param occurrencesAfter  récidives depuis la clôture
     */
    public static CapaEffectiveness evaluate(Instant closedAt, Instant now,
                                             EffectivenessWindow window,
                                             int occurrencesBefore, int occurrencesAfter) {
        if (closedAt == null) {
            throw new IllegalArgumentException(
                    "L'efficacité ne se mesure que sur une CAPA clôturée");
        }
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(window, "window");
        if (occurrencesBefore < 0 || occurrencesAfter < 0) {
            throw new IllegalArgumentException(
                    "Décompte d'occurrences négatif : " + occurrencesBefore + " / " + occurrencesAfter);
        }
        if (now.isBefore(closedAt)) {
            // Horloge qui recule ou clôture antidatée. Refuser vaut mieux que
            // rendre une durée d'observation négative que personne ne verrait.
            throw new IllegalArgumentException(
                    "Date de mesure antérieure à la clôture du dossier");
        }

        int elapsed = (int) Duration.between(closedAt, now).toDays();
        int daysObserved = Math.min(elapsed, window.days());
        boolean windowElapsed = elapsed >= window.days();

        if (occurrencesBefore == 0) {
            return new CapaEffectiveness(closedAt, MeasurementStatus.NOT_MEASURABLE,
                    0, occurrencesAfter, null, false, daysObserved, window.days());
        }
        if (!windowElapsed) {
            return new CapaEffectiveness(closedAt, MeasurementStatus.IN_OBSERVATION,
                    occurrencesBefore, occurrencesAfter, null,
                    occurrencesAfter > occurrencesBefore, daysObserved, window.days());
        }

        int rate = ratePercent(occurrencesBefore, occurrencesAfter);
        return new CapaEffectiveness(closedAt, MeasurementStatus.MEASURED,
                occurrencesBefore, occurrencesAfter, rate,
                occurrencesAfter > occurrencesBefore, daysObserved, window.days());
    }

    /**
     * Le plancher à zéro n'est pas une pudeur : un taux négatif se moyennerait
     * avec les autres dossiers et masquerait deux CAPA correctes sous une seule
     * mauvaise. L'aggravation reste visible — décompte des récidives et drapeau
     * dédié — là où elle se lit, dossier par dossier.
     *
     * <p>Arrondi au plus proche, et non tronqué : tronquer retirerait un point à
     * presque chaque dossier, toujours dans le même sens.
     */
    private static int ratePercent(int before, int after) {
        if (after >= before) {
            return 0;
        }
        double reduction = 1.0 - ((double) after / (double) before);
        return (int) Math.round(reduction * 100.0);
    }
}
