package com.openlab.qualitos.quality.objective;

import com.openlab.qualitos.quality.kpi.KpiMeasurement;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Regroupe les relevés d'un KPI dans les périodes d'un objectif.
 *
 * <p><b>Pourquoi un découpage, et pas une lecture directe des relevés.</b> La
 * période d'un objectif (1, 3, 6 ou 12 mois) n'est pas la fréquence du KPI : un
 * indicateur relevé chaque mois peut servir un objectif revu au trimestre. Dire
 * « deux périodes consécutives au-delà du seuil » sur les relevés bruts
 * compterait alors deux MOIS là où l'objectif parle de deux TRIMESTRES, et
 * déclencherait une CAPA six fois trop tôt.
 *
 * <p><b>La valeur d'une période est la moyenne de ses relevés.</b> C'est le seul
 * agrégat défendable sans connaître la formule du KPI : sommer fausserait un
 * taux, prendre le dernier jetterait les autres. Dans le cas courant — un
 * indicateur mensuel et un objectif mensuel — chaque période contient un seul
 * relevé et la moyenne est ce relevé, sans aucune déformation.
 *
 * <p>Les fenêtres se comptent à REBOURS depuis le dernier relevé : c'est la
 * dernière période qui intéresse une revue, et c'est elle qui doit tomber juste.
 * Une période sans aucun relevé est ABSENTE de la série, et non comptée comme
 * zéro — un mois non mesuré n'est pas un mois à zéro.
 */
public final class ObjectivePeriodSeries {

    private static final MathContext PRECISION = new MathContext(12, RoundingMode.HALF_UP);

    private ObjectivePeriodSeries() {}

    /** Une période de l'objectif et la valeur qu'on y a relevée. */
    public record Period(Instant start, Instant end, BigDecimal value, int measurements) {}

    /**
     * Découpe les relevés en périodes de {@code periodMonths} mois, de la plus
     * ancienne à la plus récente.
     *
     * @param measurements les relevés, dans n'importe quel ordre
     * @param periodMonths la longueur d'une période, en mois
     * @param maxPeriods   le nombre maximum de périodes à retourner, en gardant les
     *                     plus récentes ; une revue n'a pas besoin de dix ans d'historique
     */
    public static List<Period> split(List<KpiMeasurement> measurements,
                                     int periodMonths, int maxPeriods) {
        if (measurements == null || measurements.isEmpty() || periodMonths < 1) {
            return List.of();
        }
        List<KpiMeasurement> tries = measurements.stream()
                .filter(m -> m.getPeriodStart() != null && m.getValue() != null)
                .sorted(Comparator.comparing(KpiMeasurement::getPeriodStart))
                .toList();
        if (tries.isEmpty()) return List.of();

        // La dernière fenêtre se termine juste après le dernier relevé, et les
        // autres se déduisent en remontant : c'est la période courante qui doit
        // tomber juste, pas une origine arbitraire.
        Instant dernier = tries.get(tries.size() - 1).getPeriodStart();
        Instant finCourante = dernier.atZone(ZoneOffset.UTC)
                .withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
                .plusMonths(1).toInstant();
        Instant premier = tries.get(0).getPeriodStart();

        List<Period> periodes = new ArrayList<>();
        Instant fin = finCourante;
        while (!fin.isBefore(premier) && periodes.size() < Math.max(1, maxPeriods)) {
            Instant debut = fin.atZone(ZoneOffset.UTC).minusMonths(periodMonths).toInstant();
            List<BigDecimal> dedans = new ArrayList<>();
            for (KpiMeasurement m : tries) {
                Instant t = m.getPeriodStart();
                if (!t.isBefore(debut) && t.isBefore(fin)) dedans.add(m.getValue());
            }
            // Une période sans relevé n'entre pas dans la série : la compter
            // romprait une suite de dépassements sur un simple trou de mesure,
            // ou la prolongerait sur un mois que personne n'a mesuré.
            if (!dedans.isEmpty()) {
                periodes.add(new Period(debut, fin, moyenne(dedans), dedans.size()));
            }
            fin = debut;
        }
        // Construites à rebours, rendues dans l'ordre chronologique : c'est
        // l'ordre que la règle de déclenchement attend.
        return periodes.reversed();
    }

    /** Les valeurs seules, dans l'ordre chronologique : ce que la règle consomme. */
    public static List<BigDecimal> values(List<Period> periods) {
        return periods.stream().map(Period::value).toList();
    }

    private static BigDecimal moyenne(List<BigDecimal> valeurs) {
        if (valeurs.size() == 1) return valeurs.get(0);
        BigDecimal somme = valeurs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return somme.divide(BigDecimal.valueOf(valeurs.size()), PRECISION);
    }
}
