package com.openlab.qualitos.quality.spc.capability.workbook;

import com.openlab.qualitos.quality.spc.capability.domain.CapabilityCalculation;
import com.openlab.qualitos.quality.spc.capability.domain.ChartFactors;
import com.openlab.qualitos.quality.spc.capability.domain.SpcValidationException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le modèle de grille, et la lecture d'un classeur rempli.
 *
 * <p>Les classeurs sont fabriqués ici, puis relus : un lecteur juste sur un
 * fichier inventé est un lecteur juste. Et le modèle est relu par le lecteur,
 * ce qui vérifie la seule chose qui compte vraiment — qu'un aller-retour
 * télécharger / remplir / verser rende exactement ce qu'on a saisi.
 */
class SpcMeasurementsWorkbookTest {

    /** Un classeur d'une seule feuille, lignes de cellules textuelles ou vides. */
    private static byte[] classeur(String nomFeuille, List<List<Object>> lignes) throws IOException {
        try (Workbook classeur = new XSSFWorkbook();
             ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Sheet feuille = classeur.createSheet(nomFeuille);
            for (int r = 0; r < lignes.size(); r++) {
                Row ligne = feuille.createRow(r);
                List<Object> valeurs = lignes.get(r);
                for (int c = 0; c < valeurs.size(); c++) {
                    Object valeur = valeurs.get(c);
                    if (valeur == null) {
                        continue;
                    }
                    if (valeur instanceof Number nombre) {
                        ligne.createCell(c).setCellValue(nombre.doubleValue());
                    } else {
                        ligne.createCell(c).setCellValue(String.valueOf(valeur));
                    }
                }
            }
            classeur.write(sortie);
            return sortie.toByteArray();
        }
    }

    @Nested
    @DisplayName("le modèle à remplir")
    class Modele {

        @Test
        void porte_une_ligne_par_piece_et_une_colonne_par_sous_groupe() throws IOException {
            byte[] octets = SpcMeasurementsWorkbook.template(5, 20);
            try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(octets))) {
                Sheet feuille = classeur.getSheet(SpcMeasurementsWorkbook.SHEET_MEASUREMENTS);
                assertThat(feuille).isNotNull();
                // En-tête + 5 pièces.
                assertThat(feuille.getLastRowNum()).isEqualTo(5);
                Row titres = feuille.getRow(0);
                assertThat(titres.getCell(0).getStringCellValue())
                        .isEqualTo(SpcMeasurementsWorkbook.HEADER_PIECE);
                assertThat(titres.getCell(20).getStringCellValue()).isEqualTo("Sous-groupe 20");
                assertThat(titres.getCell(21)).isNull();
                // Les rangs de pièce sont écrits : on sait quelle ligne on remplit.
                assertThat(feuille.getRow(3).getCell(0).getNumericCellValue()).isEqualTo(3);
            }
        }

        // Sans mode d'emploi, chacun invente sa disposition et l'import devient devin.
        @Test
        void porte_un_mode_d_emploi() throws IOException {
            byte[] octets = SpcMeasurementsWorkbook.template(5, 20);
            try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(octets))) {
                Sheet feuille = classeur.getSheet(SpcMeasurementsWorkbook.SHEET_HOWTO);
                assertThat(feuille).isNotNull();
                StringBuilder texte = new StringBuilder();
                feuille.forEach(ligne -> texte.append(ligne.getCell(0).getStringCellValue()).append('\n'));
                assertThat(texte.toString())
                        .contains("LIGNE par pièce")
                        .contains("COLONNE par prélèvement")
                        .contains("virgule ou le point")
                        .contains("reste vide");
            }
        }

        @Test
        void refuse_des_dimensions_hors_bornes() {
            assertThatThrownBy(() -> SpcMeasurementsWorkbook.template(1, 20))
                    .isInstanceOf(SpcValidationException.class)
                    .hasMessageContaining("taille d'échantillon");
            assertThatThrownBy(() ->
                    SpcMeasurementsWorkbook.template(5, CapabilityCalculation.MAX_SUBGROUPS + 1))
                    .isInstanceOf(SpcValidationException.class)
                    .hasMessageContaining("sous-groupes");
            assertThatThrownBy(() ->
                    SpcMeasurementsWorkbook.template(ChartFactors.MAX_SUBGROUP_SIZE + 1, 20))
                    .isInstanceOf(SpcValidationException.class);
        }

        // L'aller-retour est le seul essai qui compte : télécharger, remplir, verser.
        @Test
        void se_relit_lui_meme_apres_remplissage() throws IOException {
            byte[] octets = SpcMeasurementsWorkbook.template(3, 4);
            try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(octets))) {
                for (int r = 1; r <= 3; r++) {
                    for (int c = 1; c <= 4; c++) {
                        classeur.getSheet(SpcMeasurementsWorkbook.SHEET_MEASUREMENTS)
                                .getRow(r).createCell(c).setCellValue(10 + r + c);
                    }
                }
                ByteArrayOutputStream rempli = new ByteArrayOutputStream();
                classeur.write(rempli);

                SpcMeasurementsWorkbook.ImportedGrid grille =
                        SpcMeasurementsWorkbook.read(rempli.toByteArray());
                assertThat(grille.sampleSize()).isEqualTo(3);
                assertThat(grille.subgroupCount()).isEqualTo(4);
                assertThat(grille.rows()).containsExactly(
                        List.of("12", "13", "14", "15"),
                        List.of("13", "14", "15", "16"),
                        List.of("14", "15", "16", "17"));
                assertThat(grille.warnings()).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("la lecture d'un classeur rempli")
    class Lecture {

        @Test
        void lit_une_grille_nue_sans_en_tete_ni_rangs() throws IOException {
            byte[] octets = classeur("Feuille1", List.of(
                    List.of(49.8, 50.1),
                    List.of(50.0, 49.9)));
            SpcMeasurementsWorkbook.ImportedGrid grille = SpcMeasurementsWorkbook.read(octets);
            assertThat(grille.rows()).containsExactly(
                    List.of("49.8", "50.1"),
                    List.of("50", "49.9"));
            assertThat(grille.sampleSize()).isEqualTo(2);
            assertThat(grille.subgroupCount()).isEqualTo(2);
        }

        @Test
        void retire_la_ligne_d_en_tete_et_la_colonne_des_rangs() throws IOException {
            byte[] octets = classeur(SpcMeasurementsWorkbook.SHEET_MEASUREMENTS, List.of(
                    List.of("Pièce", "Sous-groupe 1", "Sous-groupe 2"),
                    List.of(1, 49.8, 50.1),
                    List.of(2, 50.0, 49.9)));
            SpcMeasurementsWorkbook.ImportedGrid grille = SpcMeasurementsWorkbook.read(octets);
            assertThat(grille.rows()).containsExactly(
                    List.of("49.8", "50.1"),
                    List.of("50", "49.9"));
        }

        // Une colonne de gauche qui ne porte que du texte est un libellé, pas une mesure.
        @Test
        void retire_une_premiere_colonne_sans_aucun_nombre() throws IOException {
            byte[] octets = classeur("Feuille1", List.of(
                    List.of("Pièce A", 49.8, 50.1),
                    List.of("Pièce B", 50.0, 49.9)));
            SpcMeasurementsWorkbook.ImportedGrid grille = SpcMeasurementsWorkbook.read(octets);
            assertThat(grille.subgroupCount()).isEqualTo(2);
            assertThat(grille.rows().get(0)).containsExactly("49.8", "50.1");
        }

        // Des nombres qui ne suivent pas le rang des lignes SONT des mesures : les
        // prendre pour des rangs perdrait une colonne entière de relevé.
        @Test
        void garde_une_premiere_colonne_de_nombres_qui_ne_sont_pas_des_rangs() throws IOException {
            byte[] octets = classeur("Feuille1", List.of(
                    List.of(49.8, 50.1),
                    List.of(50.0, 49.9),
                    List.of(49.7, 50.2)));
            SpcMeasurementsWorkbook.ImportedGrid grille = SpcMeasurementsWorkbook.read(octets);
            assertThat(grille.subgroupCount()).isEqualTo(2);
            assertThat(grille.rows().get(0)).containsExactly("49.8", "50.1");
        }

        // Une cellule vide reste vide : la pièce n'a pas été mesurée, elle ne compte
        // pas pour zéro — c'est la moyenne du sous-groupe qui serait fausse.
        @Test
        void laisse_vide_une_cellule_vide() throws IOException {
            byte[] octets = classeur("Feuille1", List.of(
                    List.of(49.8, 50.1),
                    java.util.Arrays.asList(50.0, null)));
            SpcMeasurementsWorkbook.ImportedGrid grille = SpcMeasurementsWorkbook.read(octets);
            assertThat(grille.rows().get(1)).containsExactly("50", "");
        }

        @Test
        void ecarte_une_ligne_entierement_vide_au_milieu_du_releve() throws IOException {
            byte[] octets = classeur("Feuille1", List.of(
                    List.of(49.8, 50.1),
                    List.of(),
                    List.of(50.0, 49.9)));
            SpcMeasurementsWorkbook.ImportedGrid grille = SpcMeasurementsWorkbook.read(octets);
            assertThat(grille.sampleSize()).isEqualTo(2);
            assertThat(grille.rows().get(1)).containsExactly("50", "49.9");
        }

        @Test
        void ignore_une_cellule_de_texte_et_le_dit() throws IOException {
            byte[] octets = classeur("Feuille1", List.of(
                    List.of(49.8, "n.m."),
                    List.of(50.0, 49.9)));
            SpcMeasurementsWorkbook.ImportedGrid grille = SpcMeasurementsWorkbook.read(octets);
            assertThat(grille.rows().get(0)).containsExactly("49.8", "");
            assertThat(grille.warnings()).singleElement().asString()
                    .contains("1 cellule(s) non numérique(s)");
        }

        // Le flottant traîne des décimales qui n'ont pas été saisies : 49,000000000000004
        // dans une grille de mesures passerait pour une précision qu'on n'a pas.
        @Test
        void ecrit_les_nombres_sans_exposant_ni_decimales_fantomes() throws IOException {
            byte[] octets = classeur("Feuille1", List.of(
                    List.of(49.0 + 0.000000000000004, 1.0E-4),
                    List.of(1234567.0, 0.5)));
            SpcMeasurementsWorkbook.ImportedGrid grille = SpcMeasurementsWorkbook.read(octets);
            assertThat(grille.rows().get(0)).containsExactly("49", "0.0001");
            assertThat(grille.rows().get(1)).containsExactly("1234567", "0.5");
        }

        @Test
        void prefere_la_feuille_Mesures_aux_autres() throws IOException {
            try (Workbook classeur = new XSSFWorkbook();
                 ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
                Sheet autre = classeur.createSheet("Récapitulatif");
                autre.createRow(0).createCell(0).setCellValue(999.0);
                Sheet mesures = classeur.createSheet(SpcMeasurementsWorkbook.SHEET_MEASUREMENTS);
                Row ligne = mesures.createRow(0);
                ligne.createCell(0).setCellValue(49.8);
                ligne.createCell(1).setCellValue(50.1);
                mesures.createRow(1).createCell(0).setCellValue(50.0);
                mesures.getRow(1).createCell(1).setCellValue(49.9);
                classeur.write(sortie);

                SpcMeasurementsWorkbook.ImportedGrid grille =
                        SpcMeasurementsWorkbook.read(sortie.toByteArray());
                assertThat(grille.rows().get(0)).containsExactly("49.8", "50.1");
            }
        }

        @Test
        void tronque_au_dela_des_bornes_et_le_dit() throws IOException {
            int lignes = ChartFactors.MAX_SUBGROUP_SIZE + 3;
            java.util.List<java.util.List<Object>> contenu = new java.util.ArrayList<>();
            for (int r = 0; r < lignes; r++) {
                java.util.List<Object> ligne = new java.util.ArrayList<>();
                for (int c = 0; c < 3; c++) {
                    ligne.add(50.0 + c);
                }
                contenu.add(ligne);
            }
            SpcMeasurementsWorkbook.ImportedGrid grille =
                    SpcMeasurementsWorkbook.read(classeur("Feuille1", contenu));
            assertThat(grille.sampleSize()).isEqualTo(ChartFactors.MAX_SUBGROUP_SIZE);
            assertThat(grille.rows()).hasSize(ChartFactors.MAX_SUBGROUP_SIZE);
            assertThat(grille.warnings()).singleElement().asString()
                    .contains(lignes + " lignes lues");
        }

        @Test
        void refuse_un_fichier_vide_ou_illisible() {
            assertThatThrownBy(() -> SpcMeasurementsWorkbook.read(new byte[0]))
                    .isInstanceOf(SpcValidationException.class);
            assertThatThrownBy(() -> SpcMeasurementsWorkbook.read("ceci n'est pas un zip".getBytes()))
                    .isInstanceOf(SpcValidationException.class)
                    .hasMessageContaining("classeur Excel");
        }

        @Test
        void refuse_un_classeur_sans_aucune_mesure() throws IOException {
            assertThatThrownBy(() -> SpcMeasurementsWorkbook.read(
                    classeur("Feuille1", List.of(List.of("Pièce", "Sous-groupe 1")))))
                    .isInstanceOf(SpcValidationException.class)
                    .hasMessageContaining("en-tête");
            assertThatThrownBy(() -> SpcMeasurementsWorkbook.read(
                    classeur("Feuille1", List.of(List.of("a", "b"), List.of("c", "d")))))
                    .isInstanceOf(SpcValidationException.class)
                    .hasMessageContaining("Aucune mesure");
        }

        @Test
        void refuse_un_fichier_plus_gros_que_la_borne() {
            byte[] trop = new byte[SpcMeasurementsWorkbook.MAX_UPLOAD_BYTES + 1];
            assertThatThrownBy(() -> SpcMeasurementsWorkbook.read(trop))
                    .isInstanceOf(SpcValidationException.class)
                    .hasMessageContaining("dépasse");
        }
    }

    @Nested
    @DisplayName("ce qui compte pour un nombre")
    class Numerique {

        @Test
        void accepte_la_virgule_comme_separateur_decimal() {
            assertThat(SpcMeasurementsWorkbook.numerique("49,8")).isTrue();
            assertThat(SpcMeasurementsWorkbook.numerique("49.8")).isTrue();
            assertThat(SpcMeasurementsWorkbook.numerique("-0,5")).isTrue();
            // Un espace insécable de séparateur de milliers vient tout droit d'Excel.
            assertThat(SpcMeasurementsWorkbook.numerique("1 234,5")).isTrue();
        }

        @Test
        void refuse_ce_qui_n_est_pas_un_nombre() {
            assertThat(SpcMeasurementsWorkbook.numerique("")).isFalse();
            assertThat(SpcMeasurementsWorkbook.numerique("   ")).isFalse();
            assertThat(SpcMeasurementsWorkbook.numerique(null)).isFalse();
            assertThat(SpcMeasurementsWorkbook.numerique("n.m.")).isFalse();
            assertThat(SpcMeasurementsWorkbook.numerique("49 mm")).isFalse();
        }
    }
}
