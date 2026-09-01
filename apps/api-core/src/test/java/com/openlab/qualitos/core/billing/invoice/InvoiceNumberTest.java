package com.openlab.qualitos.core.billing.invoice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InvoiceNumber")
class InvoiceNumberTest {

    @Test
    void laNumerotationEstContinueEtSansTrou() {
        // Une numerotation a trous est un motif de rejet en controle fiscal :
        // rien ne distingue un numero saute d'une facture detruite.
        assertThat(InvoiceNumber.next("FA-2026-0041")).isEqualTo("FA-2026-0042");
    }

    @Test
    void laNumerotationRepartAUnAChaqueExercice() {
        assertThat(InvoiceNumber.first(2027)).isEqualTo("FA-2027-0001");
    }

    @Test
    void leRangDebordeVersLeHautPlutotQueDeRepartirAZero() {
        // Le point qui compte. Repartir a 0000 apres 9999 reattribuerait des
        // numeros deja emis : deux pieces comptables porteraient la meme
        // reference, ce qui est pire que le debordement de format qu'on aurait
        // voulu eviter.
        assertThat(InvoiceNumber.next("FA-2031-9999")).isEqualTo("FA-2031-10000");
        assertThat(InvoiceNumber.next("FA-2031-10000")).isEqualTo("FA-2031-10001");
    }

    @Test
    void leRangResteCadreSurQuatreChiffresEnDessousDeDixMille() {
        assertThat(InvoiceNumber.next("FA-2026-0009")).isEqualTo("FA-2026-0010");
        assertThat(InvoiceNumber.next("FA-2026-0099")).isEqualTo("FA-2026-0100");
    }

    @Test
    void lExerciceNeChangePasParIncrement() {
        // Incrementer au travers d'un 31 decembre produirait FA-2026-0413 en
        // janvier 2027 : l'exercice porte par le numero ne correspondrait plus
        // a celui de la facture. Changer d'exercice passe par first().
        assertThat(InvoiceNumber.next("FA-2026-0412")).startsWith("FA-2026-");
    }

    @Test
    void lExerciceSeLitDansLeNumero() {
        assertThat(InvoiceNumber.fiscalYearOf("FA-2026-0041")).isEqualTo(2026);
    }

    // ---------- refus ----------

    @Test
    void unNumeroIllisibleNeSeRepareEnRepartantDeUn() {
        // Repartir de 1 face a un numero abime reattribuerait des numeros deja
        // emis. On refuse, et quelqu'un regarde ce qu'il y a en base.
        assertThatThrownBy(() -> InvoiceNumber.next("2026/41"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("illisible");
    }

    @Test
    void unNumeroAbsentEstRefuse() {
        assertThatThrownBy(() -> InvoiceNumber.next(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absent");
        assertThatThrownBy(() -> InvoiceNumber.next("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absent");
    }

    @Test
    void unRangTropCourtEstRefuse() {
        // FA-2026-41 n'est pas un numero cadre : l'accepter produirait
        // FA-2026-0042 a la suite de FA-2026-41, et deux formats coexisteraient
        // dans la meme sequence.
        assertThatThrownBy(() -> InvoiceNumber.next("FA-2026-41"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unExerciceHorsPlageEstRefuse() {
        // Meme fenetre que chk_invoice_year : un exercice a trois chiffres
        // casserait le format du numero.
        assertThatThrownBy(() -> InvoiceNumber.first(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exercice");
        assertThatThrownBy(() -> InvoiceNumber.first(3000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lExerciceDUnNumeroIllisibleEstRefuse() {
        assertThatThrownBy(() -> InvoiceNumber.fiscalYearOf("FACTURE-41"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InvoiceNumber.fiscalYearOf(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
