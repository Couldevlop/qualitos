package com.openlab.qualitos.core.billing;

import static com.openlab.qualitos.core.billing.BillingPeriod.ANNUAL;
import static com.openlab.qualitos.core.billing.BillingPeriod.MONTHLY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BillingPeriodTest {

    @Test
    void leRenouvellementMensuelDuTrenteEtUnJanvierTombeEnFevrier() {
        // Le piege classique. `plusMonths` ramene au dernier jour du mois court :
        // 31 janvier -> 28 fevrier. Sans banc, on decouvre le decalage sur une
        // facture, un an plus tard.
        assertThat(MONTHLY.nextRenewal(LocalDate.of(2026, 1, 31)))
                .isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void leRenouvellementAnnuelDuVingtNeufFevrierTombeLeVingtHuit() {
        assertThat(ANNUAL.nextRenewal(LocalDate.of(2028, 2, 29)))
                .isEqualTo(LocalDate.of(2029, 2, 28));
    }
}
