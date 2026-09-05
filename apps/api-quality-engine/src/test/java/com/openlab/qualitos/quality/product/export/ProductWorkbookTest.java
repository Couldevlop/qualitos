package com.openlab.qualitos.quality.product.export;

import com.openlab.qualitos.quality.controlplan.application.ControlPlanDto;
import com.openlab.qualitos.quality.controlplan.domain.CharacteristicType;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanPhase;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStatus;
import com.openlab.qualitos.quality.controlplan.domain.InputOutput;
import com.openlab.qualitos.quality.risk.FmeaDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le classeur est RELU, pas seulement produit.
 *
 * <p>Vérifier qu'un tableau d'octets n'est pas vide ne prouve rien : un
 * classeur d'une feuille blanche est un classeur valide. On rouvre le fichier
 * avec POI et on affirme ce qu'il contient — c'est ce que le destinataire
 * ouvrira.
 */
@DisplayName("ProductWorkbook")
class ProductWorkbookTest {

    private static final UUID PROJET = UUID.randomUUID();

    @Test
    void leClasseurPorteLesDeuxFeuilles() throws IOException {
        // Le PFMEA dit ce qui peut mal tourner, le plan de surveillance ce qu'on
        // controle pour l'empecher : les separer perdrait le lien entre les deux.
        try (Workbook w = ouvre(ProductWorkbook.build("P-001 — Support", List.of(item(1, 160)), plan()))) {
            assertThat(w.getNumberOfSheets()).isEqualTo(2);
            assertThat(w.getSheetName(0)).isEqualTo("PFMEA");
            assertThat(w.getSheetName(1)).isEqualTo("Plan de surveillance");
        }
    }

    @Test
    void chaqueFeuillePorteLeProduitDansSonTitre() throws IOException {
        // Le titre est SUR la feuille et pas seulement dans le nom du fichier :
        // un onglet recopie ailleurs perd son fichier d'origine, et une feuille
        // de cotation qu'on ne rattache plus a un produit ne vaut rien.
        try (Workbook w = ouvre(ProductWorkbook.build("P-001 — Support", List.of(item(1, 160)), plan()))) {
            assertThat(texte(w.getSheetAt(0), 0, 0)).contains("P-001").contains("PFMEA");
            assertThat(texte(w.getSheetAt(1), 0, 0)).contains("P-001").contains("Plan de surveillance");
        }
    }

    @Test
    void lePfmeaPorteLeRpnEtLeRpnApresAction() throws IOException {
        // C'est ce qu'on vient chercher dans un tableur : le RPN pour trier, le
        // RPN apres pour montrer que les actions ont servi. L'ecran, faute de
        // place, n'affiche pas le second sur l'onglet produit.
        try (Workbook w = ouvre(ProductWorkbook.build("P-001", List.of(item(1, 160)), null))) {
            Sheet feuille = w.getSheetAt(0);
            List<String> entetes = entetes(feuille);
            assertThat(entetes).contains("RPN", "RPN après", "S", "O", "D");

            Row ligne = feuille.getRow(3);   // titre(0), vide(1), en-tetes(2)
            assertThat(nombre(ligne, entetes.indexOf("RPN"))).isEqualTo(160);
            assertThat(nombre(ligne, entetes.indexOf("RPN après"))).isEqualTo(48);
            assertThat(nombre(ligne, entetes.indexOf("S"))).isEqualTo(8);
        }
    }

    @Test
    void uneCoteNonRenseigneeResteVideEtNePasseraPasPourUnZero() throws IOException {
        // Un 0 sur une echelle qui commence a 1 n'existe pas : l'ecrire ferait
        // croire a une cotation faite, et fausserait tri comme moyenne.
        FmeaDto.ItemResponse sansAction = item(1, 160, null);
        try (Workbook w = ouvre(ProductWorkbook.build("P-001", List.of(sansAction), null))) {
            Sheet feuille = w.getSheetAt(0);
            int colonne = entetes(feuille).indexOf("RPN après");
            Cell cellule = feuille.getRow(3).getCell(colonne);

            assertThat(cellule.getCellType()).isEqualTo(CellType.BLANK);
        }
    }

    @Test
    void lePlanDeSurveillancePorteSesVingtColonnes() throws IOException {
        try (Workbook w = ouvre(ProductWorkbook.build("P-001", List.of(), plan()))) {
            Sheet feuille = w.getSheetAt(1);
            List<String> entetes = entetes(feuille);

            assertThat(entetes).hasSize(20);
            assertThat(entetes).contains("Plan de réaction", "Fréquence", "Qui mesure",
                    "Tolérance min.", "Tolérance max.");

            Row ligne = feuille.getRow(3);
            assertThat(texteCellule(ligne, entetes.indexOf("Plan de réaction")))
                    .isEqualTo("Bloquer le lot et alerter le pilote");
            assertThat(ligne.getCell(entetes.indexOf("Tolérance min.")).getNumericCellValue())
                    .isEqualTo(9.8);
        }
    }

    @Test
    void laRevisionEtLeStatutDuPlanFigurentDansSonTitre() throws IOException {
        // Un plan de surveillance sans sa revision n'est pas opposable : deux
        // exports du meme produit a six mois d'ecart seraient indiscernables.
        try (Workbook w = ouvre(ProductWorkbook.build("P-001", List.of(), plan()))) {
            assertThat(texte(w.getSheetAt(1), 0, 0))
                    .contains("CP-001").contains("révision 3").contains("ACTIVE");
        }
    }

    @Test
    void sansPlanDeSurveillanceLaFeuilleSortVideMaisAvecSesEntetes() throws IOException {
        // Elle dit « il n'y en a pas ». Un classeur a une seule feuille
        // laisserait croire a un export tronque — et le module peut simplement
        // ne pas etre souscrit par ce client.
        try (Workbook w = ouvre(ProductWorkbook.build("P-001", List.of(item(1, 160)), null))) {
            Sheet feuille = w.getSheetAt(1);

            assertThat(entetes(feuille)).hasSize(20);
            assertThat(feuille.getRow(3)).isNull();
        }
    }

    @Test
    void unProduitSansAnalyseNiPlanDonneUnClasseurLisible() throws IOException {
        // Le cas du produit tout neuf : pas d'erreur, deux feuilles en-tetees.
        try (Workbook w = ouvre(ProductWorkbook.build("P-NEUF", List.of(), null))) {
            assertThat(w.getNumberOfSheets()).isEqualTo(2);
            assertThat(entetes(w.getSheetAt(0))).isNotEmpty();
            assertThat(w.getSheetAt(0).getRow(3)).isNull();
        }
    }

    @Test
    void lesEntetesSontFigeesEtFiltrables() throws IOException {
        // Sans cela, la premiere chose que fait le destinataire est de les poser
        // a la main : vingt colonnes sans en-tete figee se lisent en remontant
        // sans cesse pour savoir ou l'on est.
        try (Workbook w = ouvre(ProductWorkbook.build("P-001", List.of(item(1, 160)), plan()))) {
            Sheet feuille = w.getSheetAt(0);

            assertThat(feuille.getPaneInformation()).isNotNull();
            assertThat((int) feuille.getPaneInformation().getHorizontalSplitTopRow()).isEqualTo(3);
        }
    }

    @Test
    void unTexteAbsentDevientUneCelluleVideEtNonLeMotNull() throws IOException {
        FmeaDto.ItemResponse sansCause = item(1, 160, 48, null);
        try (Workbook w = ouvre(ProductWorkbook.build("P-001", List.of(sansCause), null))) {
            Sheet feuille = w.getSheetAt(0);
            int colonne = entetes(feuille).indexOf("Cause");

            assertThat(texteCellule(feuille.getRow(3), colonne)).isEmpty();
        }
    }

    @Test
    void lesLignesSortentDansLOrdreRecu() throws IOException {
        // L'ordre du PFMEA est celui du processus : le renverser rendrait le
        // classeur illisible pour qui suit la ligne de fabrication.
        List<FmeaDto.ItemResponse> items = List.of(item(1, 160), item(2, 90), item(3, 210));
        try (Workbook w = ouvre(ProductWorkbook.build("P-001", items, null))) {
            Sheet feuille = w.getSheetAt(0);

            assertThat(nombre(feuille.getRow(3), 0)).isEqualTo(1);
            assertThat(nombre(feuille.getRow(4), 0)).isEqualTo(2);
            assertThat(nombre(feuille.getRow(5), 0)).isEqualTo(3);
        }
    }

    // ---------- montage ----------

    private static Workbook ouvre(byte[] octets) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(octets));
    }

    private static List<String> entetes(Sheet feuille) {
        List<String> noms = new ArrayList<>();
        Row ligne = feuille.getRow(2);
        for (int i = 0; i < ligne.getLastCellNum(); i++) {
            noms.add(ligne.getCell(i).getStringCellValue());
        }
        return noms;
    }

    private static String texte(Sheet feuille, int ligne, int colonne) {
        return feuille.getRow(ligne).getCell(colonne).getStringCellValue();
    }

    private static String texteCellule(Row ligne, int colonne) {
        return ligne.getCell(colonne).getStringCellValue();
    }

    private static int nombre(Row ligne, int colonne) {
        return (int) ligne.getCell(colonne).getNumericCellValue();
    }

    private static FmeaDto.ItemResponse item(int rang, int rpn) {
        return item(rang, rpn, 48);
    }

    private static FmeaDto.ItemResponse item(int rang, int rpn, Integer rpnApres) {
        return item(rang, rpn, rpnApres, "Outil usé");
    }

    private static FmeaDto.ItemResponse item(int rang, int rpn, Integer rpnApres, String cause) {
        return new FmeaDto.ItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), PROJET, rang,
                "Assembler le support", "Sertissage incomplet", "Perte de contact",
                cause, "Contrôle visuel en fin de poste",
                8, 4, 5, rpn,
                "Ajouter un capteur de présence", UUID.randomUUID(), "A. Martin",
                LocalDate.of(2026, 11, 30), "Capteur posé", LocalDate.of(2026, 10, 15),
                rpnApres == null ? null : 8, rpnApres == null ? null : 2,
                rpnApres == null ? null : 3, rpnApres,
                null, null, null, false, Instant.now(), Instant.now());
    }

    private static ControlPlanDto.Detail plan() {
        ControlPlanDto.View entete = new ControlPlanDto.View(
                UUID.randomUUID(), UUID.randomUUID(), ControlPlanPhase.PRODUCTION,
                "CP-001", 3, ControlPlanStatus.ACTIVE,
                UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                Instant.now(), Instant.now(), "sha", null);
        ControlPlanDto.LineView ligne = new ControlPlanDto.LineView(
                UUID.randomUUID(), 1, UUID.randomUUID(), "Presse 12", "C-01",
                "Diamètre de perçage", "Ø 10 ±0,2", CharacteristicType.PRODUCT,
                null, "10 mm", new BigDecimal("9.8"), new BigDecimal("10.2"), "mm",
                "Pied à coulisse", "5 pièces", "Chaque heure",
                "Mesure", "Bloquer le lot et alerter le pilote", null,
                "MO-114", InputOutput.OUTPUT, "Opérateur", "Fiche de relevé");
        return new ControlPlanDto.Detail(entete, List.of(ligne));
    }
}
