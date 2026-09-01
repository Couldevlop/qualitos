package com.openlab.qualitos.core.billing.invoice;

import com.openlab.qualitos.core.billing.BillingPeriod;
import com.openlab.qualitos.core.billing.BillingProfileDto;
import com.openlab.qualitos.core.billing.BillingTier;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le PDF est relu, pas seulement produit.
 *
 * <p>Vérifier qu'un tableau d'octets n'est pas vide ne prouve rien : un PDF
 * d'une page blanche est un PDF valide. {@link PDFTextStripper} extrait le
 * texte réellement dessiné, et c'est ce texte qu'on affirme — c'est ce que le
 * client lira.
 */
@DisplayName("PdfBoxInvoiceRenderer")
class PdfBoxInvoiceRendererTest {

    private static final UUID CLIENT = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final PdfBoxInvoiceRenderer renderer = new PdfBoxInvoiceRenderer();

    @Test
    void laFacturePorteLIdentiteDuClientPasSonUuid() throws IOException {
        // « On ne facture pas un UUID » : c'est la raison d'etre du profil de
        // facturation. Un identifiant technique sur une facture ne designe
        // personne pour un comptable.
        String texte = texteDe(renderer.render(facture(9900), profil("ACME Industries SAS", null)));

        assertThat(texte).contains("ACME Industries SAS");
        assertThat(texte).doesNotContain(CLIENT.toString());
    }

    @Test
    void laFacturePorteLeNumeroDeTvaQuandIlExiste() throws IOException {
        String texte = texteDe(renderer.render(facture(9900),
                profil("ACME Industries SAS", "FR12345678901")));

        assertThat(texte).contains("FR12345678901");
    }

    @Test
    void sansNumeroDeTvaAucuneLigneVideNEstImprimee() throws IOException {
        // Une ligne « N TVA : » vide ferait croire a une omission plutot qu'a
        // une absence.
        String texte = texteDe(renderer.render(facture(9900), profil("ACME", null)));

        assertThat(texte).doesNotContain("TVA");
    }

    @Test
    void unMontantSAfficheAvecSesDeuxDecimalesEtSaDevise() throws IOException {
        // 9900 centimes s'ecrit 99,00 EUR — jamais 9900, jamais 99. C'est
        // l'erreur qu'aucune assertion sur des entiers ne voit.
        String texte = texteDe(renderer.render(facture(9900), profil("ACME", null)));

        assertThat(texte).contains("99,00").contains("EUR");
    }

    @Test
    void unMontantInferieurAUnEuroGardeSesDeuxDecimales() throws IOException {
        // 50 centimes s'ecrit 0,50 et non 0,5 : un zero perdu decale la lecture
        // d'un facteur dix.
        String texte = texteDe(renderer.render(facture(50), profil("ACME", null)));

        assertThat(texte).contains("0,50");
    }

    @Test
    void laFacturePorteSonNumeroSaPeriodeEtSonTotal() throws IOException {
        String texte = texteDe(renderer.render(facture(9900), profil("ACME", null)));

        assertThat(texte).contains("FA-2026-0007");
        assertThat(texte).contains("2026-09");
        assertThat(texte).contains("TOTAL");
        assertThat(texte).contains("FACTURE");
    }

    @Test
    void chaqueLigneDeFactureApparaitDansLaPiece() throws IOException {
        Invoice deuxLignes = facture(9900);
        deuxLignes.getLines().add(ligne(2, "risk", 1500));
        deuxLignes.setTotalCents(11400);

        String texte = texteDe(renderer.render(deuxLignes, profil("ACME", null)));

        assertThat(texte).contains("controlplan").contains("risk");
        assertThat(texte).contains("15,00");
        assertThat(texte).contains("114,00");
    }

    @Test
    void uneRaisonSocialeTypographiqueNEmpechePasLaFactureDeSortir() throws IOException {
        // Le defaut que ce banc verrouille : les polices standard de PDFBox ne
        // savent pas dessiner l'apostrophe courbe ni le tiret cadratin, et
        // l'ecriture LEVE. Sans translitteration, une facture devenait
        // impossible a emettre a cause d'un caractere colle depuis un
        // traitement de texte.
        String texte = texteDe(renderer.render(facture(9900),
                profil("L’Étoile — Ingénierie", null)));

        assertThat(texte).contains("L'Étoile - Ingénierie");
    }

    @Test
    void unCaractereIndessinableDevientUnPointDInterrogationPasUneException() throws IOException {
        // Visible, donc corrigeable — la ou une exception aurait empeche la
        // facture de sortir.
        String texte = texteDe(renderer.render(facture(9900), profil("ACME 中文", null)));

        assertThat(texte).contains("ACME ??");
    }

    @Test
    void laTranslitterationLaisseIntactUnTexteOrdinaire() {
        assertThat(PdfBoxInvoiceRenderer.sanitize("Societe Generale 75008"))
                .isEqualTo("Societe Generale 75008");
        assertThat(PdfBoxInvoiceRenderer.sanitize(null)).isEmpty();
        assertThat(PdfBoxInvoiceRenderer.sanitize("un… deux trois €"))
                .isEqualTo("un... deux trois EUR");
        assertThat(PdfBoxInvoiceRenderer.sanitize("“cite”")).isEqualTo("\"cite\"");
    }

    @Test
    void unCaractereDeControleNeSeDessinePasNonPlus() {
        // La plage 0x00-0x1F et la plage 0x80-0x9F sont ecartees a dessein :
        // WinAnsiEncoding n'y definit pas tout, et un caractere invisible colle
        // depuis un traitement de texte ferait ECHOUER l'ecriture, donc
        // l'emission d'une facture.
        //
        // Les caracteres sont construits par leur CODE et non ecrits en
        // litteral : un caractere de controle dans un fichier source est
        // invisible a la relecture, et une sequence d echappement unicode serait
        // transformee par javac AVANT l analyse lexicale — le banc dirait alors
        // autre chose que ce qu il a l air de dire.
        assertThat(PdfBoxInvoiceRenderer.sanitize("ACME" + (char) 0x07 + "SAS"))
                .isEqualTo("ACME?SAS");
        assertThat(PdfBoxInvoiceRenderer.sanitize("ACME" + (char) 0x92 + "SAS"))
                .isEqualTo("ACME?SAS");
        // La plage haute de Latin-1, elle, se dessine : « é » n'a pas a devenir
        // un point d'interrogation.
        assertThat(PdfBoxInvoiceRenderer.sanitize("Ingénierie")).isEqualTo("Ingénierie");
    }

    @Test
    void unNumeroDeTvaFaitDEspacesNEstPasImprime() throws IOException {
        // Un champ rempli d'espaces est un champ vide : l'imprimer produirait
        // une ligne « N TVA : » sans numero, qui ferait croire a une omission.
        String texte = texteDe(renderer.render(facture(9900), profil("ACME", "   ")));

        assertThat(texte).doesNotContain("TVA");
    }

    // ---------- montage ----------

    private static String texteDe(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static Invoice facture(long cents) {
        List<InvoiceLine> lines = new ArrayList<>();
        lines.add(ligne(1, "controlplan", cents));
        return Invoice.builder()
                .id(UUID.randomUUID())
                .tenantId(CLIENT)
                .number("FA-2026-0007")
                .fiscalYear(2026)
                .periodYear(2026)
                .periodMonth(9)
                .currency("EUR")
                .totalCents(cents)
                .issuedAt(Instant.parse("2026-10-01T06:00:00Z"))
                .issuedBy(UUID.randomUUID())
                .lines(lines)
                .build();
    }

    private static InvoiceLine ligne(int lineNo, String moduleCode, long cents) {
        return InvoiceLine.builder()
                .id(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .lineNo(lineNo)
                .moduleCode(moduleCode)
                .billingTier(BillingTier.STANDARD)
                .period(BillingPeriod.MONTHLY)
                .quantity(1)
                .unitAmountCents(cents)
                .lineTotalCents(cents)
                .build();
    }

    private static BillingProfileDto.View profil(String legalName, String vatNumber) {
        return new BillingProfileDto.View(
                UUID.randomUUID(), CLIENT, legalName, vatNumber,
                "1 rue de la Facture", null, "75000", "Paris", "FR",
                "compta@acme.example", "EUR", false, null,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }
}
