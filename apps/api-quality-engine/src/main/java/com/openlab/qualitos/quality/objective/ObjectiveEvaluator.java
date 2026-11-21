package com.openlab.qualitos.quality.objective;

import com.openlab.qualitos.quality.kpi.KpiDirection;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * La règle d'un objectif qualité, sans base de données ni horloge.
 *
 * <p>Isolée parce que c'est la seule partie dont une erreur ne se voit pas :
 * compter deux périodes au lieu de trois, ou lire « dépasse le seuil » dans le
 * mauvais sens sur un indicateur qu'on cherche à faire monter, produit un écran
 * parfaitement crédible et faux. Elle se teste donc sur des listes de nombres.
 *
 * <p><b>Le sens de « dépasser » dépend du KPI.</b> Pour un taux de rebut
 * ({@code LOWER_IS_BETTER}) dépasser un seuil c'est passer AU-DESSUS ; pour un
 * taux de livraison à l'heure ({@code HIGHER_IS_BETTER}) c'est passer EN
 * DESSOUS. Le sens est celui du KPI, jamais recopié sur l'objectif : un objectif
 * qui porterait le sien pourrait le contredire, et c'est la cible affichée qui
 * mentirait.
 *
 * <p><b>Les mesures sont attendues de la plus ancienne à la plus récente</b>, une
 * par période, et les comptages se font en partant de la FIN : « deux périodes
 * consécutives » veut dire les deux dernières, pas deux quelque part dans
 * l'historique.
 */
public final class ObjectiveEvaluator {

    private static final MathContext PRECISION = new MathContext(12, RoundingMode.HALF_UP);
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private ObjectiveEvaluator() {}

    /**
     * Confronte un objectif à ses mesures.
     *
     * @param objective l'objectif et ses seuils
     * @param direction le sens d'amélioration, repris du KPI
     * @param values    les valeurs par période, de la plus ancienne à la plus récente
     * @param heldLevel le niveau d'une CAPA acceptée encore ouverte sur cet objectif, ou
     *                  {@code null} s'il n'y en a pas. Renseigné, il empêche le retour à
     *                  « atteint » tant que le retour durable sous la cible n'est pas constaté.
     */
    public static ObjectiveAssessment evaluate(QualityObjective objective,
                                               KpiDirection direction,
                                               List<BigDecimal> values,
                                               ObjectiveLevel heldLevel) {
        if (objective == null) {
            throw new IllegalArgumentException("Un objectif est nécessaire pour l'apprécier");
        }
        if (direction == null) {
            throw new IllegalArgumentException(
                    "Le sens d'amélioration vient du KPI : sans lui, « dépasser le seuil » n'a pas de sens");
        }
        List<BigDecimal> mesures = values == null ? List.of()
                : values.stream().filter(java.util.Objects::nonNull).toList();
        if (mesures.isEmpty()) {
            return ObjectiveAssessment.notEvaluated();
        }

        BigDecimal target = objective.getTargetValue();
        BigDecimal observed = mesures.get(mesures.size() - 1);

        int warningBreaches = trailingCount(mesures,
                v -> breaches(direction, v, objective.getThresholdWarning()));
        int criticalBreaches = trailingCount(mesures,
                v -> breaches(direction, v, objective.getThresholdCritical()));
        int onTargetPeriods = trailingCount(mesures, v -> onTarget(direction, v, target));

        ObjectiveLevel triggered = null;
        if (criticalBreaches >= objective.getCriticalPeriods()) {
            triggered = ObjectiveLevel.CRITICAL;
        } else if (warningBreaches >= objective.getWarningPeriods()) {
            triggered = ObjectiveLevel.WARNING;
        }

        boolean recovered = onTargetPeriods >= objective.getRecoveryPeriods();

        ObjectiveStatus status = statusOf(direction, observed, target, criticalBreaches);
        // LE SOUVENIR DE L'ÉCART. Une CAPA acceptée est en cours : l'objectif ne
        // redevient « atteint » qu'au retour DURABLE sous la cible, pas au
        // premier relevé redevenu bon. C'est exactement le critère d'efficacité
        // que porte la CAPA — un seul chiffre pour les deux, parce que c'est la
        // même question posée deux fois.
        boolean held = false;
        if (heldLevel != null && !recovered) {
            status = pire(status, heldLevel.asStatus());
            held = true;
        }

        BigDecimal ecart = target == null ? null : observed.subtract(target);
        BigDecimal ecartPct = (ecart == null || target == null
                || target.compareTo(BigDecimal.ZERO) == 0)
                ? null
                : ecart.divide(target.abs(), PRECISION).multiply(CENT, PRECISION);

        return new ObjectiveAssessment(status, observed, ecart, ecartPct,
                warningBreaches, criticalBreaches, onTargetPeriods,
                triggered, recovered, held, mesures.size());
    }

    /**
     * Le statut lu sur la dernière valeur, avec la même sémantique que
     * {@code KpiEvaluator} : au-delà du critique c'est critique, du bon côté de
     * la cible c'est atteint, et entre les deux c'est une alerte.
     *
     * <p>Un dépassement du seuil critique vaut {@code CRITICAL} dès la première
     * période, même si la règle de déclenchement en demande davantage pour
     * proposer un dossier : l'écran doit montrer où on en est, pas où en est la
     * procédure.
     *
     * <p>Hors cible SANS avoir franchi le seuil d'alerte vaut quand même
     * {@code WARNING} : l'objectif n'est pas tenu. Le compter « atteint »
     * laisserait une revue conclure qu'un objectif manqué est atteint.
     */
    private static ObjectiveStatus statusOf(KpiDirection direction, BigDecimal observed,
                                            BigDecimal target, int criticalBreaches) {
        if (criticalBreaches >= 1) return ObjectiveStatus.CRITICAL;
        return onTarget(direction, observed, target)
                ? ObjectiveStatus.ON_TARGET
                : ObjectiveStatus.WARNING;
    }

    /** Le plus grave des deux statuts. */
    private static ObjectiveStatus pire(ObjectiveStatus a, ObjectiveStatus b) {
        return gravite(a) >= gravite(b) ? a : b;
    }

    private static int gravite(ObjectiveStatus s) {
        return switch (s) {
            case CRITICAL -> 3;
            case WARNING -> 2;
            case ON_TARGET -> 1;
            case NOT_EVALUATED -> 0;
        };
    }

    /**
     * La valeur est-elle du bon côté de la cible ?
     *
     * <p>La cible est INCLUSE : « ≤ 500 ppm » est atteint à 500 exactement. Un
     * objectif qu'on manque en l'atteignant pile serait incompréhensible devant
     * une revue.
     */
    static boolean onTarget(KpiDirection direction, BigDecimal value, BigDecimal target) {
        if (value == null || target == null) return false;
        return direction == KpiDirection.LOWER_IS_BETTER
                ? value.compareTo(target) <= 0
                : value.compareTo(target) >= 0;
    }

    /**
     * La valeur dépasse-t-elle ce seuil, dans le mauvais sens ?
     *
     * <p>Le seuil lui-même n'est PAS un dépassement : « dépasse le seuil
     * d'alerte » se lit strictement, comme le dit la maquette. Sans seuil
     * renseigné, il n'y a rien à dépasser.
     */
    static boolean breaches(KpiDirection direction, BigDecimal value, BigDecimal threshold) {
        if (value == null || threshold == null) return false;
        return direction == KpiDirection.LOWER_IS_BETTER
                ? value.compareTo(threshold) > 0
                : value.compareTo(threshold) < 0;
    }

    /** Combien de valeurs, en partant de la dernière, satisfont le prédicat sans interruption. */
    private static int trailingCount(List<BigDecimal> values,
                                     java.util.function.Predicate<BigDecimal> predicate) {
        int n = 0;
        for (int i = values.size() - 1; i >= 0; i--) {
            if (!predicate.test(values.get(i))) break;
            n++;
        }
        return n;
    }
}
