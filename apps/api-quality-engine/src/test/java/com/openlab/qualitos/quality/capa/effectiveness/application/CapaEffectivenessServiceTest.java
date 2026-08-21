package com.openlab.qualitos.quality.capa.effectiveness.application;

import com.openlab.qualitos.quality.capa.effectiveness.domain.MeasurementStatus;
import com.openlab.qualitos.quality.capa.effectiveness.domain.RecurrenceSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La lecture d'efficacité.
 *
 * <p>Ce banc protège surtout des choix de FENÊTRE, qui décident du résultat sans
 * se voir : la période « avant » s'arrête à l'ouverture du dossier et non à sa
 * clôture — les non-conformités survenues pendant le traitement ont motivé
 * l'action, elles ne sont pas son échec — et la période « après » s'arrête à
 * aujourd'hui tant que la fenêtre n'est pas écoulée, sans quoi on compterait des
 * récidives dans un futur qui n'a pas eu lieu.
 */
class CapaEffectivenessServiceTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PRODUIT = UUID.randomUUID();
    static final UUID MODE = UUID.randomUUID();
    static final Instant MAINTENANT = Instant.parse("2026-08-21T12:00:00Z");

    ClosedCapaPort closedCapas;
    NcOccurrencePort occurrences;
    TenantProvider tenants;
    CapaEffectivenessService service;

    @BeforeEach
    void setUp() {
        closedCapas = mock(ClosedCapaPort.class);
        occurrences = mock(NcOccurrencePort.class);
        tenants = mock(TenantProvider.class);
        when(tenants.requireTenantId()).thenReturn(TENANT);
        service = new CapaEffectivenessService(closedCapas, occurrences, tenants,
                Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    }

    @Test
    void itMeasuresAClosedCapaAgainstItsOwnHistory() {
        donne(capa("Dérive dimensionnelle", jours(-400), jours(-300)));
        when(occurrences.countBetween(any(), any(), any(), any())).thenReturn(4, 1);

        CapaEffectivenessDto.Summary summary = service.measure(6);

        assertThat(summary.rows()).hasSize(1);
        CapaEffectivenessDto.Row row = summary.rows().get(0);
        assertThat(row.status()).isEqualTo(MeasurementStatus.MEASURED);
        assertThat(row.occurrencesBefore()).isEqualTo(4);
        assertThat(row.occurrencesAfter()).isEqualTo(1);
        assertThat(row.ratePercent()).isEqualTo(75);
        assertThat(summary.averageRatePercent()).isEqualTo(75);
    }

    @Test
    void theWindowBeforeStopsAtTheOpeningOfTheCase() {
        Instant ouverture = jours(-400);
        donne(capa("Dérive dimensionnelle", ouverture, jours(-300)));
        when(occurrences.countBetween(any(), any(), any(), any())).thenReturn(2, 0);

        service.measure(6);

        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(occurrences, org.mockito.Mockito.times(2))
                .countBetween(eq(TENANT), any(), from.capture(), to.capture());

        assertThat(to.getAllValues().get(0)).isEqualTo(ouverture);
        assertThat(from.getAllValues().get(0)).isEqualTo(ouverture.minus(180, ChronoUnit.DAYS));
    }

    @Test
    void theWindowAfterNeverReachesIntoTheFuture() {
        Instant cloture = jours(-30);
        donne(capa("Dérive dimensionnelle", jours(-200), cloture));
        when(occurrences.countBetween(any(), any(), any(), any())).thenReturn(3, 0);

        service.measure(6);

        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(occurrences, org.mockito.Mockito.times(2))
                .countBetween(any(), any(), any(), to.capture());

        // La fenêtre irait jusqu'à cloture + 180 jours, bien après aujourd'hui.
        assertThat(to.getAllValues().get(1)).isEqualTo(MAINTENANT);
    }

    @Test
    void aCaseClosedTooRecentlyIsAnnouncedAsUnderObservation() {
        donne(capa("Dérive dimensionnelle", jours(-200), jours(-30)));
        when(occurrences.countBetween(any(), any(), any(), any())).thenReturn(3, 0);

        CapaEffectivenessDto.Summary summary = service.measure(6);

        assertThat(summary.rows().get(0).status()).isEqualTo(MeasurementStatus.IN_OBSERVATION);
        assertThat(summary.rows().get(0).ratePercent()).isNull();
        assertThat(summary.inObservation()).isEqualTo(1);
        assertThat(summary.averageRatePercent()).isNull();
    }

    @Test
    void aCaseWithoutAnySignatureIsLeftOutRatherThanGivenImaginaryRecurrences() {
        donne(new ClosedCapaPort.ClosedCapa(UUID.randomUUID(), "Audit interne",
                "MAJOR", jours(-400), jours(-300), null, RecurrenceSignature.NONE));

        CapaEffectivenessDto.Summary summary = service.measure(6);

        assertThat(summary.rows()).isEmpty();
        verify(occurrences, never()).countBetween(any(), any(), any(), any());
    }

    @Test
    void aCaseWithoutAClosureDateIsLeftOut() {
        donne(new ClosedCapaPort.ClosedCapa(UUID.randomUUID(), "En cours", "MAJOR",
                jours(-400), null, null, RecurrenceSignature.precise(PRODUIT, MODE)));

        assertThat(service.measure(6).rows()).isEmpty();
    }

    @Test
    void aRateFoundedOnTheCategoryAloneIsFlaggedAsApproximate() {
        donne(new ClosedCapaPort.ClosedCapa(UUID.randomUUID(), "Défaut process",
                "MINOR", jours(-400), jours(-300), null, RecurrenceSignature.byCategory("PROCESS")));
        when(occurrences.countBetween(any(), any(), any(), any())).thenReturn(4, 2);

        CapaEffectivenessDto.Row row = service.measure(6).rows().get(0);

        assertThat(row.preciseMatch()).isFalse();
        assertThat(row.ratePercent()).isEqualTo(50);
    }

    @Test
    void theAverageIgnoresWhatCannotBeMeasured() {
        donne(capa("Dérive dimensionnelle", jours(-400), jours(-300)),
              capa("Erreur étiquetage", jours(-200), jours(-30)));
        // 1er dossier : 4 avant / 1 après = 75 %. 2e : en observation.
        when(occurrences.countBetween(any(), any(), any(), any())).thenReturn(4, 1, 5, 0);

        CapaEffectivenessDto.Summary summary = service.measure(6);

        assertThat(summary.measured()).isEqualTo(1);
        assertThat(summary.inObservation()).isEqualTo(1);
        assertThat(summary.averageRatePercent()).isEqualTo(75);
    }

    @Test
    void aCaseDeclaredEffectiveButFollowedByAsManyDefectsIsCounted() {
        donne(new ClosedCapaPort.ClosedCapa(UUID.randomUUID(), "Étiquetage",
                "MAJOR", jours(-400), jours(-300), Boolean.TRUE,
                RecurrenceSignature.precise(PRODUIT, MODE)));
        when(occurrences.countBetween(any(), any(), any(), any())).thenReturn(2, 3);

        CapaEffectivenessDto.Summary summary = service.measure(6);

        assertThat(summary.declaredButFailed()).isEqualTo(1);
        assertThat(summary.aggravated()).isEqualTo(1);
        assertThat(summary.rows().get(0).ratePercent()).isZero();
        assertThat(summary.rows().get(0).declaredEffective()).isTrue();
    }

    @Test
    void whatCallsForADecisionComesFirst() {
        donne(capa("Dossier efficace", jours(-400), jours(-300)),
              capa("Dossier aggravé", jours(-400), jours(-300)));
        // BON : 4 → 0 (100 %). PIRE : 2 → 5 (aggravé).
        when(occurrences.countBetween(any(), any(), any(), any())).thenReturn(4, 0, 2, 5);

        CapaEffectivenessDto.Summary summary = service.measure(6);

        assertThat(summary.rows().get(0).title()).isEqualTo("Dossier aggravé");
        assertThat(summary.rows().get(1).title()).isEqualTo("Dossier efficace");
    }

    @Test
    void anEmptyPerimeterYieldsAnEmptySummaryAndNoAverage() {
        donne();

        CapaEffectivenessDto.Summary summary = service.measure(6);

        assertThat(summary.rows()).isEmpty();
        assertThat(summary.averageRatePercent()).isNull();
        assertThat(summary.windowMonths()).isEqualTo(6);
    }

    @Test
    void theTenantAlwaysComesFromTheSecurityContext() {
        donne(capa("Dérive dimensionnelle", jours(-400), jours(-300)));
        when(occurrences.countBetween(any(), any(), any(), any())).thenReturn(1, 0);

        service.measure(6);

        verify(tenants).requireTenantId();
        verify(closedCapas).findClosed(TENANT);
        verify(occurrences, org.mockito.Mockito.atLeastOnce())
                .countBetween(eq(TENANT), any(), any(), any());
    }

    @Test
    void anAbsurdWindowIsRefusedBeforeAnyQuery() {
        assertThatThrownBy(() -> service.measure(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.measure(99))
                .isInstanceOf(IllegalArgumentException.class);

        verify(closedCapas, never()).findClosed(any());
    }

    // ---------- montage ----------

    private void donne(ClosedCapaPort.ClosedCapa... capas) {
        when(closedCapas.findClosed(TENANT)).thenReturn(List.of(capas));
    }

    private ClosedCapaPort.ClosedCapa capa(String titre, Instant ouverture, Instant cloture) {
        return new ClosedCapaPort.ClosedCapa(UUID.randomUUID(), titre,
                "MAJOR", ouverture, cloture, null, RecurrenceSignature.precise(PRODUIT, MODE));
    }

    private static Instant jours(int decalage) {
        return MAINTENANT.plus(decalage, ChronoUnit.DAYS);
    }
}
