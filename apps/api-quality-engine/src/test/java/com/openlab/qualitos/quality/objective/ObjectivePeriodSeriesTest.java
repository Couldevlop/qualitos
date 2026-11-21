package com.openlab.qualitos.quality.objective;

import com.openlab.qualitos.quality.kpi.KpiMeasurement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Les relevés d'un KPI regroupés dans les périodes d'un objectif (ADR 0133)")
class ObjectivePeriodSeriesTest {

    private static KpiMeasurement releve(String mois, String valeur) {
        KpiMeasurement m = new KpiMeasurement();
        m.setPeriodStart(LocalDate.parse(mois + "-01").atStartOfDay(ZoneOffset.UTC).toInstant());
        m.setValue(valeur == null ? null : new BigDecimal(valeur));
        return m;
    }

    private static List<String> valeurs(List<ObjectivePeriodSeries.Period> p) {
        return ObjectivePeriodSeries.values(p).stream()
                .map(v -> v.stripTrailingZeros().toPlainString()).toList();
    }

    @Test
    void sansReleve_aucunePeriode() {
        assertThat(ObjectivePeriodSeries.split(List.of(), 1, 24)).isEmpty();
        assertThat(ObjectivePeriodSeries.split(null, 1, 24)).isEmpty();
    }

    @Test
    void periodeNonPositive_aucunePeriode() {
        assertThat(ObjectivePeriodSeries.split(List.of(releve("2026-10", "7")), 0, 24)).isEmpty();
    }

    @Test
    void relevesSansDateOuSansValeur_ignores() {
        KpiMeasurement sansDate = new KpiMeasurement();
        sansDate.setValue(new BigDecimal("1"));
        assertThat(ObjectivePeriodSeries.split(
                Arrays.asList(sansDate, releve("2026-10", null)), 1, 24)).isEmpty();
    }

    @Test
    void objectifMensuel_unRelevePaMois_chaquePeriodePorteSonRelevéSansDeformation() {
        List<ObjectivePeriodSeries.Period> p = ObjectivePeriodSeries.split(List.of(
                releve("2026-08", "700"), releve("2026-09", "650"), releve("2026-10", "720")),
                1, 24);
        assertThat(valeurs(p)).containsExactly("700", "650", "720");
        assertThat(p).allSatisfy(x -> assertThat(x.measurements()).isEqualTo(1));
    }

    @Test
    void lOrdreDEntreeNImportePas_laSortieEstChronologique() {
        List<KpiMeasurement> desordre = new ArrayList<>(List.of(
                releve("2026-10", "720"), releve("2026-08", "700"), releve("2026-09", "650")));
        assertThat(valeurs(ObjectivePeriodSeries.split(desordre, 1, 24)))
                .containsExactly("700", "650", "720");
    }

    @Test
    void objectifTrimestriel_surUnKpiMensuel_laPeriodeVautLaMoyenneDeSesReleves() {
        // Trois mois dans une période : 600, 700, 800 → 700. Compter les mois
        // comme des trimestres déclencherait une CAPA trois fois trop tôt.
        List<ObjectivePeriodSeries.Period> p = ObjectivePeriodSeries.split(List.of(
                releve("2026-08", "600"), releve("2026-09", "700"), releve("2026-10", "800")),
                3, 24);
        assertThat(p).hasSize(1);
        assertThat(p.get(0).measurements()).isEqualTo(3);
        assertThat(p.get(0).value()).isEqualByComparingTo("700");
    }

    @Test
    void laDernierePeriodeSeTermineJusteApresLeDernierReleve() {
        List<ObjectivePeriodSeries.Period> p = ObjectivePeriodSeries.split(
                List.of(releve("2026-10", "720")), 1, 24);
        assertThat(p).hasSize(1);
        assertThat(p.get(0).start())
                .isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
        assertThat(p.get(0).end())
                .isEqualTo(Instant.parse("2026-11-01T00:00:00Z"));
    }

    @Test
    void unMoisNonMesure_estAbsentDeLaSerie_etNonCompteCommeZero() {
        // Août et octobre mesurés, septembre non. Trois périodes seraient
        // fausses : un mois non mesuré n'est pas un mois à zéro, et le compter
        // romprait une suite de dépassements sur un simple trou de mesure.
        List<ObjectivePeriodSeries.Period> p = ObjectivePeriodSeries.split(List.of(
                releve("2026-08", "700"), releve("2026-10", "720")), 1, 24);
        assertThat(valeurs(p)).containsExactly("700", "720");
    }

    @Test
    void auDelaDuPlafond_seulesLesPeriodesLesPlusRecentesSontRendues() {
        List<KpiMeasurement> douze = new ArrayList<>();
        for (int mois = 1; mois <= 12; mois++) {
            douze.add(releve(String.format("2026-%02d", mois), String.valueOf(mois)));
        }
        assertThat(valeurs(ObjectivePeriodSeries.split(douze, 1, 3)))
                .containsExactly("10", "11", "12");
    }

    @Test
    void plafondNul_aumoinsUnePeriodeEstRendue() {
        assertThat(ObjectivePeriodSeries.split(List.of(releve("2026-10", "720")), 1, 0))
                .hasSize(1);
    }

    @Test
    void objectifAnnuel_douzeMoisTiennentDansUnePeriode() {
        List<KpiMeasurement> douze = new ArrayList<>();
        for (int mois = 1; mois <= 12; mois++) {
            douze.add(releve(String.format("2026-%02d", mois), "100"));
        }
        List<ObjectivePeriodSeries.Period> p = ObjectivePeriodSeries.split(douze, 12, 24);
        assertThat(p).hasSize(1);
        assertThat(p.get(0).measurements()).isEqualTo(12);
        assertThat(p.get(0).value()).isEqualByComparingTo("100");
    }
}
