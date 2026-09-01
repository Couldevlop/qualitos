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

    @Test
    void multiplierParUneQuantiteNegativeEstRefuse() {
        // Une quantite negative sur une ligne de facture produirait un montant
        // a retrancher la ou le modele ne prevoit que des montants dus : c'est
        // un avoir deguise, et un avoir se traite comme un avoir, pas comme
        // une multiplication. Le garde-fou existe deja ; ce banc prouve qu'il
        // fonctionne (sans lui, on pourrait le supprimer sans qu'aucun test
        // ne rougisse).
        assertThatThrownBy(() -> Money.of(1250, "EUR").times(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uneDeviseNulleEstRefusee() {
        assertThatThrownBy(() -> Money.of(100, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void uneAdditionQuiDeborderaitLevePlutotQueDeRendreUnMontantFaux() {
        // cents + other.cents peut depasser Long.MAX_VALUE et boucler vers un
        // petit nombre positif, qui passerait alors la validation "pas de
        // negatif" sans etre detecte. Un montant faux voyage jusqu'au client
        // et devient un litige ; une exception, elle, s'arrete ici.
        Money presqueMax = Money.of(Long.MAX_VALUE, "EUR");
        Money unCentime = Money.of(1, "EUR");

        assertThatThrownBy(() -> presqueMax.plus(unCentime))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void uneMultiplicationQuiDeborderaitLevePlutotQueDEnrouler() {
        Money grosMontant = Money.of(Long.MAX_VALUE / 2 + 1, "EUR");

        assertThatThrownBy(() -> grosMontant.times(4))
                .isInstanceOf(ArithmeticException.class);
    }
}
