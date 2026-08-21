package com.openlab.qualitos.quality.capa.effectiveness.application;

import com.openlab.qualitos.quality.capa.effectiveness.domain.CapaEffectiveness;
import com.openlab.qualitos.quality.capa.effectiveness.domain.MeasurementStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Ce que l'API rend sur l'efficacité des CAPA closes. */
public final class CapaEffectivenessDto {

    private CapaEffectivenessDto() {
    }

    /**
     * Une ligne du tableau.
     *
     * @param declaredEffective ce que le responsable avait coché à la clôture.
     *                          Rendu à côté du taux mesuré, et volontairement :
     *                          l'écart entre l'opinion et le terrain est
     *                          l'information la plus intéressante de l'écran.
     * @param preciseMatch      vrai quand la récidive se reconnaît au mode de
     *                          défaillance ; faux quand elle se devine à la
     *                          seule catégorie, ce qui rend le taux indicatif
     */
    public record Row(UUID capaId, String title, String criticity,
                      Instant closedAt, MeasurementStatus status,
                      int occurrencesBefore, int occurrencesAfter,
                      Integer ratePercent, boolean aggravated,
                      int daysObserved, int daysInWindow,
                      Boolean declaredEffective, boolean preciseMatch) {

        static Row of(ClosedCapaPort.ClosedCapa capa, CapaEffectiveness measure, boolean precise) {
            return new Row(capa.id(), capa.title(), capa.criticity(),
                    measure.closedAt(), measure.status(),
                    measure.occurrencesBefore(), measure.occurrencesAfter(),
                    measure.ratePercent(), measure.aggravated(),
                    measure.daysObserved(), measure.daysInWindow(),
                    capa.effectivenessVerified(), precise);
        }
    }

    /**
     * La synthèse.
     *
     * @param averageRatePercent moyenne des seuls dossiers MESURÉS ; {@code null}
     *                           s'il n'y en a aucun. Y mêler les dossiers en
     *                           observation ferait bouger la moyenne au rythme
     *                           des clôtures plutôt qu'à celui des résultats.
     * @param declaredButFailed  dossiers déclarés efficaces à la clôture dont la
     *                           mesure dit le contraire. C'est le chiffre qu'un
     *                           auditeur cherchera.
     */
    public record Summary(int windowMonths, int measured, int inObservation, int notMeasurable,
                          Integer averageRatePercent, int aggravated, int declaredButFailed,
                          List<Row> rows) {
    }
}
