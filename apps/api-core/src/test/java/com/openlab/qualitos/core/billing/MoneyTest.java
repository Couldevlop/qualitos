package com.openlab.qualitos.core.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void deuxMontantsDeDevisesDifferentesNeSAdditionnentPas() {
        // Additionner 10 EUR et 10 USD donnerait un nombre qui ne veut rien dire.
        // Une facture qui additionne des devises est une facture fausse.
        Money euros = Money.of(1000, "EUR");
        Money dollars = Money.of(1000, "USD");

        assertThatThrownBy(() -> euros.plus(dollars))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("devise");
    }

    @Test
    void unMontantSeCompteEnCentiemesEntiers() {
        // 0.1 + 0.2 != 0.3 en virgule flottante. Sur une facture, cet ecart
        // devient un litige.
        Money dixCentimes = Money.of(10, "EUR");
        Money vingtCentimes = Money.of(20, "EUR");

        assertThat(dixCentimes.plus(vingtCentimes).cents()).isEqualTo(30);
    }

    @Test
    void unMontantNegatifEstRefuse() {
        assertThatThrownBy(() -> Money.of(-1, "EUR"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laDeviseSuitLaNormeIso4217() {
        assertThatThrownBy(() -> Money.of(100, "euro"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multiplierParUneQuantiteGardeLaDevise() {
        Money resultat = Money.of(1250, "EUR").times(3);

        assertThat(resultat.cents()).isEqualTo(3750);
        assertThat(resultat.currency()).isEqualTo("EUR");
    }
}
