package com.openlab.qualitos.core.billing.invoice;

import com.openlab.qualitos.core.billing.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'invariant que porte la pièce elle-même : <b>un envoi ne se réécrit pas</b>.
 *
 * <p>{@link InvoiceService} refuse déjà le second envoi, plus tôt — avant de
 * fabriquer un PDF qui ne partira pas. Le garde-fou est ici aussi parce que le
 * service n'est pas le seul appelant concevable (un ordonnanceur de relances,
 * une reprise de données) et que la date d'envoi est une donnée opposable :
 * c'est d'elle que court le délai de paiement.
 */
@DisplayName("Invoice")
class InvoiceTest {

    static final Instant PREMIER_ENVOI = Instant.parse("2026-10-02T09:00:00Z");

    @Test
    void uneFactureNeuveNEstPasEnvoyee() {
        assertThat(facture().isSent()).isFalse();
    }

    @Test
    void envoyerHorodateEtNommeLeDestinataire() {
        Invoice facture = facture();

        facture.markSent("compta@acme.example", PREMIER_ENVOI);

        assertThat(facture.isSent()).isTrue();
        assertThat(facture.getSentAt()).isEqualTo(PREMIER_ENVOI);
        assertThat(facture.getSentTo()).isEqualTo("compta@acme.example");
    }

    @Test
    void unSecondEnvoiNEcrasePasLePremier() {
        // Deux exemplaires de la meme facture, c'est un litige : le client ne
        // sait pas s'il doit payer une fois ou deux, et rien dans les deux
        // exemplaires ne le lui dit.
        Invoice facture = facture();
        facture.markSent("compta@acme.example", PREMIER_ENVOI);

        assertThatThrownBy(() -> facture.markSent("autre@acme.example",
                Instant.parse("2026-11-02T09:00:00Z")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deja envoyee")
                .hasMessageContaining("FA-2026-0007");

        assertThat(facture.getSentAt()).isEqualTo(PREMIER_ENVOI);
        assertThat(facture.getSentTo()).isEqualTo("compta@acme.example");
    }

    @Test
    void laPeriodeEtLeTotalSeLisentDepuisLeursColonnes() {
        // Le total porte sa DEVISE : un long nu ne dit pas combien on reclame.
        Invoice facture = facture();

        assertThat(facture.period()).isEqualTo(YearMonth.of(2026, 9));
        assertThat(facture.total()).isEqualTo(Money.of(9900, "EUR"));
    }

    private static Invoice facture() {
        return Invoice.builder()
                .id(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .number("FA-2026-0007")
                .fiscalYear(2026)
                .periodYear(2026)
                .periodMonth(9)
                .currency("EUR")
                .totalCents(9900)
                .issuedAt(Instant.parse("2026-10-01T06:00:00Z"))
                .issuedBy(UUID.randomUUID())
                .lines(new ArrayList<>())
                .build();
    }
}
